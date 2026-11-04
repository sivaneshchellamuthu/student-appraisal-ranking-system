package store;

import model.Mark;
import model.Student;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class RankingService {

    private final DataStore store;

    public RankingService(DataStore store) {
        this.store = store;
    }

    public record RankRow(int rank, String regNo, String name, int subjects, double total, double average) {
    }

    public record SubjectRank(String subject, double mark, int rank, int total) {
    }

    public record StudentReport(Student student, List<SubjectRank> subjectRanks, double average,
                                int overallRank, int classSize) {
    }

    public record SubjectStat(String subject, double average, int students) {
    }

    public List<RankRow> overallRankList() {
        List<RankRow> raw = new ArrayList<>();
        for (Student student : store.getStudents()) {
            List<Mark> studentMarks = store.marksFor(student.getRegNo());
            if (studentMarks.isEmpty()) {
                continue;
            }
            double total = 0;
            for (Mark mark : studentMarks) {
                total += mark.getValue();
            }
            raw.add(new RankRow(0, student.getRegNo(), student.getName(),
                    studentMarks.size(), round2(total), round2(total / studentMarks.size())));
        }

        raw.sort(Comparator.comparingDouble(RankRow::average).reversed()
                .thenComparing(RankRow::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(RankRow::regNo, String.CASE_INSENSITIVE_ORDER));

        List<RankRow> ranked = new ArrayList<>(raw.size());
        int rank = 0;
        double previous = Double.NaN;
        for (int i = 0; i < raw.size(); i++) {
            RankRow row = raw.get(i);
            if (i == 0 || Math.abs(row.average() - previous) > 1e-9) {
                rank = i + 1;
            }
            previous = row.average();
            ranked.add(new RankRow(rank, row.regNo(), row.name(), row.subjects(),
                    row.total(), row.average()));
        }
        return ranked;
    }

    public StudentReport report(String regNo) {
        Student student = store.findStudent(regNo);
        if (student == null) {
            return null;
        }

        List<Mark> studentMarks = store.marksFor(student.getRegNo());
        List<SubjectRank> subjectRanks = new ArrayList<>();
        double total = 0;
        for (Mark mark : studentMarks) {
            subjectRanks.add(subjectRank(mark.getSubject(), student.getRegNo()));
            total += mark.getValue();
        }
        subjectRanks.sort(Comparator.comparing(SubjectRank::subject, String.CASE_INSENSITIVE_ORDER));

        double average = studentMarks.isEmpty() ? 0 : total / studentMarks.size();
        List<RankRow> overall = overallRankList();
        int rank = 0;
        for (RankRow row : overall) {
            if (row.regNo().equalsIgnoreCase(student.getRegNo())) {
                rank = row.rank();
                break;
            }
        }
        return new StudentReport(student, subjectRanks, round2(average), rank, overall.size());
    }

    public List<SubjectStat> subjectStats() {
        Map<String, List<Double>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Mark mark : store.getMarks()) {
            grouped.computeIfAbsent(mark.getSubject(), key -> new ArrayList<>()).add(mark.getValue());
        }
        List<SubjectStat> stats = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : grouped.entrySet()) {
            double total = 0;
            for (double value : entry.getValue()) {
                total += value;
            }
            stats.add(new SubjectStat(entry.getKey(), round2(total / entry.getValue().size()),
                    entry.getValue().size()));
        }
        return stats;
    }

    private SubjectRank subjectRank(String subject, String regNo) {
        List<Mark> subjectMarks = new ArrayList<>();
        for (Mark mark : store.getMarks()) {
            if (mark.getSubject().equalsIgnoreCase(subject)) {
                subjectMarks.add(mark);
            }
        }
        subjectMarks.sort(Comparator.comparingDouble(Mark::getValue).reversed()
                .thenComparing(Mark::getRegNo, String.CASE_INSENSITIVE_ORDER));

        int rank = 0;
        double previous = Double.NaN;
        for (int i = 0; i < subjectMarks.size(); i++) {
            Mark mark = subjectMarks.get(i);
            if (i == 0 || Math.abs(mark.getValue() - previous) > 1e-9) {
                rank = i + 1;
            }
            if (mark.getRegNo().equalsIgnoreCase(regNo)) {
                return new SubjectRank(subject, mark.getValue(), rank, subjectMarks.size());
            }
            previous = mark.getValue();
        }
        return new SubjectRank(subject, 0, 0, subjectMarks.size());
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static String formatMark(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.US, "%.2f", value);
    }
}
