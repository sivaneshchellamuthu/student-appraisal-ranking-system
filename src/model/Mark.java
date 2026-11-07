package model;

/**
 * One score obtained by one student in one subject.
 * The database enforces UNIQUE(student_id, subject_id), so a mark row is
 * updated rather than duplicated when the same subject is re-entered.
 */
public class Mark {

    private int markId;
    private int studentId;
    private int subjectId;
    private String studentRollNo;
    private String studentName;
    private String subjectCode;
    private String subjectName;
    private double marksObtained;
    private double maxMarks;
    private int enteredBy;

    public Mark() {
        this.maxMarks = 100.0;
    }

    public Mark(int markId, int studentId, int subjectId, double marksObtained,
                double maxMarks, String subjectCode, String subjectName) {
        this.markId = markId;
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.marksObtained = marksObtained;
        this.maxMarks = maxMarks;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
    }

    public int getMarkId() {
        return markId;
    }

    public void setMarkId(int markId) {
        this.markId = markId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getStudentRollNo() {
        return studentRollNo;
    }

    public void setStudentRollNo(String studentRollNo) {
        this.studentRollNo = studentRollNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public double getMarksObtained() {
        return marksObtained;
    }

    public void setMarksObtained(double marksObtained) {
        this.marksObtained = marksObtained;
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(double maxMarks) {
        this.maxMarks = maxMarks;
    }

    public int getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(int enteredBy) {
        this.enteredBy = enteredBy;
    }

    public double getPercentage() {
        if (maxMarks <= 0.0) {
            return 0.0;
        }
        return (marksObtained / maxMarks) * 100.0;
    }

    public String getGrade() {
        return GradeCalculator.gradeFor(getPercentage());
    }

    @Override
    public String toString() {
        return subjectCode + ": " + marksObtained + "/" + maxMarks;
    }
}
