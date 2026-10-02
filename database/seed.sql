-- =====================================================================
--  Demo / sample data.  Run AFTER schema.sql:
--     psql -U library_user -d library_db -f database/seed.sql
--
--  DEMO CREDENTIALS — for local testing only. Change them in any real deployment.
--     admin     / Admin@123
--     librarian / Librarian@123
--     student   / Student@123
--
--  All borrowing dates are relative to CURRENT_DATE, so there are always
--  current, due-soon, overdue and returned records whenever you load this file.
-- =====================================================================

BEGIN;

-- ---------- Users (roles already exist from schema.sql; passwords hashed with BCrypt by pgcrypto) ----------
INSERT INTO users (id, username, email, password_hash, full_name, role_id) VALUES
  (1, 'admin',     'admin@library.local',     crypt('Admin@123',     gen_salt('bf', 10)), 'System Administrator', 1),
  (2, 'librarian', 'librarian@library.local', crypt('Librarian@123', gen_salt('bf', 10)), 'Anjali Verma',         2),
  (3, 'student',   'riya.patil@college.edu',  crypt('Student@123',   gen_salt('bf', 10)), 'Riya Patil',           3);

-- ---------- Library settings (single row) ----------
INSERT INTO library_settings (id, fine_per_day, max_fine_per_book, grace_period_days, max_books_per_member,
                              loan_period_days, max_renewals, renewal_period_days, allow_renewal_when_overdue,
                              block_issue_on_unpaid_fines, fine_block_threshold, reservation_hold_days,
                              due_reminder_days, updated_by)
VALUES (1, 5.00, 500.00, 0, 3, 14, 2, 7, FALSE, TRUE, 100.00, 3, 2, 'admin')
ON CONFLICT (id) DO UPDATE SET
    fine_per_day = EXCLUDED.fine_per_day, max_fine_per_book = EXCLUDED.max_fine_per_book,
    grace_period_days = EXCLUDED.grace_period_days, max_books_per_member = EXCLUDED.max_books_per_member,
    loan_period_days = EXCLUDED.loan_period_days, max_renewals = EXCLUDED.max_renewals,
    renewal_period_days = EXCLUDED.renewal_period_days, allow_renewal_when_overdue = EXCLUDED.allow_renewal_when_overdue,
    block_issue_on_unpaid_fines = EXCLUDED.block_issue_on_unpaid_fines, fine_block_threshold = EXCLUDED.fine_block_threshold,
    reservation_hold_days = EXCLUDED.reservation_hold_days, due_reminder_days = EXCLUDED.due_reminder_days,
    updated_by = EXCLUDED.updated_by;

-- ---------- Categories ----------
INSERT INTO categories (id, name, description) VALUES
  (1, 'Computer Science',     'Algorithms, systems, networks and databases'),
  (2, 'Software Engineering', 'Design, craftsmanship and architecture'),
  (3, 'Mathematics',          'Linear algebra and applied mathematics'),
  (4, 'Literature',           'Fiction and classics'),
  (5, 'Business & Management','Management and professional effectiveness');

-- ---------- Authors ----------
INSERT INTO authors (id, name, biography) VALUES
  (1,  'Robert C. Martin',   'Software engineer and author known as "Uncle Bob".'),
  (2,  'Thomas H. Cormen',   'Computer scientist, co-author of Introduction to Algorithms.'),
  (3,  'Andrew S. Tanenbaum','Professor of computer science, author of classic systems textbooks.'),
  (4,  'Abraham Silberschatz','Professor of computer science, known for database and OS textbooks.'),
  (5,  'Gilbert Strang',     'Mathematician known for his linear algebra teaching.'),
  (6,  'Martin Fowler',      'Software developer and author on refactoring and architecture.'),
  (7,  'Erich Gamma',        'Co-author of Design Patterns (the "Gang of Four").'),
  (8,  'R. K. Narayan',      'Indian novelist, creator of the fictional town of Malgudi.'),
  (9,  'Jane Austen',        'English novelist of the Regency era.'),
  (10, 'Peter F. Drucker',   'Management consultant and author.');

