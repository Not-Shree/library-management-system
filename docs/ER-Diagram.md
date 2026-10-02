# ER Diagram

Rendered automatically on GitHub (Mermaid). The source of truth is `database/schema.sql`.

```mermaid
erDiagram
    ROLES ||--o{ USERS : "has"
    USERS |o--o| MEMBERS : "login for"
    AUTHORS ||--o{ BOOKS : "writes"
    PUBLISHERS |o--o{ BOOKS : "publishes"
    CATEGORIES ||--o{ BOOKS : "groups"
    BOOKS ||--o{ BOOK_COPIES : "has physical"
    MEMBERS ||--o{ BORROWINGS : "borrows"
    BOOK_COPIES ||--o{ BORROWINGS : "is lent in"
    USERS |o--o{ BORROWINGS : "issued by"
    BORROWINGS ||--o| RETURNS : "closed by"
    BORROWINGS ||--o| FINES : "may create"
    FINES ||--o{ FINE_PAYMENTS : "paid by"
    MEMBERS ||--o{ RESERVATIONS : "places"
    BOOKS ||--o{ RESERVATIONS : "queued for"
    BOOK_COPIES |o--o{ RESERVATIONS : "held for"
    MEMBERS ||--o{ NOTIFICATIONS : "receives"
    USERS |o--o{ AUDIT_LOGS : "performed"

    ROLES {
        bigint id PK
        varchar name UK "ADMIN, LIBRARIAN, MEMBER"
    }
    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password_hash "BCrypt"
        varchar full_name
        bigint role_id FK
        boolean enabled
    }
    MEMBERS {
        bigint id PK
        bigint user_id FK "nullable, unique"
        varchar member_code UK
        varchar full_name
        varchar email UK
        varchar phone
        varchar department
        varchar course
        int year_of_study
        date membership_date
        varchar status "ACTIVE/INACTIVE"
        int max_books_allowed "null = library default"
    }
    AUTHORS {
        bigint id PK
        varchar name UK
        text biography
    }
    PUBLISHERS {
        bigint id PK
        varchar name UK
        varchar address
        varchar website
    }
    CATEGORIES {
        bigint id PK
        varchar name UK
        varchar description
    }
    BOOKS {
        bigint id PK
        varchar isbn "unique while not deleted"
        varchar title
        bigint author_id FK
        bigint publisher_id FK
        bigint category_id FK
        varchar language
        varchar edition
        int publication_year
        varchar shelf_number
        boolean deleted "soft delete"
    }
    BOOK_COPIES {
        bigint id PK
        bigint book_id FK
        varchar copy_code UK "e.g. LIB-CC-001"
        varchar status
        date acquired_date
        bigint version "optimistic lock"
    }
    BORROWINGS {
        bigint id PK
        bigint member_id FK
        bigint book_copy_id FK
        date issue_date
        date due_date
        int renewal_count
        varchar status "BORROWED/RETURNED"
        bigint issued_by FK
    }
    RETURNS {
        bigint id PK
        bigint borrowing_id FK,UK
        date return_date
        int overdue_days
        varchar book_condition
        bigint received_by FK
    }
    FINES {
        bigint id PK
        bigint borrowing_id FK,UK
        int overdue_days
        numeric fine_per_day "rate at the time"
        numeric amount
        numeric paid_amount
        numeric waived_amount
        varchar status
    }
    FINE_PAYMENTS {
        bigint id PK
        bigint fine_id FK
        numeric amount
        numeric balance_after
        varchar payment_method
        varchar reference_number
        timestamp payment_date
        varchar payment_status
        bigint recorded_by FK
    }
    RESERVATIONS {
        bigint id PK
        bigint member_id FK
        bigint book_id FK
        timestamp reserved_at
        varchar status
        bigint held_copy_id FK
        date expiry_date
    }
    NOTIFICATIONS {
        bigint id PK
        bigint member_id FK
        varchar type
        varchar title
        varchar message
        boolean is_read
    }
    LIBRARY_SETTINGS {
        smallint id PK "always 1"
        numeric fine_per_day
        numeric max_fine_per_book
        int grace_period_days
        int max_books_per_member
        int loan_period_days
        int max_renewals
        int renewal_period_days
        boolean block_issue_on_unpaid_fines
        numeric fine_block_threshold
        int reservation_hold_days
    }
    AUDIT_LOGS {
        bigint id PK
        bigint user_id FK
        varchar username
        varchar action
        varchar entity_type
        bigint entity_id
        text details
        timestamp created_at
    }
```

## Design notes (useful in a viva)

- **Book vs. copy.** `books` is the title (one ISBN); `book_copies` is each physical item with its own code. Two copies of *Clean Code* are one `books` row and two `book_copies` rows.
- **Nothing that can be calculated is stored.** Total/available copies, a member's current number of books and outstanding fines are computed with `COUNT`/`SUM`, so they can never go out of sync.
- **Overdue is derived**, not a stored status: a borrowing is overdue when `status = 'BORROWED' AND due_date < today`.
- **Integrity in the database, not only in Java:** `CHECK` constraints (e.g. `due_date >= issue_date`, `paid_amount + waived_amount <= amount`), a partial unique index so one copy can never have two active loans, and `ON DELETE RESTRICT` foreign keys so history cannot be deleted by accident.
- **Normalisation:** every non-key column depends only on its table's key (3NF). Author, publisher and category names live in their own tables and are referenced by id.
