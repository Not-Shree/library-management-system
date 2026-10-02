# Project Documentation — Library Management & Fine Calculator System

## 1. Problem and scope

College libraries still track loans in registers or spreadsheets. Fines are calculated by hand, students do not know when books are due, and nobody can see who is waiting for a popular title. This system digitises the circulation desk: the catalogue, members, issue / return / renewal, automatic fine calculation, offline fine payments, reservations, notifications and reports.

**In scope:** everything listed in the README feature list.
**Out of scope in this version:** see [section 9](#9-what-is-not-implemented).

## 2. Architecture

```
 Browser (React + Vite + Bootstrap)
      │  JSON over HTTP, JWT in the Authorization header
      ▼
 Spring Boot REST API
   controller  → receives HTTP, validates DTOs (@Valid), checks role (@PreAuthorize)
   service     → all business rules, one @Transactional method per use case
   repository  → Spring Data JPA (derived queries, JPQL, Specifications for search)
   entity      → JPA mapping of the tables
      │  JDBC
      ▼
 PostgreSQL (schema.sql = source of truth; Hibernate does not create tables)
```

Why this layering: controllers stay thin and are easy to read; every rule lives in exactly one service method, so it applies no matter who calls the API (the React app, Swagger, or Postman).

**Request life-cycle (example: issue a book)**
1. `JwtAuthenticationFilter` reads the `Bearer` token, verifies the signature and expiry, and loads the user.
2. `@PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")` rejects members with HTTP 403.
3. `@Valid IssueRequest` rejects a missing member id with HTTP 400.
4. `BorrowingService.issue()` runs the rules inside one transaction. Any rule failure throws `BusinessRuleException`, the transaction rolls back, and `GlobalExceptionHandler` turns it into the standard error JSON.

## 3. Business rules and where they are enforced

| # | Rule | Enforced in |
|---|---|---|
| 1 | Only **active** members can borrow or reserve. | `BorrowingService.issue`, `ReservationService.create` |
| 2 | A member cannot exceed their **borrowing limit** (member's own limit, else the library default). | `BorrowingService.issue` → `borrowLimit()` |
| 3 | Only a copy that is **AVAILABLE** can be issued — or a **RESERVED** copy, and only to the member it is held for. | `BorrowingService.issue` |
| 4 | A copy can be out with **one member at a time**, even under simultaneous requests. | Row lock `findByIdForUpdate` + partial unique index `uq_active_borrowing_per_copy` |
| 5 | A member cannot borrow **two copies of the same title** at once. | `BorrowingService.issue` |
| 6 | Members owing more than the **unpaid-fine limit** cannot borrow (switchable in Settings). | `BorrowingService.issue` |
| 7 | **Due date = issue date + loan period** from settings. | `BorrowingService.issue` |
| 8 | **Fine is calculated automatically** on return from the settings (rate, grace, maximum) and the rate is stored on the fine. | `FineCalculator`, `BorrowingService.returnBook` |
| 9 | A return date cannot be in the **future** or **before the issue date**. | `BorrowingService.validReturnDate` |
| 10 | **Renewal** limits: maximum renewals, no renewal when overdue (unless allowed), no renewal when **another member has reserved** the book. | `BorrowingService.renew` |
| 11 | A payment can never be **more than the outstanding amount**; UPI and card payments need a **reference number**. | `FineService.recordPayment`, DB check `chk_fine_not_overpaid` |
| 12 | Reservations are only for books with **no free copy**, one active reservation per member per book, served **first come, first served**. | `ReservationService.create` / `offerCopyToQueue`, index `uq_active_reservation` |
| 13 | A returned copy is **held** for the next person in the queue for `reservation_hold_days`; uncollected holds **expire** and move on. | `ReservationService.offerCopyToQueue`, `expireUncollectedHolds` (daily job) |
| 14 | **History is never deleted**: books are soft-deleted, members with history can only be deactivated, borrowed books cannot be removed. | `BookService.delete`, `MemberService.delete`, `ON DELETE RESTRICT` FKs |

Every state-changing action also writes an **audit log** row in the same transaction (so a failed action leaves no log).

## 4. The fine calculator

```
overdue days     = return date − due date          (0 if returned on or before the due date)
chargeable days  = max(0, overdue days − grace period)
fine             = min(chargeable days × fine per day, maximum fine per book)
```

`FineCalculator` is a pure class with no database access, so it is unit-tested directly (`FineCalculatorTest`, 8 tests).

| Due | Returned | Grace | Rate | Max | Fine | Why |
|---|---|---|---|---|---|---|
| 10 Oct 2026 | 10 Oct 2026 | 0 | ₹5 | ₹500 | ₹0 | On time |
| 10 Oct 2026 | 15 Oct 2026 | 0 | ₹5 | ₹500 | **₹25** | 5 × ₹5 (the example in the specification) |
| 10 Oct 2026 | 15 Oct 2026 | 2 | ₹5 | ₹500 | ₹15 | (5 − 2) × ₹5 |
| 01 Jan 2026 | 01 Jun 2026 | 0 | ₹5 | ₹500 | ₹500 | 151 × ₹5 = ₹755, capped |

**Worked due-date example:** issued 25 Sep 2026 with a 14-day loan period → due 9 Oct 2026.

Money uses `BigDecimal` / `NUMERIC(10,2)`, never `double`, so there are no rounding errors.

## 5. Reservation life-cycle

```
           reserve (no copy free)
  ──────────────▶ WAITING ──── copy returned, member is first in queue ───▶ AVAILABLE (copy held, collect by date)
                     │                                                           │        │
                  cancel                                             issued to member   not collected in time
                     ▼                                                           ▼        ▼
                 CANCELLED                                                   FULFILLED   EXPIRED → copy offered to next in queue
```

The queue position shown to members is computed (`count of WAITING reservations placed earlier + 1`), not stored.

## 6. Security

- Passwords are stored as **BCrypt** hashes (strength 10). The seed data creates them with PostgreSQL `pgcrypto`, which produces the same `$2a$` format Spring Security verifies.
- Stateless **JWT** signed with HMAC-SHA (jjwt picks HS256/384/512 from the secret length). The secret comes from the `JWT_SECRET` environment variable and must be at least 32 characters — the app refuses to start otherwise.
- Every endpoint is protected by role; members can only reach `/api/me/**` and their own borrowings/reservations (checked in the service, not just hidden in the UI).
- Login errors never reveal whether the username exists.
- CSV export neutralises spreadsheet formula injection (cells starting with `=`, `+`, `-`, `@`).
- CORS is limited to `CORS_ALLOWED_ORIGINS`.
- No secrets are committed: `.env` files are git-ignored and `.env.example` files contain placeholders.

## 7. Scheduled jobs

`ScheduledJobs` runs every day at 08:00 (and once at startup): due-soon reminders, overdue notices, and expiry of uncollected holds. Each reminder is sent once per loan (flags `due_reminder_sent`, `overdue_notice_sent`; reset on renewal).

## 8. Verification done while building

| Check | Result |
|---|---|
| `schema.sql` and `seed.sql` loaded into PostgreSQL 16 | Clean; seed produces ₹570 outstanding across 3 fines, 3 overdue loans, 3 waiting reservations |
| Every JPA entity field compared with the live database columns | 152 / 152 match in both directions |
| All backend Java sources type-checked with `javac` (Java 21) | 0 errors |
| Native SQL used by dashboard charts, run against the seed data | Returns the expected monthly figures |
| Fine calculator logic | Run standalone; unit tests included |
| Frontend production build (`npm run build`) | Succeeds |
| ESLint (undefined variables, rules of hooks) on all frontend files | 0 errors, 0 warnings |
| Key screens rendered in headless Chromium, desktop and mobile | No runtime errors |

**Not verified in the build environment:** the backend was not started end-to-end, because the sandbox could not download Maven dependencies. The Java code was type-checked against stub versions of the Spring APIs rather than the real libraries. Run `mvn spring-boot:run` once locally (README section 8) — that is the first real start-up, so please report any stack trace.

## 9. What is not implemented

These are deliberately left out and are **not** faked in the UI:

- **PDF export** of reports — CSV only (the PDF button is visibly disabled with a tooltip).
- **Email / SMS** — notifications are in-app only. "Forgot password" explains the counter reset process; staff reset passwords from the admin panel.
- **Online payments** — payments are recorded by staff as offline cash/UPI/card transactions; no gateway is called.
- **Barcode scanner hardware** — a USB scanner that types the copy code into the "copy code" box works, but there is no camera scanning.
- **Refresh tokens** — users log in again after the token expires.
- **Multi-branch libraries**, book purchase / acquisition workflow, and e-books.

## 10. Likely viva questions

**Why separate `books` and `book_copies`?** A title can have many physical copies with different states (one borrowed, one damaged). Loans and reservations point at the exact copy, while search and reservations work at title level.

**Why not store `available_copies` in `books`?** It would have to be updated on every issue, return, loss and repair, and one missed update makes it wrong forever. `COUNT(*) WHERE status = 'AVAILABLE'` is always right and indexed.

**What stops two librarians issuing the same copy at the same moment?** `SELECT … FOR UPDATE` on the copy row inside the issue transaction; the second request waits, then sees the copy is BORROWED. As a last line of defence, the partial unique index rejects a second active loan.

**Why copy `fine_per_day` into the fine?** If the admin changes the rate later, old fines must not change retroactively.

**Why is a fine not created for books still out?** A fine is final only when the book comes back. While it is out, the UI shows an *estimated* fine ("₹80 so far") calculated on the fly.

**Why DTOs instead of returning entities?** Entities contain lazy relations and fields such as `password_hash`; DTOs expose exactly what the screen needs and keep the API stable if tables change.

**How does the frontend know the user's role?** The login response includes the user and role; the JWT is stored and sent on every request. The UI hides pages by role, but the **server** is what enforces it.

**What happens when the token expires?** The API returns 401, the Axios interceptor clears the token and sends the user to the login page with a message.