-- ---------- Publishers ----------
INSERT INTO publishers (id, name, address, website) VALUES
  (1, 'Prentice Hall',             'Upper Saddle River, NJ, USA', NULL),
  (2, 'MIT Press',                 'Cambridge, MA, USA',          'https://mitpress.mit.edu'),
  (3, 'Pearson',                   'London, UK',                  'https://www.pearson.com'),
  (4, 'McGraw Hill',               'New York, NY, USA',           NULL),
  (5, 'Wiley',                     'Hoboken, NJ, USA',            NULL),
  (6, 'Wellesley-Cambridge Press', 'Wellesley, MA, USA',          NULL),
  (7, 'Addison-Wesley',            'Boston, MA, USA',             NULL),
  (8, 'Penguin Books',             'Gurugram, India',             NULL),
  (9, 'Harper Business',           'New York, NY, USA',           NULL);

-- ---------- Books (cover images come from the free Open Library covers service) ----------
INSERT INTO books (id, isbn, title, subtitle, author_id, publisher_id, category_id, language, edition,
                   publication_year, shelf_number, description, cover_image_url) VALUES
  (1,  '9780132350884', 'Clean Code', 'A Handbook of Agile Software Craftsmanship', 1, 1, 2, 'English', '1st', 2008, 'SE-A1', 'Principles and practices for writing readable, maintainable code.', 'https://covers.openlibrary.org/b/isbn/9780132350884-M.jpg'),
  (2,  '9780134494166', 'Clean Architecture', 'A Craftsman''s Guide to Software Structure and Design', 1, 1, 2, 'English', '1st', 2017, 'SE-A1', 'Architecture rules that keep systems flexible.', 'https://covers.openlibrary.org/b/isbn/9780134494166-M.jpg'),
  (3,  '9780137081073', 'The Clean Coder', 'A Code of Conduct for Professional Programmers', 1, 1, 2, 'English', '1st', 2011, 'SE-A1', 'Professional conduct for software developers.', 'https://covers.openlibrary.org/b/isbn/9780137081073-M.jpg'),
  (4,  '9780262046305', 'Introduction to Algorithms', NULL, 2, 2, 1, 'English', '4th', 2022, 'CS-B2', 'Comprehensive textbook covering a broad range of algorithms.', 'https://covers.openlibrary.org/b/isbn/9780262046305-M.jpg'),
  (5,  '9780262518802', 'Algorithms Unlocked', NULL, 2, 2, 1, 'English', '1st', 2013, 'CS-B2', 'A gentle introduction to how algorithms work.', 'https://covers.openlibrary.org/b/isbn/9780262518802-M.jpg'),
  (6,  '9780132126953', 'Computer Networks', NULL, 3, 3, 1, 'English', '5th', 2010, 'CS-B3', 'Layered approach to computer networking.', 'https://covers.openlibrary.org/b/isbn/9780132126953-M.jpg'),
  (7,  '9780133591620', 'Modern Operating Systems', NULL, 3, 3, 1, 'English', '4th', 2014, 'CS-B3', 'Processes, memory, file systems and security.', 'https://covers.openlibrary.org/b/isbn/9780133591620-M.jpg'),
  (8,  '9780132916523', 'Structured Computer Organization', NULL, 3, 3, 1, 'English', '6th', 2012, 'CS-B3', 'Computer architecture from digital logic upward.', 'https://covers.openlibrary.org/b/isbn/9780132916523-M.jpg'),
  (9,  '9780078022159', 'Database System Concepts', NULL, 4, 4, 1, 'English', '7th', 2019, 'CS-B4', 'Relational model, SQL, design and transactions.', 'https://covers.openlibrary.org/b/isbn/9780078022159-M.jpg'),
  (10, '9781119800361', 'Operating System Concepts', NULL, 4, 5, 1, 'English', '10th', 2021, 'CS-B4', 'The "dinosaur book" on operating systems.', 'https://covers.openlibrary.org/b/isbn/9781119800361-M.jpg'),
  (11, '9781733146678', 'Introduction to Linear Algebra', NULL, 5, 6, 3, 'English', '6th', 2023, 'MA-C1', 'Vectors, matrices and their applications.', 'https://covers.openlibrary.org/b/isbn/9781733146678-M.jpg'),
  (12, '9780692196380', 'Linear Algebra and Learning from Data', NULL, 5, 6, 3, 'English', '1st', 2019, 'MA-C1', 'Linear algebra for machine learning and data science.', 'https://covers.openlibrary.org/b/isbn/9780692196380-M.jpg'),
  (13, '9780134757599', 'Refactoring', 'Improving the Design of Existing Code', 6, 7, 2, 'English', '2nd', 2018, 'SE-A2', 'Catalogue of refactorings with JavaScript examples.', 'https://covers.openlibrary.org/b/isbn/9780134757599-M.jpg'),
  (14, '9780321127426', 'Patterns of Enterprise Application Architecture', NULL, 6, 7, 2, 'English', '1st', 2002, 'SE-A2', 'Patterns for enterprise software design.', 'https://covers.openlibrary.org/b/isbn/9780321127426-M.jpg'),
  (15, '9780201633610', 'Design Patterns', 'Elements of Reusable Object-Oriented Software', 7, 7, 2, 'English', '1st', 1994, 'SE-A2', 'The classic catalogue of 23 object-oriented design patterns.', 'https://covers.openlibrary.org/b/isbn/9780201633610-M.jpg'),
  (16, '9780143039655', 'Malgudi Days', NULL, 8, 8, 4, 'English', NULL, 2006, 'LT-D1', 'Short stories set in the town of Malgudi.', 'https://covers.openlibrary.org/b/isbn/9780143039655-M.jpg'),
  (17, '9780143039648', 'The Guide', NULL, 8, 8, 4, 'English', NULL, 2006, 'LT-D1', 'A tour guide becomes an accidental holy man.', 'https://covers.openlibrary.org/b/isbn/9780143039648-M.jpg'),
  (18, '9780141439518', 'Pride and Prejudice', NULL, 9, 8, 4, 'English', NULL, 2003, 'LT-D2', 'Classic novel of manners.', 'https://covers.openlibrary.org/b/isbn/9780141439518-M.jpg'),
  (19, '9780060833459', 'The Effective Executive', NULL, 10, 9, 5, 'English', NULL, 2006, 'BM-E1', 'What makes an executive effective.', 'https://covers.openlibrary.org/b/isbn/9780060833459-M.jpg'),
  (20, '9780887306150', 'Management', 'Tasks, Responsibilities, Practices', 10, 9, 5, 'English', NULL, 1993, 'BM-E1', 'Drucker''s comprehensive work on management.', 'https://covers.openlibrary.org/b/isbn/9780887306150-M.jpg');

