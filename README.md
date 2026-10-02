# Library Management & Fine Calculator System

A full-stack web application for a college library: catalogue, members, issue / return / renewal, **automatic fine calculation**, offline fine payments, reservations with a first-come-first-served queue, in-app notifications, dashboards and reports.

Built with **React + Vite + Bootstrap** on the front end, **Java 21 + Spring Boot 3** on the back end, and **PostgreSQL**. Every library rule is enforced on the server.

It runs as a **website** and as an **Android app** (the same React code packaged with Capacitor) — see [docs/Android-App.md](docs/Android-App.md).

---

## Contents

1. [Overview](#1-overview)
2. [Features](#2-features)
3. [Technology stack](#3-technology-stack)
4. [Architecture and project structure](#4-architecture-and-project-structure)
5. [Database schema](#5-database-schema)
6. [Requirements](#6-requirements)
7. [PostgreSQL setup](#7-postgresql-setup)
8. [Backend setup](#8-backend-setup)
9. [Frontend setup](#9-frontend-setup)
10. [Environment variables](#10-environment-variables)
11. [How to run](#11-how-to-run)
12. [Demo credentials](#12-demo-credentials)
13. [API documentation](#13-api-documentation)
14. [Screenshots](#14-screenshots)
15. [Future enhancements](#15-future-enhancements)

Also: [Android app](#android-app) · [Deployment](#deployment) · [Troubleshooting](#troubleshooting) · [Build and verification status](#build-and-verification-status)

---

## 1. Overview

Three kinds of users work with the system:

| Role | What they do |
|---|---|
| **Admin** | Everything a librarian does, plus staff accounts, library settings (fine rate, limits, loan period), waiving fines, deleting records, and audit logs. |
| **Librarian** | Runs the circulation desk: issues, returns and renews books, records fine payments, manages books, copies and members, handles reservations, runs reports. |
| **Member** (student / staff) | Browses the catalogue, reserves books that are out, sees their borrowed books, due dates, fines, payments and notifications, renews books, and edits their contact details. |

The catalogue can also be browsed without logging in.

**Fine rule (configurable):** `fine = min((days late − grace days) × fine per day, maximum per book)`.
With the defaults (₹5/day, no grace, max ₹500), a book due on 10 Oct 2026 and returned on 15 Oct 2026 has a fine of **₹25**.

## 2. Features

**Catalogue**
- Books with ISBN, author, publisher, category, language, edition, year, shelf number, description and cover image.
- Each title has separate **physical copies** with their own codes (e.g. `LIB-CC-001`) and statuses: available, borrowed, reserved (held), damaged, lost, withdrawn.
- Search by title, ISBN, author, publisher or category; filter by category, author, language, availability and year range; sort and paginate.
- Books are soft-deleted, so borrowing history is never lost.

**Circulation desk**
- **Issue:** find the member, pick a free copy (or type/scan its code). The due date is set from the loan period. Blocks inactive members, members at their limit, members over the unpaid-fine limit, unavailable copies, and a second copy of the same title.
- **Return:** see the fine *before* confirming (receipt-style breakdown), choose the return date and book condition (good / damaged / lost). The fine is created automatically. If someone reserved the book, the copy is set aside for them and the librarian is told.
- **Renew:** up to the maximum renewals, not when overdue (configurable), and not when another member has reserved the book.

**Fines and payments**
- Automatic fines using the settings in force at return time; estimated fines shown for books still out.
- Record full or part payments by cash, UPI, card or other (**offline/mock — no gateway**), with reference numbers for UPI/card. Overpayment is impossible.
- Admin can waive the remainder of a fine with a reason.

**Reservations**
- "Currently unavailable — Reserve book" for titles with no free copy; one active reservation per member per title.
- First come, first served. A returned copy is held for the next member for a set number of days; uncollected holds expire and pass to the next person.
- Statuses: waiting, ready to collect, fulfilled, cancelled, expired.

**Members and staff**
- Member records (member ID, department, course, year, contact details), per-member borrowing limit, activate/deactivate, optional portal login.
- Self-registration for members. Admin manages librarian/admin accounts; staff reset passwords.

**Dashboards and reports**
- Admin, librarian and member dashboards with charts (issued vs. returned per month, fine collection, most borrowed books, most active members, books by category).
- 10 reports with date ranges and **CSV export**: borrowed, overdue, returned, fine collection, outstanding fines, most borrowed, most active members, books by category, inventory, reservations.

**Notifications and audit**
- In-app notifications: due soon, overdue, fine added, book issued/returned/renewed, reserved book ready, hold expired. A daily job sends reminders.
- Audit log of every change: who, what, when.

**Security**
- JWT authentication, BCrypt password hashing, role-based access on every endpoint, standard error responses, secrets only in environment variables.

## 3. Technology stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite 5, JavaScript, Bootstrap 5, Bootstrap Icons, Axios, React Router 6, Recharts |
| Android app | Capacitor 8 (wraps the React frontend; plugins: App, Filesystem, Share, Splash Screen) |
| Backend | Java 21, Spring Boot 3.3 (Web, Data JPA / Hibernate, Security, Validation), JJWT 0.12, springdoc-openapi (Swagger UI), Maven |
| Database | PostgreSQL 14+ (tested with 16) |
| Tooling | JUnit 5, Docker Compose (optional, database only) |

All libraries are free and open source.

## 4. Architecture and project structure

```
React (browser) ──HTTP + JWT──▶ Spring Boot REST API ──JPA/JDBC──▶ PostgreSQL
                                 controller → service → repository → entity
```

- **Controllers** receive requests, validate DTOs and check roles.
- **Services** hold all business rules; each use case is one transaction.
- **Repositories** use Spring Data JPA (derived queries, JPQL, and Specifications for search).
- **DTOs** are what the API sends and receives — entities are never exposed directly.

```
library-management-system/
├── backend/                     Spring Boot API
│   ├── pom.xml
│   ├── .env.example             → copy to backend/.env
│   ├── application-example.properties
│   └── src/main/java/com/college/library/
│       ├── controller/          REST endpoints
│       ├── service/             business rules (FineCalculator, BorrowingService, ...)
│       ├── repository/          data access + search specifications
│       ├── entity/              JPA entities and enums
│       ├── dto/                 request/response records
│       ├── mapper/              entity → DTO
│       ├── security/            JWT, filters, security config
│       ├── exception/           error handling
│       ├── config/              Swagger
│       └── util/
├── frontend/                    React app
│   ├── .env.example             → copy to frontend/.env
│   ├── capacitor.config.json    Android app settings
│   ├── android/                 Android Studio project (generated by Capacitor)
│   ├── assets/                  app icon and splash source images
│   └── src/
│       ├── api/                 Axios client (adds the JWT)
│       ├── context/             auth + toast state
│       ├── components/          layout, tables, modals, charts
│       └── pages/               public/, staff/, member/
├── database/
│   ├── schema.sql               tables, constraints, indexes, default settings
│   └── seed.sql                 demo data
├── docs/
│   ├── ER-Diagram.md (+ .png)
│   ├── API-Documentation.md
│   ├── Android-App.md           build and run the Android app
│   ├── Deployment.md            free hosting on Neon + Render
│   └── Project-Documentation.md business rules, design decisions, viva notes
├── docker-compose.yml           optional PostgreSQL container
├── render.yaml                  Render blueprint (deployment)
├── .github/workflows/           builds the Android APK on GitHub
├── .env.example                 for docker-compose only
└── README.md
```

## 5. Database schema

16 tables, normalised to 3NF, with foreign keys, `CHECK` constraints and indexes. Full diagram: [docs/ER-Diagram.md](docs/ER-Diagram.md).

| Table | Holds |
|---|---|
| `roles`, `users` | Login accounts and their role (ADMIN, LIBRARIAN, MEMBER) |
| `members` | Library members; optionally linked to a `users` login |
| `authors`, `publishers`, `categories` | Catalogue lookups |
| `books` | Titles (soft-deletable) |
| `book_copies` | Physical copies with codes and statuses |
| `borrowings` | Loans (issue date, due date, renewals) |
| `returns` | Return date, days late and condition |
| `fines`, `fine_payments` | Fines and the offline payments against them |
| `reservations` | The reservation queue and held copies |
| `notifications` | In-app messages for members |
| `library_settings` | One row with every configurable rule |
| `audit_logs` | Who changed what, and when |

Values that can be calculated are **not stored**: available copies, a member's current book count and outstanding fines are computed by queries, so they are always correct. "Overdue" is derived from the due date.

`schema.sql` is the source of truth. Hibernate does not create or alter tables (`spring.jpa.hibernate.ddl-auto=none`). Set `JPA_DDL_AUTO=validate` if you want the app to check at start-up that the entities match the tables.

## 6. Requirements

| Software | Version | Check with |
|---|---|---|
| Java JDK | 21 | `java -version` |
| Maven | 3.9+ (or use your IDE's built-in Maven) | `mvn -v` |
| Node.js | 18 or newer for the website; **22 or newer** for the Android app | `node -v` |
| Android Studio (only for the Android app) | latest | — |
| PostgreSQL | 14 or newer | `psql --version` |
| Docker (optional) | any recent | `docker -v` |

## 7. PostgreSQL setup

Skip this section if you use Docker (Option 2 in section 11).

1. Open a PostgreSQL shell as the `postgres` superuser:
   ```bash
   psql -U postgres            # Windows: open "SQL Shell (psql)" from the Start menu
   ```
2. Create a user and a database it owns (choose your own password):
   ```sql
   CREATE USER library_user WITH PASSWORD 'your_password';
   CREATE DATABASE library_db OWNER library_user;
   \q
   ```
3. Load the schema, then the demo data, **in this order**, from the project root:
   ```bash
   psql -U library_user -d library_db -h localhost -f database/schema.sql
   psql -U library_user -d library_db -h localhost -f database/seed.sql
   ```
   `schema.sql` enables the `pgcrypto` extension (used by `seed.sql` to hash the demo passwords). On PostgreSQL 13+ the database owner can do this. On older versions, run `CREATE EXTENSION pgcrypto;` once as `postgres` first.

To start again from scratch: `DROP DATABASE library_db;` as `postgres`, then repeat steps 2–3.

`seed.sql` places the demo loans relative to **today**, so there are always overdue books and due-soon reminders to look at, whenever you load it.

## 8. Backend setup

```bash
cd backend
cp .env.example .env          # Windows: copy .env.example .env
```

Edit `backend/.env`:

- `DB_PASSWORD` — the password you chose in section 7.
- `JWT_SECRET` — any random string of **at least 32 characters**. Generate one with `openssl rand -base64 48`, or type a long random sentence. The app will not start without it.

Then run:

```bash
mvn spring-boot:run
```

The first run downloads dependencies (a few minutes). When you see `Started LibraryApplication`, the API is live at **http://localhost:8080** and Swagger UI at **http://localhost:8080/swagger-ui.html**.

Run the unit tests with `mvn test`. To build a runnable jar: `mvn clean package`, then `java -jar target/library-management-system-1.0.0.jar` (run it from the `backend` folder so it finds `.env`).

**IntelliJ IDEA / Eclipse:** open the `backend` folder as a Maven project and run `LibraryApplication`. Make sure the run configuration's working directory is the `backend` folder, so `.env` is found.

## 9. Frontend setup

```bash
cd frontend
cp .env.example .env          # Windows: copy .env.example .env
npm install
npm run dev
```

Open **http://localhost:5173**. `VITE_API_BASE_URL` in `frontend/.env` must point to the backend (default `http://localhost:8080/api`).

For a production build: `npm run build` (output in `frontend/dist`), preview it with `npm run preview`.

## 10. Environment variables

**Backend** (`backend/.env` or real environment variables):

| Variable | Required | Default | Meaning |
|---|---|---|---|
| `DB_URL` | no | `jdbc:postgresql://localhost:5432/library_db` | JDBC URL |
| `DB_USERNAME` | no | `library_user` | Database user |
| `DB_PASSWORD` | **yes** | — | Database password |
| `JWT_SECRET` | **yes** | — | Token signing secret, 32+ characters |
| `JWT_EXPIRATION_MINUTES` | no | `480` | How long a login lasts |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:5173` | Frontend URL(s), comma-separated |
| `SERVER_PORT` | no | `8080` | API port |
| `JPA_DDL_AUTO` | no | `none` | Set to `validate` to check entities against the tables at start-up |
| `APP_TIMEZONE` | no | `Asia/Kolkata` | Time zone used for "today" (due dates, overdue). To change it, set it as a real OS environment variable — it is read before `.env` is loaded. |

**Frontend** (`frontend/.env`): `VITE_API_BASE_URL` (default `http://localhost:8080/api`).

**Docker** (root `.env`, only for `docker-compose.yml`): `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`.

The `.env` files are git-ignored. Only the `.env.example` files, which contain placeholders, are committed.

## 11. How to run

### Option 1 — everything installed locally

1. Set up PostgreSQL (section 7).
2. Terminal 1: `cd backend && mvn spring-boot:run`
3. Terminal 2: `cd frontend && npm run dev`
4. Open http://localhost:5173 and log in with a demo account (section 12).

### Option 2 — PostgreSQL in Docker

```bash
cp .env.example .env              # set POSTGRES_PASSWORD
docker compose up -d              # starts PostgreSQL and loads schema.sql + seed.sql
```

Put the same password in `backend/.env` as `DB_PASSWORD`, then start the backend and frontend exactly as in Option 1 (steps 2–4).

The scripts are loaded only the first time, into an empty data volume. To reset the database: `docker compose down -v && docker compose up -d`.

### Suggested demo flow (about 5 minutes)

1. Log in as **librarian** → the dashboard shows 3 overdue books.
2. **Return book** → search `LIB-DSC-001` (overdue) → see the fine slip → confirm → **Collect now** and record a UPI payment with any reference number.
3. **Issue book** → choose **Prof. Sanjay Gupta (EMP-1002)**, who owes ₹500 → try to issue any copy → the unpaid-fine rule blocks it.
4. **Renew book** → pick *Design Patterns* borrowed by Dr. Meera Rao → blocked, because Riya has reserved it.
5. Log in as **student** (Riya) → **My borrowed books** → renew *Clean Code* (works) and *Refactoring* (blocked: overdue) → **Browse books** → reserve *Algorithms Unlocked*.
6. Log in as **admin** → **Settings** → change the fine per day and watch the preview → **Reports** → download the overdue report as CSV → **Audit logs** → see everything you just did.

## 12. Demo credentials

> ⚠️ **For local testing only.** These accounts are created by `database/seed.sql`. Change or delete them before any real use.

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `Admin@123` |
| Librarian | `librarian` | `Librarian@123` |
| Member (student Riya Patil, STU-2026-001) | `student` | `Student@123` |

The login page has buttons that fill these in.

## 13. API documentation

- **Swagger UI:** http://localhost:8080/swagger-ui.html (click **Authorize** and paste the token from `POST /api/auth/login`).
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **Written reference with all 76 endpoints, roles, error format and curl examples:** [docs/API-Documentation.md](docs/API-Documentation.md)

Every error uses the same JSON shape:

```json
{ "timestamp": "2026-09-25T10:15:30", "status": 400, "error": "BORROW_LIMIT_EXCEEDED",
  "message": "Riya Patil already has 3 of 3 allowed books", "path": "/api/borrowings/issue" }
```

## 14. Screenshots

Add screenshots to `docs/screenshots/` and link them here. Suggested set:

| Screen | File |
|---|---|
| Login page | `docs/screenshots/login.png` |
| Admin dashboard | `docs/screenshots/admin-dashboard.png` |
| Librarian dashboard | `docs/screenshots/librarian-dashboard.png` |
| Issue book | `docs/screenshots/issue-book.png` |
| Return book with fine slip | `docs/screenshots/return-fine.png` |
| Fines and payment dialog | `docs/screenshots/fines.png` |
| Book catalogue | `docs/screenshots/catalogue.png` |
| Member dashboard | `docs/screenshots/member-dashboard.png` |
| Reports | `docs/screenshots/reports.png` |

## 15. Future enhancements

Not included in this version (and not simulated in the UI):

- **PDF export** of reports (e.g. with OpenPDF) — CSV is available now.
- **Email / SMS notifications** via SMTP or an SMS gateway — notifications are in-app today.
- **Self-service password reset** by email link — today staff reset passwords.
- **Real online payments** (UPI / card gateway) — today payments are recorded offline by staff.
- Barcode scanning with a phone camera; printed spine labels for copy codes.
- Refresh tokens and "log out from all devices".
- Multiple library branches and inter-branch transfers.
- Acquisition workflow (purchase requests, vendors, budgets) and e-book lending.
- Integration tests with Testcontainers and a CI pipeline.

---

## Android app

The React frontend also builds as an Android app with Capacitor:

```bash
cd frontend
npm install
npm run android:build   # build the web app and copy it into frontend/android
npm run android:open    # open in Android Studio, then press Run
```

**No Android Studio?** Push the repo to GitHub, open **Actions → Build Android APK → Run workflow**, then download `college-library.apk` from **Releases → Android APK (latest)**.

The emulator reaches the backend on your computer at `http://10.0.2.2:8080/api` (the app's default). On a real phone, set your computer's Wi-Fi IP (e.g. `http://192.168.1.5:8080/api`) from **Server → Change** on the login screen. Full guide, APK building and troubleshooting: **[docs/Android-App.md](docs/Android-App.md)**.

## Deployment

Free deployment with **Neon** (database) and **Render** (backend + frontend), using the included `backend/Dockerfile` and `render.yaml` blueprint: see **[docs/Deployment.md](docs/Deployment.md)**.

## Troubleshooting

| Problem | Fix |
|---|---|
| Backend exits with `JWT_SECRET is missing or shorter than 32 characters` | Set `JWT_SECRET` in `backend/.env` and run from the `backend` folder. |
| `password authentication failed for user "library_user"` | `DB_PASSWORD` in `backend/.env` does not match the database password. |
| `relation "users" does not exist` | `schema.sql` was not loaded into `library_db` (section 7, step 3). |
| Demo login says "Invalid username or password" | `seed.sql` was not loaded, or was loaded before `schema.sql`. Reload both. |
| Frontend says "Cannot reach the server" | The backend is not running, or `VITE_API_BASE_URL` is wrong. Restart `npm run dev` after editing `.env`. |
| Browser console shows a CORS error | Add the frontend URL to `CORS_ALLOWED_ORIGINS`. |
| `ERROR: permission denied to create extension "pgcrypto"` | PostgreSQL is older than 13: run `CREATE EXTENSION pgcrypto;` as `postgres`, then load the scripts again. |
| Port 5432 / 8080 / 5173 already in use | Stop the other program, or change `POSTGRES_PORT`, `SERVER_PORT`, or the Vite port. |

## Build and verification status

What was checked while building this project:

- `schema.sql` and `seed.sql` load cleanly into PostgreSQL 16.
- All 152 entity fields match the database columns exactly.
- All backend Java sources type-check with Java 21 with no errors; the dashboard's native SQL queries return correct results on the demo data; the fine calculator was run and unit-tested.
- The frontend builds for production and passes ESLint (no undefined variables, no hook misuse); the main screens were rendered in a headless browser without errors, on desktop and mobile widths.

**Not yet verified:** the Spring Boot application itself was not started, because the build environment could not download Maven dependencies. The first `mvn spring-boot:run` on your machine is the first real start-up, so if anything fails there, the stack trace will point to the exact file to fix.

See [docs/Project-Documentation.md](docs/Project-Documentation.md) for the business rules, design decisions and viva notes.
