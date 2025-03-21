package com.saif.jobnet.Models.Education;

import androidx.annotation.NonNull;

public class Class10Details extends EducationDetails {
    private String board;
    private String schoolName10th;
    private String medium;
    private String marks;

    public Class10Details(String id,String educationLevel,String board, String schoolName, String medium,
                          String marks, String passingYear) {
        super(id, educationLevel, passingYear,"Class10Details");
        this.board = board;
        this.schoolName10th = schoolName;
        this.medium = medium;
        this.marks = marks;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public String getSchoolName() {
        return schoolName10th;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName10th = schoolName;
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public String getMarks() {
        return marks;
    }

    public void setMarks(String marks) {
        this.marks = marks;
    }

//    public String getPassingYear() {
//        return passingYear;
//    }
//
//    public void setPassingYear(String passingYear) {
//        this.passingYear = passingYear;
//    }

    @NonNull
    @Override
    public String toString() {
        return "Class10Details{" +
                "board='" + board + '\'' +
                ", schoolName='" + schoolName10th + '\'' +
                ", medium='" + medium + '\'' +
                ", marks='" + marks + '\'' +
                ", passingYear='" + super.getPassingYear() + '\'' +
                '}';
    }
}
