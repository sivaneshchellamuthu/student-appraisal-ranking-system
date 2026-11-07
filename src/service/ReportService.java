package service;

import exception.AppraisalException;
import model.GradeResult;
import model.Mark;
import model.RankEntry;
import model.Student;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds report-card, rank-list and topper-list output as plain text (for the
 * print-friendly view) or as CSV files.
 *
 * This class contains no SQL. It uses the student, appraisal and ranking
 * services, keeping the four layers clean.
 */
public class ReportService {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StudentService studentService = new StudentService();
    private final AppraisalService appraisalService = new AppraisalService();
    private final RankingService rankingService = new RankingService();

    /** Simple value object pairing a student with the calculated result. */
    public static class ReportCard {
        private final Student student;
        private final GradeResult result;

        public ReportCard(Student student, GradeResult result) {
            this.student = student;
            this.result = result;
        }

        public Student getStudent() {
            return student;
        }

        public GradeResult getResult() {
            return result;
        }
    }

    public ReportCard getReportCard(int studentId) throws AppraisalException {
        Student student = studentService.getStudent(studentId);
        GradeResult result = appraisalService.appraise(student);
        return new ReportCard(student, result);
    }

    public List<RankEntry> getClassRankList(String department, String section)
            throws AppraisalException {
        return rankingService.generateRanking(department, section);
    }

    public List<RankEntry> getTopperList(int limit, String department,
                                         String section) throws AppraisalException {
        return rankingService.getToppers(limit, department, section);
    }

    // ==================================================================
    // PRINT-FRIENDLY TEXT
    // ==================================================================

    public String buildReportCardText(ReportCard reportCard) {
        Student student = reportCard.getStudent();
        GradeResult result = reportCard.getResult();

        StringBuilder text = new StringBuilder();
        text.append("==============================================================================\n");
        text.append("                 STUDENT APPRAISAL AND RANKING SYSTEM\n");
        text.append("                        INDIVIDUAL REPORT CARD\n");
        text.append("==============================================================================\n");
        text.append(String.format("Roll No        : %s%n", student.getRollNo()));
        text.append(String.format("Student Name   : %s%n", student.getName()));
        text.append(String.format("Department     : %s%n", student.getDepartment()));
        text.append(String.format("Year / Section : %d / %s%n",
                student.getYear(), student.getSection()));
        text.append(String.format("Attendance     : %.2f%%%n", student.getAttendance()));
        text.append("------------------------------------------------------------------------------\n");
        text.append(String.format("%-14s %-32s %10s %10s %6s%n",
                "Subject Code", "Subject Name", "Max", "Obtained", "Grade"));
        text.append("------------------------------------------------------------------------------\n");

        if (result.getMarks().isEmpty()) {
            text.append("No marks have been entered for this student yet.\n");
        } else {
            for (Mark mark : result.getMarks()) {
                text.append(String.format("%-14s %-32s %10.2f %10.2f %6s%n",
                        mark.getSubjectCode(), mark.getSubjectName(),
                        mark.getMaxMarks(), mark.getMarksObtained(),
                        mark.getGrade()));
            }
        }

        text.append("------------------------------------------------------------------------------\n");
        text.append(String.format("Total Marks    : %.2f / %.2f%n",
                result.getTotal(), result.getMaxTotal()));
        text.append(String.format("Percentage     : %.2f%%%n", result.getPercentage()));
        text.append(String.format("Grade          : %s%n", result.getGrade()));
        text.append(String.format("Performance    : %s%n", result.getPerformance()));
        text.append(String.format("Result         : %s%n", result.getResultText()));
        text.append("==============================================================================\n");
        text.append("Generated on   : ")
                .append(LocalDateTime.now().format(STAMP)).append('\n');
        return text.toString();
    }

