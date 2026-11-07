-- ============================================================================
-- Student Appraisal and Ranking System (CGB1201)
-- MySQL schema, constraints and sample data
--
-- Run with:
--   mysql -u root -p < schema.sql
--
-- Default accounts:
--   admin   / admin123    (ADMIN)
--   faculty / faculty123  (FACULTY)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS student_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE student_db;

-- Child table first so foreign keys do not block the drop.
DROP TABLE IF EXISTS marks;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS users;

-- ----------------------------------------------------------------------------
-- USERS
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash CHAR(64)     NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    user_role     ENUM('ADMIN', 'FACULTY') NOT NULL DEFAULT 'FACULTY',
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- STUDENTS
-- ----------------------------------------------------------------------------
CREATE TABLE students (
    student_id  INT AUTO_INCREMENT PRIMARY KEY,
    roll_no     VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    department  VARCHAR(50)  NOT NULL,
    study_year  INT          NOT NULL,
    section     VARCHAR(5)   NOT NULL,
    attendance  DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    email       VARCHAR(100) NULL,
    phone       VARCHAR(15)  NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_student_year
        CHECK (study_year BETWEEN 1 AND 4),
    CONSTRAINT chk_student_attendance
        CHECK (attendance BETWEEN 0 AND 100)
) ENGINE = InnoDB;

CREATE INDEX idx_students_department_section
    ON students (department, section);

-- ----------------------------------------------------------------------------
-- SUBJECTS
-- ----------------------------------------------------------------------------
CREATE TABLE subjects (
    subject_id   INT AUTO_INCREMENT PRIMARY KEY,
    subject_code VARCHAR(20)  NOT NULL UNIQUE,
    subject_name VARCHAR(100) NOT NULL,
    department   VARCHAR(50)  NOT NULL,
    study_year   INT          NOT NULL,
    max_marks    INT          NOT NULL DEFAULT 100,
    CONSTRAINT chk_subject_year
        CHECK (study_year BETWEEN 1 AND 4),
    CONSTRAINT chk_subject_max_marks
        CHECK (max_marks BETWEEN 1 AND 100)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- MARKS
-- One row per student per subject (enforced by uq_marks_student_subject).
-- ----------------------------------------------------------------------------
CREATE TABLE marks (
    mark_id        INT AUTO_INCREMENT PRIMARY KEY,
    student_id     INT          NOT NULL,
    subject_id     INT          NOT NULL,
    marks_obtained DECIMAL(5, 2) NOT NULL,
    entered_by     INT          NULL,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_marks_student
        FOREIGN KEY (student_id) REFERENCES students (student_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_marks_subject
        FOREIGN KEY (subject_id) REFERENCES subjects (subject_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_marks_user
        FOREIGN KEY (entered_by) REFERENCES users (user_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT uq_marks_student_subject
        UNIQUE (student_id, subject_id),
    CONSTRAINT chk_marks_range
        CHECK (marks_obtained BETWEEN 0 AND 100)
) ENGINE = InnoDB;

-- ============================================================================
-- SEED DATA
-- SHA-256 hashes:
--   admin123   = 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
--   faculty123 = 27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a
-- ============================================================================
INSERT INTO users (user_id, username, password_hash, full_name, user_role, active)
VALUES
    (1, 'admin',
     '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
     'System Administrator', 'ADMIN', 1),
    (2, 'faculty',
     '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a',
     'Course Faculty', 'FACULTY', 1);

INSERT INTO students
    (student_id, roll_no, name, department, study_year, section,
     attendance, email, phone)
VALUES
    (1, 'CSE2023001', 'Aarav Sharma', 'Computer Science', 3, 'A',
     92.50, 'aarav.sharma@example.com', '9876543210'),
    (2, 'CSE2023002', 'Diya Patel', 'Computer Science', 3, 'A',
     88.00, 'diya.patel@example.com', '9876543211'),
    (3, 'CSE2023003', 'Rohan Verma', 'Computer Science', 3, 'B',
     76.50, 'rohan.verma@example.com', '9876543212'),
    (4, 'ECE2023001', 'Ananya Iyer', 'Electronics', 2, 'A',
     95.00, 'ananya.iyer@example.com', '9876543213'),
    (5, 'MEC2023001', 'Kabir Singh', 'Mechanical', 1, 'A',
     70.00, 'kabir.singh@example.com', '9876543214');

INSERT INTO subjects
    (subject_id, subject_code, subject_name, department, study_year, max_marks)
VALUES
    (1, 'CS301', 'Data Structures', 'Computer Science', 3, 100),
    (2, 'CS302', 'Database Systems', 'Computer Science', 3, 100),
    (3, 'CS303', 'Operating Systems', 'Computer Science', 3, 100),
    (4, 'EC201', 'Digital Electronics', 'Electronics', 2, 100),
    (5, 'ME101', 'Engineering Mechanics', 'Mechanical', 1, 100);

-- The two 90% students demonstrate the shared-rank rule:
-- both are rank 2; attendance (92.50 vs 88.00) decides display order.
INSERT INTO marks
    (student_id, subject_id, marks_obtained, entered_by)
VALUES
    (1, 1, 90.00, 1),
    (1, 2, 90.00, 1),
    (1, 3, 90.00, 1),
    (2, 1, 92.00, 1),
    (2, 2, 88.00, 1),
    (2, 3, 90.00, 1),
    (3, 1, 72.00, 1),
    (3, 2, 68.00, 1),
    (3, 3, 75.00, 1),
    (4, 4, 95.00, 1),
    (5, 5, 74.00, 1);

-- ============================================================================
-- QUICK VERIFICATION
-- ============================================================================
SELECT 'student_db created with sample data' AS message;
SELECT username, full_name, user_role FROM users ORDER BY user_id;
SELECT roll_no, name, department, study_year, section FROM students ORDER BY roll_no;
