package service;

import dao.DataStore;
import exception.AppraisalException;
import model.GradeResult;
import model.RankEntry;
import model.Student;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ranking logic.
 *
 * Ordering rules:
 *   1. Higher percentage first.
 *   2. If percentages are equal, higher total marks first (tie-breaker).
 *   3. If totals are equal, higher attendance first (tie-breaker).
 *   4. If all are equal, roll number is used only to keep a stable display order.
 *
 * Students with the same percentage share the same rank. For example, in the
 * seed data Aarav and Diya both have 90%, so both are rank 2 and the next
 * student is rank 4 (standard competition ranking).
 */
public class RankingService {

    private final DataStore dataStore;
    private final AppraisalService appraisalService;

    public RankingService() {
        this(new DataStore(), new AppraisalService());
    }

    public RankingService(DataStore dataStore, AppraisalService appraisalService) {
        this.dataStore = dataStore;
        this.appraisalService = appraisalService;
    }

    public List<RankEntry> generateRanking(String department, String section)
            throws AppraisalException {
        List<Student> students;
        try {
            students = dataStore.getStudentsByFilter(department, section);
        } catch (SQLException ex) {
            throw new AppraisalException(
                    "Database error while loading students for ranking: "
                    + ex.getMessage(), ex);
        }

        List<RankEntry> entries = new ArrayList<>();
        for (Student student : students) {
            GradeResult result = appraisalService.appraise(student);
            entries.add(new RankEntry(student, result.getTotal(),
                    result.getPercentage(), student.getAttendance(),
                    result.getGrade(), result.isPassed()));
        }

        entries.sort((first, second) -> {
            int compare = Double.compare(second.getPercentage(), first.getPercentage());
            if (compare != 0) {
                return compare;
            }
            compare = Double.compare(second.getTotal(), first.getTotal());
            if (compare != 0) {
                return compare;
            }
            compare = Double.compare(second.getAttendance(), first.getAttendance());
            if (compare != 0) {
                return compare;
            }
            return first.getRollNo().compareToIgnoreCase(second.getRollNo());
        });

        assignRanks(entries);
        return entries;
    }

    public List<RankEntry> getToppers(int limit, String department, String section)
            throws AppraisalException {
        if (limit <= 0) {
            return new ArrayList<>();
        }
        List<RankEntry> ranking = generateRanking(department, section);
        int end = Math.min(limit, ranking.size());
        return new ArrayList<>(ranking.subList(0, end));
    }

    private void assignRanks(List<RankEntry> entries) {
        int rank = 0;
        double previousPercentage = -1.0;
        for (int index = 0; index < entries.size(); index++) {
            double percentage = entries.get(index).getPercentage();
            if (index == 0
                    || Math.abs(percentage - previousPercentage) > 0.0001) {
                rank = index + 1;
            }
            entries.get(index).setRank(rank);
            previousPercentage = percentage;
        }
    }
}