-- ---------- Physical copies: generated as LIB-<code>-001, LIB-<code>-002 ... ----------
INSERT INTO book_copies (book_id, copy_code, status, acquired_date)
SELECT v.book_id, 'LIB-' || v.code || '-' || LPAD(n::TEXT, 3, '0'), 'AVAILABLE', DATE '2024-06-15'
FROM (VALUES (1,'CC',3), (2,'CA',2), (3,'TCC',2), (4,'ITA',3), (5,'AU',1), (6,'CN',2), (7,'MOS',2),
             (8,'SCO',1), (9,'DSC',2), (10,'OSC',2), (11,'ILA',2), (12,'LALD',1), (13,'RF',2), (14,'PEAA',1),
             (15,'DP',2), (16,'MD',2), (17,'GD',1), (18,'PP',2), (19,'EE',2), (20,'MGT',1)) AS v(book_id, code, copies)
CROSS JOIN LATERAL generate_series(1, v.copies) AS n
ORDER BY v.book_id, n;

-- ---------- Members ----------
INSERT INTO members (id, user_id, member_code, full_name, email, phone, department, course, year_of_study,
                     address, membership_date, status, max_books_allowed) VALUES
  (1,  3,    'STU-2026-001', 'Riya Patil',       'riya.patil@college.edu',     '9820011111', 'Computer Engineering',     'B.E.',  3, 'Andheri East, Mumbai', CURRENT_DATE - 400, 'ACTIVE', NULL),
  (2,  NULL, 'STU-2026-002', 'Aarav Deshmukh',   'aarav.deshmukh@college.edu', '9820022222', 'Computer Engineering',     'B.E.',  2, 'Thane West',           CURRENT_DATE - 300, 'ACTIVE', NULL),
  (3,  NULL, 'STU-2026-003', 'Sneha Kulkarni',   'sneha.kulkarni@college.edu', '9820033333', 'Information Technology',   'B.E.',  4, 'Dadar, Mumbai',        CURRENT_DATE - 700, 'ACTIVE', NULL),
  (4,  NULL, 'STU-2026-004', 'Rohan Mehta',      'rohan.mehta@college.edu',    '9820044444', 'Electronics',              'B.E.',  1, 'Borivali, Mumbai',     CURRENT_DATE - 60,  'ACTIVE', NULL),
  (5,  NULL, 'STU-2026-005', 'Priya Nair',       'priya.nair@college.edu',     '9820055555', 'Computer Applications',    'MCA',   1, 'Vashi, Navi Mumbai',   CURRENT_DATE - 90,  'ACTIVE', NULL),
  (6,  NULL, 'STU-2026-006', 'Kabir Shaikh',     'kabir.shaikh@college.edu',   '9820066666', 'Computer Engineering',     'B.E.',  3, 'Kurla, Mumbai',        CURRENT_DATE - 380, 'ACTIVE', NULL),
  (7,  NULL, 'STU-2026-007', 'Ananya Iyer',      'ananya.iyer@college.edu',    '9820077777', 'Mathematics',              'B.Sc.', 2, 'Chembur, Mumbai',      CURRENT_DATE - 250, 'ACTIVE', NULL),
  (8,  NULL, 'STU-2026-008', 'Vikram Joshi',     'vikram.joshi@college.edu',   '9820088888', 'Management Studies',       'BBA',   3, 'Powai, Mumbai',        CURRENT_DATE - 420, 'INACTIVE', NULL),
  (9,  NULL, 'EMP-1001',     'Dr. Meera Rao',    'meera.rao@college.edu',      '9820099999', 'Computer Engineering',     NULL, NULL, 'Staff Quarters, Block A', CURRENT_DATE - 900, 'ACTIVE', 5),
  (10, NULL, 'EMP-1002',     'Prof. Sanjay Gupta','sanjay.gupta@college.edu',  '9820010101', 'Mathematics',              NULL, NULL, 'Staff Quarters, Block B', CURRENT_DATE - 800, 'ACTIVE', 5);

