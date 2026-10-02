# API Documentation

Base URL: `http://localhost:8080/api` · Interactive docs (Swagger UI): **http://localhost:8080/swagger-ui.html**

## Authentication

1. `POST /api/auth/login` with `{"username": "librarian", "password": "Librarian@123"}` (username **or** email).
2. The response contains a JWT: `{"token": "eyJ...", "tokenType": "Bearer", "expiresAt": "...", "user": {...}}`.
3. Send it on every other request: `Authorization: Bearer eyJ...`. In Swagger UI, click **Authorize** and paste the token.

Tokens expire after `JWT_EXPIRATION_MINUTES` (default 480 = 8 hours). There is no refresh token; the user logs in again.

## Error format

Every error has the same shape:

```json
{
  "timestamp": "2026-09-25T10:15:30",
  "status": 400,
  "error": "BORROW_LIMIT_EXCEEDED",
  "message": "Riya Patil already has 3 of 3 allowed books",
  "path": "/api/borrowings/issue"
}
```

Validation errors (HTTP 400, `error: "VALIDATION_ERROR"`) also include `"fieldErrors": {"email": "must be a valid email"}`.

| Status | When |
|---|---|
| 400 | Invalid input, or a library rule was broken (limit, unpaid fines, renewal rules, ...) |
| 401 | Not logged in, bad credentials, or expired token |
| 403 | Logged in, but the role is not allowed |
| 404 | Record does not exist |
| 409 | Conflict: duplicate ISBN / member ID / copy code, copy not available, record in use |

## Paging

List endpoints accept `page` (0-based), `size` (max 100) and return:
`{"content": [...], "page": 0, "size": 10, "totalElements": 42, "totalPages": 5}`.

## Endpoints

### Authentication

| Method | Endpoint | Who can call it | Purpose |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Log in with username (or email) and password; returns a JWT |
| POST | `/api/auth/register` | Public | Register as a library member |
| GET | `/api/auth/me` | Any logged-in user | The logged-in user |
| POST | `/api/auth/change-password` | Any logged-in user | Change my password |

### Staff accounts

| Method | Endpoint | Who can call it | Purpose |
|---|---|---|---|
| GET | `/api/users/staff` | Admin | List librarians and admins |
| POST | `/api/users/staff` | Admin | Create a librarian or admin account |
| PUT | `/api/users/staff/{id}` | Admin | Edit a staff account / enable or disable it |
| POST | `/api/users/{id}/reset-password` | Admin, Librarian | Set a new password (admin: anyone; librarian: members only) |

### Books & copies

| Method | Endpoint | Who can call it | Purpose |
|---|---|---|---|
| GET | `/api/books` | Public | Search the catalogue (public) |
| GET | `/api/books/{id}` | Public | One book with copy counts |
| GET | `/api/books/meta/languages` | Public | Languages used in the catalogue (for filters) |
| POST | `/api/books` | Admin, Librarian | Add a book (optionally with its first copies) |
| PUT | `/api/books/{id}` | Admin, Librarian | Edit book details |
| DELETE | `/api/books/{id}` | Admin | Remove a book from the catalogue (soft delete, history is kept) |
| GET | `/api/books/{id}/copies` | Admin, Librarian | Physical copies of a book, with current borrower |
| POST | `/api/books/{id}/copies` | Admin, Librarian | Register new copies (codes generated if blank) |
| PUT | `/api/books/copies/{copyId}` | Admin, Librarian | Mark a copy available, damaged, lost or withdrawn |

### Copy search

| Method | Endpoint | Who can call it | Purpose |
|---|---|---|---|
| GET | `/api/copies` | Admin, Librarian | Search all copies by code, title or ISBN; filter by status |

### Authors, publishers, categories

| Method | Endpoint | Who can call it | Purpose |
|---|---|---|---|
| GET | `/api/authors` | Public | List / search authors |
| POST | `/api/authors` | Admin, Librarian | Add author |
| PUT | `/api/authors/{id}` | Admin, Librarian | Edit author |
| DELETE | `/api/authors/{id}` | Admin | Delete author (only if no books) |
| GET | `/api/publishers` | Public | List / search publishers |
| POST | `/api/publishers` | Admin, Librarian | Add publisher |
| PUT | `/api/publishers/{id}` | Admin, Librarian | Edit publisher |
| DELETE | `/api/publishers/{id}` | Admin | Delete publisher (only if no books) |
| GET | `/api/categories` | Public | List / search categories |
| POST | `/api/categories` | Admin, Librarian | Add category |
| PUT | `/api/categories/{id}` | Admin, Librarian | Edit category |
| DELETE | `/api/categories/{id}` | Admin | Delete category (only if no books) |

### Members

| Method | Endpoint | Who can call it | Purpose |
|---|---|---|---|
| GET | `/api/members` | Admin, Librarian | Search members (q, status, page, size, sort) |
| GET | `/api/members/{id}` | Admin, Librarian | One member with books out and fines owed |
| POST | `/api/members` | Admin, Librarian | Register a member (optionally with a login) |
| PUT | `/api/members/{id}` | Admin, Librarian | Edit member details |
| PATCH | `/api/members/{id}/status` | Admin, Librarian | Activate / deactivate a member |
| DELETE | `/api/members/{id}` | Admin | Delete a member with no history (otherwise deactivate) |
| POST | `/api/members/{id}/login` | Admin, Librarian | Create a portal login for an existing member |
| GET | `/api/members/{id}/borrowings` | Admin, Librarian | Borrowing history of a member |
| GET | `/api/members/{id}/fines` | Admin, Librarian | Fines of a member |
| GET | `/api/members/{id}/payments` | Admin, Librarian | Payments of a member |
| GET | `/api/members/{id}/reservations` | Admin, Librarian | Reservations of a member |

