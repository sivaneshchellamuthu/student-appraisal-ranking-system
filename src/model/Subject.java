package model;

/**
 * Subject entity. A subject belongs to a department and a year of study and
 * defines the maximum marks that can be awarded.
 */
public class Subject {

    private int subjectId;
    private String subjectCode;
    private String subjectName;
    private String department;
    private int year;
    private int maxMarks;

    public Subject() {
        this.maxMarks = 100;
    }

    public Subject(int subjectId, String subjectCode, String subjectName,
                   String department, int year, int maxMarks) {
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.department = department;
        this.year = year;
        this.maxMarks = maxMarks;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(int maxMarks) {
        this.maxMarks = maxMarks;
    }

    @Override
    public String toString() {
        return subjectCode + " - " + subjectName;
    }
}