-- ---------- Borrowings ----------
-- Helper columns (member code, copy code) are resolved to ids with sub-selects.
INSERT INTO borrowings (id, member_id, book_copy_id, issue_date, due_date, renewal_count, status, issued_by,
                        overdue_notice_sent, created_at)
SELECT v.id,
       (SELECT id FROM members WHERE member_code = v.member_code),
       (SELECT id FROM book_copies WHERE copy_code = v.copy_code),
       CURRENT_DATE + v.issue_offset, CURRENT_DATE + v.due_offset, v.renewals, v.status, 2,
       (v.status = 'BORROWED' AND v.due_offset < 0),
       (CURRENT_DATE + v.issue_offset)::TIMESTAMP + TIME '10:30'
FROM (VALUES
  -- currently borrowed, on time
  (1,  'STU-2026-001', 'LIB-CC-001',  -5,   9,  0, 'BORROWED'),
  (2,  'STU-2026-002', 'LIB-ITA-001', -12,  2,  0, 'BORROWED'),   -- due soon
  (3,  'STU-2026-003', 'LIB-AU-001',  -3,   11, 0, 'BORROWED'),   -- the only copy -> reservable
  (4,  'STU-2026-004', 'LIB-DP-001',  -8,   6,  0, 'BORROWED'),
  (5,  'EMP-1001',     'LIB-DP-002',  -2,   12, 0, 'BORROWED'),   -- both Design Patterns copies out
  -- currently borrowed, overdue
  (6,  'STU-2026-001', 'LIB-RF-001',  -20, -6,  0, 'BORROWED'),
  (7,  'STU-2026-005', 'LIB-CN-001',  -37, -16, 1, 'BORROWED'),
  (8,  'STU-2026-006', 'LIB-DSC-001', -25, -11, 0, 'BORROWED'),
  -- returned
  (9,  'STU-2026-001', 'LIB-PP-001',  -60, -46, 0, 'RETURNED'),
  (10, 'STU-2026-002', 'LIB-MOS-001', -50, -36, 0, 'RETURNED'),
  (11, 'STU-2026-007', 'LIB-CA-001',  -45, -31, 0, 'RETURNED'),
  (12, 'STU-2026-001', 'LIB-MD-001',  -40, -26, 0, 'RETURNED'),
  (13, 'STU-2026-008', 'LIB-EE-001',  -70, -56, 0, 'RETURNED'),
  (14, 'EMP-1002',     'LIB-ILA-001', -120,-106,0, 'RETURNED'),
  (15, 'STU-2026-003', 'LIB-CC-002',  -35, -21, 0, 'RETURNED'),
  (16, 'STU-2026-004', 'LIB-TCC-001', -150,-136,0, 'RETURNED'),
  (17, 'STU-2026-006', 'LIB-OSC-001', -95, -81, 0, 'RETURNED'),
  (18, 'STU-2026-005', 'LIB-GD-001',  -80, -66, 0, 'RETURNED'),
  (19, 'EMP-1001',     'LIB-PEAA-001',-100,-86, 0, 'RETURNED'),
  (20, 'STU-2026-002', 'LIB-CC-003',  -65, -51, 0, 'RETURNED')
) AS v(id, member_code, copy_code, issue_offset, due_offset, renewals, status);

