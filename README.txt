==============================================================================
 STUDENT APPRAISAL AND RANKING SYSTEM
 Course: CGB1201 - Java Programming
 Stack : Java 17+, Swing/AWT, JDBC, MySQL 8, MySQL Connector/J
==============================================================================

1. WHAT THE PROJECT DOES
------------------------------------------------------------------------------
- Secure login with SHA-256 password hashing and ADMIN / FACULTY roles.
- Student management: add, update, delete, search and view students.
- Subject management and marks entry per student per subject.
- Appraisal: total, percentage, grade, pass/fail and performance.
- Ranking by percentage with attendance/total tie-breaks; equal percentages
  share the same rank. Department and section filters are supported.
- Reports: individual report card, class rank list, topper list.
- CSV export and a print-friendly output view.
- Admin-only user management (create, edit, delete, assign roles).
- Faculty accounts never see the Users tab; UserService also rejects non-admin calls.

2. REQUIREMENTS
------------------------------------------------------------------------------
- JDK 17 or newer (the project was compiled and tested with JDK 17).
- MySQL 8.x server running on localhost:3306.
- MySQL Connector/J jar (for example mysql-connector-j-9.x.x.jar).
  Download: https://dev.mysql.com/downloads/connector/j/
  Copy/rename the jar to:
      lib\mysql-connector-j.jar
- No Maven, Gradle or Spring is required.

3. PROJECT FOLDER STRUCTURE
------------------------------------------------------------------------------
StudentAppraisalSystem\
 |-- src\
 |    |-- Main.java                  Application entry point
 |    |-- model\                     Person, Gradable, Student, User, Subject,
 |    |                              Mark, GradeResult, RankEntry, GradeCalculator
 |    |-- dao\                       DataStore.java (all SQL and JDBC)
 |    |-- service\                   Auth, Student, Subject, Marks, Appraisal,
 |    |                              Ranking, Report and User services
 |    |-- exception\                 AppraisalException (custom exception)
 |    |-- ui\                        LoginFrame, MainFrame, Dashboard, Student,
 |    |                              Marks, Ranking, Report, Users, UiUtils
 |-- lib\                           mysql-connector-j.jar goes here
 |-- bin\                           compiled .class files
 |-- schema.sql                     database, tables, constraints and seed data
 |-- migrate_teacher_to_faculty.sql  optional migration for an existing DB
 |-- compile.bat                    one-click Windows compile
 |-- run.bat                        one-click Windows run
 |-- README.txt                     this file

4. DATABASE SETUP
------------------------------------------------------------------------------
A. Open DataStore.java and check the three constants at the top:
       private static final String DB_URL = "... student_db ...";
       private static final String DB_USER = "root";
       private static final String DB_PASSWORD = "root";
   Change DB_USER / DB_PASSWORD to match your MySQL account.

B. Create the database and load the sample data. Open Command Prompt in the
   project folder and run:

       mysql -u root -p < schema.sql

   If mysql.exe is not on PATH, use its full path, for example:
       "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < schema.sql

   WARNING: schema.sql drops the four project tables before recreating them.
   Running it again resets the sample data.

   If you already created the database with the older TEACHER role and want
   to keep your existing data, run the migration instead:

       mysql -u root -p < migrate_teacher_to_faculty.sql

C. Default accounts created by schema.sql:
       ADMIN   : admin   / admin123
       FACULTY : faculty / faculty123

   Both passwords are stored only as SHA-256 hashes.

5. COMPILE AND RUN ON WINDOWS
------------------------------------------------------------------------------
Option 1 - use the batch files (recommended):
       compile.bat
       run.bat

Option 2 - exact manual commands in Command Prompt (cmd.exe):

       if not exist bin mkdir bin
       javac -encoding UTF-8 -cp ".;lib\mysql-connector-j.jar" -d bin src\Main.java src\model\*.java src\dao\*.java src\service\*.java src\exception\*.java src\ui\*.java
       java -cp ".;bin;lib\mysql-connector-j.jar" Main

   Explanation:
   - The compile command lists every source package; javac expands the
     package wildcards and also handles folder names containing spaces.
   - The runtime classpath contains the current folder, bin and the JDBC jar.
   - compile.bat uses an @sources.txt response file internally, which is an
     equivalent alternative if you prefer a one-command build.

   If your jar has a different filename, either rename it to
   mysql-connector-j.jar or update compile.bat and run.bat.

6. ROLE-BASED ACCESS
------------------------------------------------------------------------------
- ADMIN: all tabs, including "Users"; can create/edit/delete accounts.
- FACULTY: Dashboard, Students, Marks, Ranking and Reports only.
- The User Management tab is not created for FACULTY accounts.
- UserService checks the role again for every user-management operation, so
  the restriction is not only a UI setting.
- Disabled accounts cannot log in.

7. BUSINESS RULES
------------------------------------------------------------------------------
Grade scale:
       O  >= 90
       A+ >= 80
       A  >= 70
       B+ >= 60
       B  >= 50
       C  >= 40
       F  <  40

