-- ============================================================================
-- Migration: rename the TEACHER role to FACULTY
--
-- Use this only if you already created student_db with the older schema and
-- want to keep your existing data. A fresh run of schema.sql already uses
-- FACULTY, so no migration is needed for a new database.
--
-- Run:
--   mysql -u root -p < migrate_teacher_to_faculty.sql
-- ============================================================================

USE student_db;

-- 1. Convert existing role values before narrowing the ENUM definition.
UPDATE users
SET user_role = 'FACULTY'
WHERE user_role = 'TEACHER';

-- 2. Replace the allowed role values.
ALTER TABLE users
    MODIFY user_role ENUM('ADMIN', 'FACULTY') NOT NULL DEFAULT 'FACULTY';

-- 3. Rename the sample account teacher/teacher123 to faculty/faculty123.
--    If you already have a different account named 'faculty', skip these two
--    statements (or rename/remove that account first).
UPDATE users
SET full_name = 'Course Faculty'
WHERE username = 'teacher';

UPDATE users
SET username = 'faculty',
    password_hash = '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a'
WHERE username = 'teacher';

SELECT username, full_name, user_role
FROM users
ORDER BY user_id;
