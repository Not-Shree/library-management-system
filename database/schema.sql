-- =====================================================================
--  Library Management & Fine Calculator System — PostgreSQL schema
--  Run once on an empty database:
--     psql -U library_user -d library_db -f database/schema.sql
-- =====================================================================

-- pgcrypto is used only by seed.sql to create BCrypt hashes for the demo users.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------
-- Security
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE CHECK (name IN ('ADMIN', 'LIBRARIAN', 'MEMBER'))
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,              -- BCrypt hash, never plain text
    full_name     VARCHAR(120) NOT NULL,
    role_id       BIGINT       NOT NULL REFERENCES roles (id),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_role ON users (role_id);

-- ---------------------------------------------------------------------
-- Members
-- "Current borrowed books" and "outstanding fine" are NOT stored here.
-- They are calculated from borrowings and fines so they can never drift.
-- ---------------------------------------------------------------------
CREATE TABLE members (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT UNIQUE REFERENCES users (id) ON DELETE SET NULL, -- optional portal login
    member_code       VARCHAR(30)  NOT NULL UNIQUE,                          -- student / employee ID
    full_name         VARCHAR(120) NOT NULL,
    email             VARCHAR(120) NOT NULL UNIQUE,
    phone             VARCHAR(20),
    department        VARCHAR(80),
    course            VARCHAR(80),
    year_of_study     INTEGER CHECK (year_of_study BETWEEN 1 AND 6),
    address           VARCHAR(255),
    membership_date   DATE        NOT NULL DEFAULT CURRENT_DATE,
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    max_books_allowed INTEGER CHECK (max_books_allowed > 0),                 -- NULL = use library setting
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_members_name ON members (LOWER(full_name));

-- ---------------------------------------------------------------------
-- Catalogue
-- ---------------------------------------------------------------------
CREATE TABLE authors (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120) NOT NULL UNIQUE,
    biography  VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE publishers (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120) NOT NULL UNIQUE,
    address    VARCHAR(255),
    website    VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Books are soft-deleted (deleted = TRUE) so borrowing history is never destroyed.
-- Total / available copies are calculated from book_copies.
CREATE TABLE books (
    id               BIGSERIAL PRIMARY KEY,
    isbn             VARCHAR(20)  NOT NULL,
    title            VARCHAR(200) NOT NULL,
    subtitle         VARCHAR(200),
    author_id        BIGINT NOT NULL REFERENCES authors (id),
    publisher_id     BIGINT REFERENCES publishers (id),
    category_id      BIGINT NOT NULL REFERENCES categories (id),
    language         VARCHAR(40) NOT NULL DEFAULT 'English',
    edition          VARCHAR(40),
    publication_year INTEGER CHECK (publication_year BETWEEN 1450 AND 2100),
    description      VARCHAR(2000),
    shelf_number     VARCHAR(30),
    cover_image_url  VARCHAR(500),
    deleted          BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
-- ISBN must be unique among books that are still in the catalogue.
CREATE UNIQUE INDEX uq_books_isbn_active ON books (isbn) WHERE deleted = FALSE;
CREATE INDEX idx_books_title     ON books (LOWER(title));
CREATE INDEX idx_books_author    ON books (author_id);
CREATE INDEX idx_books_category  ON books (category_id);
CREATE INDEX idx_books_publisher ON books (publisher_id);

-- Every physical copy is tracked separately (e.g. LIB-CC-001, LIB-CC-002).
CREATE TABLE book_copies (
    id            BIGSERIAL PRIMARY KEY,
    book_id       BIGINT      NOT NULL REFERENCES books (id),
    copy_code     VARCHAR(40) NOT NULL UNIQUE,
    status        VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
                  CHECK (status IN ('AVAILABLE', 'BORROWED', 'RESERVED', 'LOST', 'DAMAGED', 'WITHDRAWN')),
    acquired_date DATE,
    notes         VARCHAR(255),
    version       BIGINT    NOT NULL DEFAULT 0,   -- optimistic locking (JPA @Version)
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_copies_book_status ON book_copies (book_id, status);

-- ---------------------------------------------------------------------
-- Circulation
-- ---------------------------------------------------------------------
CREATE TABLE borrowings (
    id                  BIGSERIAL PRIMARY KEY,
    member_id           BIGINT NOT NULL REFERENCES members (id),
    book_copy_id        BIGINT NOT NULL REFERENCES book_copies (id),
    issue_date          DATE   NOT NULL,
    due_date            DATE   NOT NULL,
    renewal_count       INTEGER NOT NULL DEFAULT 0 CHECK (renewal_count >= 0),
    status              VARCHAR(20) NOT NULL DEFAULT 'BORROWED' CHECK (status IN ('BORROWED', 'RETURNED')),
    issued_by           BIGINT REFERENCES users (id),
    due_reminder_sent   BOOLEAN NOT NULL DEFAULT FALSE,
    overdue_notice_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_due_after_issue CHECK (due_date >= issue_date)
);
-- Database-level guarantee: a copy can be out with only ONE member at a time.
CREATE UNIQUE INDEX uq_active_borrowing_per_copy ON borrowings (book_copy_id) WHERE status = 'BORROWED';
CREATE INDEX idx_borrowings_member ON borrowings (member_id, status);
CREATE INDEX idx_borrowings_due    ON borrowings (status, due_date);
CREATE INDEX idx_borrowings_issue  ON borrowings (issue_date);

-- One row per completed return.
CREATE TABLE returns (
    id             BIGSERIAL PRIMARY KEY,
    borrowing_id   BIGINT NOT NULL UNIQUE REFERENCES borrowings (id),
    return_date    DATE   NOT NULL,
    overdue_days   INTEGER NOT NULL DEFAULT 0 CHECK (overdue_days >= 0),
    book_condition VARCHAR(20) NOT NULL DEFAULT 'GOOD' CHECK (book_condition IN ('GOOD', 'DAMAGED', 'LOST')),
    remarks        VARCHAR(255),
    received_by    BIGINT REFERENCES users (id),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_returns_date ON returns (return_date);

-- A fine is created only when a book comes back late.
-- fine_per_day is copied from settings at return time, so changing the
-- rate later never rewrites old fines.
CREATE TABLE fines (
    id            BIGSERIAL PRIMARY KEY,
    borrowing_id  BIGINT NOT NULL UNIQUE REFERENCES borrowings (id),
    overdue_days  INTEGER        NOT NULL CHECK (overdue_days > 0),
    fine_per_day  NUMERIC(10, 2) NOT NULL CHECK (fine_per_day >= 0),
    amount        NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    paid_amount   NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    waived_amount NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (waived_amount >= 0),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING', 'PARTIALLY_PAID', 'PAID', 'WAIVED')),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_fine_not_overpaid CHECK (paid_amount + waived_amount <= amount)
);
CREATE INDEX idx_fines_status ON fines (status);

-- Mock / offline payments: the system records money received, it never processes it.
CREATE TABLE fine_payments (
    id               BIGSERIAL PRIMARY KEY,
    fine_id          BIGINT NOT NULL REFERENCES fines (id),
    amount           NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    balance_after    NUMERIC(10, 2) NOT NULL CHECK (balance_after >= 0),
    payment_method   VARCHAR(10) NOT NULL CHECK (payment_method IN ('CASH', 'UPI', 'CARD', 'OTHER')),
    reference_number VARCHAR(60),
    payment_date     TIMESTAMP NOT NULL DEFAULT NOW(),
    payment_status   VARCHAR(20) NOT NULL CHECK (payment_status IN ('PAID', 'PARTIAL')),
    remarks          VARCHAR(255),
    recorded_by      BIGINT REFERENCES users (id),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payments_fine ON fine_payments (fine_id);
CREATE INDEX idx_payments_date ON fine_payments (payment_date);

CREATE TABLE reservations (
    id           BIGSERIAL PRIMARY KEY,
    member_id    BIGINT NOT NULL REFERENCES members (id),
    book_id      BIGINT NOT NULL REFERENCES books (id),
    reserved_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    status       VARCHAR(20) NOT NULL DEFAULT 'WAITING'
                 CHECK (status IN ('WAITING', 'AVAILABLE', 'FULFILLED', 'CANCELLED', 'EXPIRED')),
    held_copy_id BIGINT REFERENCES book_copies (id),  -- copy kept aside once status = AVAILABLE
    available_at TIMESTAMP,
    expiry_date  DATE,                                 -- last day to collect the held copy
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
-- A member can hold only one active reservation per book.
CREATE UNIQUE INDEX uq_active_reservation ON reservations (member_id, book_id) WHERE status IN ('WAITING', 'AVAILABLE');
-- First-come-first-served queue lookup.
CREATE INDEX idx_reservation_queue ON reservations (book_id, status, reserved_at);

CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT NOT NULL REFERENCES members (id),
    type       VARCHAR(30) NOT NULL CHECK (type IN ('DUE_SOON', 'OVERDUE', 'FINE_GENERATED', 'RESERVATION_AVAILABLE',
                                                    'RESERVATION_EXPIRED', 'BOOK_ISSUED', 'BOOK_RETURNED', 'BOOK_RENEWED')),
    title      VARCHAR(150) NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_member ON notifications (member_id, is_read, created_at DESC);

-- ---------------------------------------------------------------------
-- Configuration & auditing
-- ---------------------------------------------------------------------
-- Exactly one row (id = 1). The fine calculator reads every rule from here.
CREATE TABLE library_settings (
    id                          BIGINT PRIMARY KEY CHECK (id = 1),
    fine_per_day                NUMERIC(10, 2) NOT NULL DEFAULT 5   CHECK (fine_per_day >= 0),
    max_fine_per_book           NUMERIC(10, 2) NOT NULL DEFAULT 500 CHECK (max_fine_per_book >= 0),
    grace_period_days           INTEGER NOT NULL DEFAULT 0  CHECK (grace_period_days >= 0),
    max_books_per_member        INTEGER NOT NULL DEFAULT 3  CHECK (max_books_per_member > 0),
    loan_period_days            INTEGER NOT NULL DEFAULT 14 CHECK (loan_period_days > 0),
    max_renewals                INTEGER NOT NULL DEFAULT 2  CHECK (max_renewals >= 0),
    renewal_period_days         INTEGER NOT NULL DEFAULT 7  CHECK (renewal_period_days > 0),
    allow_renewal_when_overdue  BOOLEAN NOT NULL DEFAULT FALSE,
    block_issue_on_unpaid_fines BOOLEAN NOT NULL DEFAULT TRUE,
    fine_block_threshold        NUMERIC(10, 2) NOT NULL DEFAULT 100 CHECK (fine_block_threshold >= 0),
    reservation_hold_days       INTEGER NOT NULL DEFAULT 3  CHECK (reservation_hold_days > 0),
    due_reminder_days           INTEGER NOT NULL DEFAULT 2  CHECK (due_reminder_days >= 0),
    updated_by                  VARCHAR(50),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    username    VARCHAR(50),
    action      VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   BIGINT,
    details     VARCHAR(1000),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_created ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_entity  ON audit_logs (entity_type, entity_id);

-- ---------------------------------------------------------------------
-- Essential rows: the application cannot run without the roles and the
-- settings row, so they are created here (seed.sql adds demo data only).
-- ---------------------------------------------------------------------
INSERT INTO roles (id, name) VALUES (1, 'ADMIN'), (2, 'LIBRARIAN'), (3, 'MEMBER');
SELECT setval(pg_get_serial_sequence('roles', 'id'), 3);
INSERT INTO library_settings (id) VALUES (1);   -- all columns take their defaults