### Circulation (issue / return / renew)

| Method | Endpoint | Who can call it | Purpose |
|---|---|---|---|
| POST | `/api/borrowings/issue` | Admin, Librarian | Issue a copy to a member (checks limit, availability and unpaid fines) |
| GET | `/api/borrowings/{id}/return-preview` | Admin, Librarian | Calculate the fine for a return without saving anything |
| POST | `/api/borrowings/{id}/return` | Admin, Librarian | Return a book; the fine is calculated automatically |
| POST | `/api/borrowings/{id}/renew` | Any logged-in user | Renew a loan (staff, or the member who borrowed it) |
| GET | `/api/borrowings` | Admin, Librarian | List borrowings (q, status, overdue=true, page, size, sort) |
| GET | `/api/borrowings/{id}` | Any logged-in user | One borrowing (members: only their own) |
| GET | `/api/borrowings/member/{memberId}` | Admin, Librarian | Borrowings of one member |

### Fines & payments

| Method | Endpoint | Who can call it | Purpose |
|---|---|---|---|
| GET | `/api/fines` | Admin, Librarian | List fines (q, status) |
| GET | `/api/fines/member/{memberId}` | Admin, Librarian | Fines of one member |
| POST | `/api/fines/{id}/payment` | Admin, Librarian | Record a (mock/offline) payment — no real money is processed |
| GET | `/api/fines/{id}/payments` | Admin, Librarian | Payments made against one fine |
| POST | `/api/fines/{id}/waive` | Admin | Waive the remaining amount of a fine (reason required) |
| GET | `/api/payments` | Admin, Librarian | Payment history (q, method, from, to) |

### Reservations

| Method | Endpoint | Who can call it | Purpose |
|---|---|---|---|
| POST | `/api/reservations` | Any logged-in user | Reserve a book with no free copy (members reserve for themselves; staff pass memberId) |
| GET | `/api/reservations` | Admin, Librarian | List reservations (q, status) |
| DELETE | `/api/reservations/{id}` | Any logged-in user | Cancel a reservation (staff, or the member who made it) |

### Member self-service

| Method | Endpoint | Who can call it | Purpose |
|---|---|---|---|
| GET | `/api/me` | Member | My member profile |
| PUT | `/api/me` | Member | Update my phone / address |
| GET | `/api/me/borrowings/current` | Member | Books I have now |
| GET | `/api/me/borrowings` | Member | My borrowing history |
| GET | `/api/me/fines` | Member | My fines |
| GET | `/api/me/payments` | Member | My payments |
| GET | `/api/me/reservations` | Member | My reservations |
| GET | `/api/me/notifications` | Member | My notifications (paged) |
| GET | `/api/me/notifications/unread-count` | Member | Number of unread notifications |
| PATCH | `/api/me/notifications/{id}/read` | Member | Mark one notification read |
| PATCH | `/api/me/notifications/read-all` | Member | Mark all notifications read |

### Dashboards, reports, settings, audit

| Method | Endpoint | Who can call it | Purpose |
|---|---|---|---|
| GET | `/api/dashboard/admin` | Admin | Admin dashboard figures and chart data |
| GET | `/api/dashboard/librarian` | Admin, Librarian | Circulation desk figures, overdue list |
| GET | `/api/dashboard/member` | Member | Member home page figures |
| GET | `/api/reports` | Admin, Librarian | List available report types |
| GET | `/api/reports/{type}` | Admin, Librarian | Run a report as JSON, or as a CSV download with format=csv |
| GET | `/api/settings` | Any logged-in user | Read library rules (fine rate, limits, ...) |
| PUT | `/api/settings` | Admin | Change library rules |
| GET | `/api/audit-logs` | Admin | Audit trail (q, entityType, from, to) |

_76 endpoints in total._

## Example: issue → return → pay

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"librarian","password":"Librarian@123"}' | jq -r .token)

# Issue copy LIB-CC-002 to member 1
curl -X POST localhost:8080/api/borrowings/issue -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"memberId":1,"copyCode":"LIB-CC-002"}'

# Preview the fine for borrowing 21 if returned on 15 Oct 2026 (nothing is saved)
curl "localhost:8080/api/borrowings/21/return-preview?returnDate=2026-10-15" -H "Authorization: Bearer $TOKEN"

# Return it (fine is calculated and saved automatically)
curl -X POST localhost:8080/api/borrowings/21/return -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"bookCondition":"GOOD"}'

# Record a UPI payment of ₹25 against fine 5 (offline/mock payment)
curl -X POST localhost:8080/api/fines/5/payment -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"amount":25,"paymentMethod":"UPI","referenceNumber":"UPI-4471"}'

# Download the overdue report as CSV
curl -OJ "localhost:8080/api/reports/overdue?format=csv" -H "Authorization: Bearer $TOKEN"
```