Pass rule:
       Overall percentage >= 40 AND no individual subject mark < 40.

Ranking order:
       1. Higher percentage first.
       2. If percentages are equal, higher total marks first.
       3. If totals are equal, higher attendance first.
       4. Equal percentages share the same rank (standard competition ranking).
       Example from the seed data: Ananya is rank 1 (95%). Aarav and Diya both
       have 90% and both are rank 2; attendance orders their rows. The next
       student is rank 4.

8. HOW TO TEST EACH MODULE
------------------------------------------------------------------------------
MODULE 1 - MODEL / OOP
- Compile successfully. Open the source files and review the comments marked
  "OOP CONCEPT": abstraction (Person), inheritance (Student/User), interface
  and polymorphism (Gradable, getDetails), encapsulation and the custom
  AppraisalException.
- Log in and confirm the Dashboard shows grade rules.

MODULE 2 - DATABASE / DataStore
- Run schema.sql, then start the app. The login screen should say
  "Database connected (student_db)".
- Stop MySQL and restart the login screen: it should show
  "Database not reachable" and a clear error on login.
- Verify only DataStore.java contains SQL statements.

MODULE 3 - LOGIN / AUTHENTICATION
- Log in as admin / admin123 -> Users tab is visible.
- Log out and log in as faculty / faculty123 -> no Users tab.
- Try a wrong password -> "Invalid username or password."
- In MySQL, set a user's active = 0, then try to log in -> account disabled.

MODULE 4 - STUDENT MANAGEMENT
- Open Students -> Add Student. Save a new roll number and confirm it appears.
- Try adding the same roll number again -> duplicate roll error.
- Edit the student, change attendance and save.
- Search by part of a name, roll number, department or section.
- Delete a student -> marks are removed by the database cascade.
- Enter attendance 120 -> validation error.

MODULE 5 - SUBJECTS AND MARKS
- Open Marks -> Subjects -> Add Subject (for example CS304, 100 max marks).
- Open Enter Marks, choose a student and subject, enter 85 and save.
- Re-save the same student/subject with 90 -> the mark is updated, not duplicated.
- Enter -5 or 105 -> "Marks must be between 0 and 100."
- Select a row in the marks table -> subject and mark are filled for editing.
- Delete a mark and confirm it disappears.

MODULE 6 - APPRAISAL / RANKING
- Open Ranking. The seed data should show:
      Rank 1  Ananya Iyer   95.00%
      Rank 2  Aarav Sharma  90.00%
      Rank 2  Diya Patel    90.00%   (attendance decides row order)
      Rank 4  Kabir Singh   74.00%
      Rank 5  Rohan Verma   71.67%
- Filter Department = Computer Science and Section = A; only CSE/A students show.
- Export CSV and open it in Excel or a text editor.
- Click Print and choose a printer or "Microsoft Print to PDF".

MODULE 7 - REPORTS
- Reports -> select a student -> Individual Report Card.
- Click Class Rank List and Topper List (Top 10).
- Export each report to CSV.
- Click Print. The monospaced text area is the print-friendly view.
- A student with no marks receives 0%, grade F and FAIL with a clear note.

MODULE 8 - USER MANAGEMENT (ADMIN ONLY)
- Log in as admin -> Users -> Add User (for example faculty2 / pass123,
  role FACULTY).
- Log out and log in as faculty2: no Users tab.
- As admin, edit the user, change the role or reset the password.
- Try to delete your own account -> blocked.
- Try to delete/disable the last ADMIN -> blocked.
- Passwords are stored as 64-character SHA-256 hex strings in the users table.

9. OOP CONCEPT MAP
------------------------------------------------------------------------------
- Classes and objects: every entity and service is a class; objects are
  created in the UI and service layers.
- Encapsulation: private fields with public getters/setters in all models.
- Inheritance: abstract Person -> Student and User.
- Polymorphism:
      * getDetails() is overridden by Student and User.
      * Gradable is implemented by Student and GradeResult.
      * AppraisalService.describe(Gradable) works with either implementation.
- Custom exception: exception.AppraisalException wraps validation and DB errors.
- Layering: ui -> service -> dao -> MySQL; no SQL outside DataStore.java.

10. TROUBLESHOOTING
------------------------------------------------------------------------------
- "MySQL Connector/J was not found" / "No suitable driver":
  Put the jar in lib\mysql-connector-j.jar and use the exact classpath shown.
- "Access denied for user":
  Fix DB_USER / DB_PASSWORD at the top of DataStore.java.
- "Unknown database 'student_db'":
  Run: mysql -u root -p < schema.sql
- "Communications link failure":
  Start the MySQL service and confirm it listens on localhost:3306.
- Public key retrieval / SSL errors:
  The URL already includes useSSL=false and allowPublicKeyRetrieval=true.
- CHECK constraints are enforced by MySQL 8.0.16+. The Java services also
  validate all input, so older servers still behave correctly.
- If your schema run reports a foreign key error, run schema.sql as a whole;
  it drops the child `marks` table before the parent tables.

==============================================================================
 End of README
==============================================================================