-- Copies that are out right now must show BORROWED.
UPDATE book_copies SET status = 'BORROWED'
WHERE id IN (SELECT book_copy_id FROM borrowings WHERE status = 'BORROWED');

-- ---------- Returns ----------
INSERT INTO returns (borrowing_id, return_date, overdue_days, book_condition, received_by, created_at)
SELECT v.borrowing_id, CURRENT_DATE + v.return_offset,
       GREATEST(0, (CURRENT_DATE + v.return_offset) - b.due_date), 'GOOD', 2,
       (CURRENT_DATE + v.return_offset)::TIMESTAMP + TIME '16:00'
FROM (VALUES (9, -48), (10, -31), (11, -19), (12, -18), (13, -56), (14, -2), (15, -22),
             (16, -140), (17, -85), (18, -70), (19, -90), (20, -53)) AS v(borrowing_id, return_offset)
JOIN borrowings b ON b.id = v.borrowing_id;

-- ---------- Fines (₹5/day, capped at ₹500) ----------
--  #10: 5 days late  -> ₹25  (paid by UPI)
--  #11: 12 days late -> ₹60  (₹30 paid in cash)
--  #12: 8 days late  -> ₹40  (pending)
--  #14: 104 days late -> ₹520, capped to ₹500 (pending, blocks further issues)
INSERT INTO fines (id, borrowing_id, overdue_days, fine_per_day, amount, paid_amount, status, created_at) VALUES
  (1, 10, 5,   5.00, 25.00,  25.00, 'PAID',           (CURRENT_DATE - 31)::TIMESTAMP + TIME '16:00'),
  (2, 11, 12,  5.00, 60.00,  30.00, 'PARTIALLY_PAID', (CURRENT_DATE - 19)::TIMESTAMP + TIME '16:00'),
  (3, 12, 8,   5.00, 40.00,  0.00,  'PENDING',        (CURRENT_DATE - 18)::TIMESTAMP + TIME '16:00'),
  (4, 14, 104, 5.00, 500.00, 0.00,  'PENDING',        (CURRENT_DATE - 2)::TIMESTAMP + TIME '16:00');