    public String buildRankListText(String title, String department,
                                    String section, List<RankEntry> entries) {
        StringBuilder text = new StringBuilder();
        text.append("==============================================================================\n");
        text.append(center(title, 78)).append('\n');
        text.append("==============================================================================\n");
        text.append("Filter: Department = ").append(displayFilter(department))
                .append(" | Section = ").append(displayFilter(section)).append('\n');
        text.append("------------------------------------------------------------------------------\n");
        text.append(String.format("%-5s %-12s %-22s %-16s %-4s %-4s %8s %8s %8s %-5s %s%n",
                "Rank", "Roll No", "Name", "Department", "Year", "Sec",
                "Total", "Percent", "Attend", "Grade", "Result"));
        text.append("------------------------------------------------------------------------------\n");

        if (entries.isEmpty()) {
            text.append("No students matched the selected filters.\n");
        } else {
            for (RankEntry entry : entries) {
                text.append(String.format(
                        "%-5d %-12s %-22s %-16s %-4d %-4s %8.2f %7.2f%% %7.2f%% %-5s %s%n",
                        entry.getRank(), entry.getRollNo(), entry.getStudentName(),
                        entry.getDepartment(), entry.getYear(), entry.getSection(),
                        entry.getTotal(), entry.getPercentage(), entry.getAttendance(),
                        entry.getGrade(), entry.getResultText()));
            }
        }

        text.append("------------------------------------------------------------------------------\n");
        text.append("Total students listed: ").append(entries.size()).append('\n');
        text.append("Generated on         : ")
                .append(LocalDateTime.now().format(STAMP)).append('\n');
        return text.toString();
    }

    // ==================================================================
    // CSV EXPORT
    // ==================================================================

    public void exportReportCardCsv(File file, ReportCard reportCard)
            throws AppraisalException {
        Student student = reportCard.getStudent();
        GradeResult result = reportCard.getResult();

        try (BufferedWriter writer = Files.newBufferedWriter(
                file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Roll No,Name,Department,Year,Section,"
                    + "Subject Code,Subject Name,Max Marks,Marks Obtained,Subject Grade");
            writer.newLine();

            for (Mark mark : result.getMarks()) {
                writer.write(String.join(",",
                        csv(student.getRollNo()), csv(student.getName()),
                        csv(student.getDepartment()),
                        String.valueOf(student.getYear()),
                        csv(student.getSection()),
                        csv(mark.getSubjectCode()), csv(mark.getSubjectName()),
                        String.valueOf(mark.getMaxMarks()),
                        String.valueOf(mark.getMarksObtained()),
                        csv(mark.getGrade())));
                writer.newLine();
            }

            writer.newLine();
            writer.write("Summary,Total Marks,Max Total,Percentage,Grade,Result,"
                    + "Performance,Attendance");
            writer.newLine();
            writer.write(String.join(",",
                    "Overall",
                    String.valueOf(result.getTotal()),
                    String.valueOf(result.getMaxTotal()),
                    String.format("%.2f", result.getPercentage()),
                    csv(result.getGrade()), csv(result.getResultText()),
                    csv(result.getPerformance()),
                    String.format("%.2f", student.getAttendance())));
            writer.newLine();
        } catch (IOException ex) {
            throw new AppraisalException("Could not export the report card: "
                    + ex.getMessage(), ex);
        }
    }

    public void exportRankListCsv(File file, List<RankEntry> entries)
            throws AppraisalException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Rank,Roll No,Name,Department,Year,Section,Total,"
                    + "Percentage,Attendance,Grade,Result");
            writer.newLine();
            for (RankEntry entry : entries) {
                writer.write(String.join(",",
                        String.valueOf(entry.getRank()),
                        csv(entry.getRollNo()), csv(entry.getStudentName()),
                        csv(entry.getDepartment()),
                        String.valueOf(entry.getYear()),
                        csv(entry.getSection()),
                        String.format("%.2f", entry.getTotal()),
                        String.format("%.2f", entry.getPercentage()),
                        String.format("%.2f", entry.getAttendance()),
                        csv(entry.getGrade()), csv(entry.getResultText())));
                writer.newLine();
            }
        } catch (IOException ex) {
            throw new AppraisalException("Could not export the rank list: "
                    + ex.getMessage(), ex);
        }
    }

    // ==================================================================
    // HELPERS
    // ==================================================================

    private static String displayFilter(String value) {
        return value == null || value.trim().isEmpty()
                || "All".equalsIgnoreCase(value.trim()) ? "ALL" : value.trim();
    }

    private static String center(String value, int width) {
        if (value.length() >= width) {
            return value;
        }
        int left = (width - value.length()) / 2;
        return " ".repeat(left) + value;
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String text = value;
        if (text.contains(",") || text.contains("\"")
                || text.contains("\n") || text.contains("\r")) {
            text = "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