INSERT INTO fine_payments (fine_id, amount, balance_after, payment_method, reference_number, payment_date,
                           payment_status, remarks, recorded_by) VALUES
  (1, 25.00, 0.00,  'UPI',  'UPI-DEMO-482193', (CURRENT_DATE - 31)::TIMESTAMP + TIME '16:05', 'PAID',    'Paid at counter', 2),
  (2, 30.00, 30.00, 'CASH', NULL,              (CURRENT_DATE - 19)::TIMESTAMP + TIME '16:10', 'PARTIAL', 'Balance to be paid next visit', 2);

-- ---------- Reservations (FIFO queue on books with no free copy) ----------
INSERT INTO reservations (member_id, book_id, reserved_at, status) VALUES
  (2, 5,  (CURRENT_DATE - 2)::TIMESTAMP + TIME '11:00', 'WAITING'),   -- Algorithms Unlocked, 1st in queue
  (7, 5,  (CURRENT_DATE - 1)::TIMESTAMP + TIME '09:15', 'WAITING'),   -- Algorithms Unlocked, 2nd in queue
  (1, 15, (CURRENT_DATE - 1)::TIMESTAMP + TIME '14:40', 'WAITING');   -- Design Patterns

-- ---------- Notifications for the demo student ----------
INSERT INTO notifications (member_id, type, title, message, is_read, created_at) VALUES
  (1, 'BOOK_ISSUED',    'Book issued',      '"Clean Code" (LIB-CC-001) was issued to you. Due on ' || TO_CHAR(CURRENT_DATE + 9, 'DD Mon YYYY') || '.', FALSE, (CURRENT_DATE - 5)::TIMESTAMP + TIME '10:31'),
  (1, 'OVERDUE',        'Book overdue',     '"Refactoring" was due on ' || TO_CHAR(CURRENT_DATE - 6, 'DD Mon YYYY') || '. A fine of ₹5 per day is adding up.', FALSE, (CURRENT_DATE - 5)::TIMESTAMP + TIME '08:00'),
  (1, 'BOOK_RETURNED',  'Book returned',    '"Malgudi Days" was returned 8 days late.', TRUE, (CURRENT_DATE - 18)::TIMESTAMP + TIME '16:00'),
  (1, 'FINE_GENERATED', 'Fine generated',   'A fine of ₹40.00 was added for "Malgudi Days".', TRUE, (CURRENT_DATE - 18)::TIMESTAMP + TIME '16:00');

-- ---------- A few audit entries ----------
INSERT INTO audit_logs (user_id, username, action, entity_type, entity_id, details, created_at) VALUES
  (1, 'admin',     'UPDATE_SETTINGS', 'LibrarySettings', 1,  'Initial library settings configured', NOW() - INTERVAL '30 days'),
  (2, 'librarian', 'ISSUE_BOOK',      'Borrowing',       1,  'Issued LIB-CC-001 to STU-2026-001', (CURRENT_DATE - 5)::TIMESTAMP + TIME '10:30'),
  (2, 'librarian', 'RETURN_BOOK',     'Borrowing',       14, 'Returned LIB-ILA-001, 104 days late, fine ₹500.00', (CURRENT_DATE - 2)::TIMESTAMP + TIME '16:00');

-- ---------- Move sequences past the explicit ids used above ----------
SELECT setval(pg_get_serial_sequence('users', 'id'),        (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('categories', 'id'),   (SELECT MAX(id) FROM categories));
SELECT setval(pg_get_serial_sequence('authors', 'id'),      (SELECT MAX(id) FROM authors));
SELECT setval(pg_get_serial_sequence('publishers', 'id'),   (SELECT MAX(id) FROM publishers));
SELECT setval(pg_get_serial_sequence('books', 'id'),        (SELECT MAX(id) FROM books));
SELECT setval(pg_get_serial_sequence('members', 'id'),      (SELECT MAX(id) FROM members));
SELECT setval(pg_get_serial_sequence('borrowings', 'id'),   (SELECT MAX(id) FROM borrowings));
SELECT setval(pg_get_serial_sequence('fines', 'id'),        (SELECT MAX(id) FROM fines));

COMMIT;
