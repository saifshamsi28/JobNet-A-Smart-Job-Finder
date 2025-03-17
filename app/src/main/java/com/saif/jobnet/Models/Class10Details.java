package com.saif.jobnet.Models;

import androidx.annotation.NonNull;

import com.saif.jobnet.EducationDetails;

public class Class10Details implements EducationDetails {
    private String board;
    private String schoolName;
    private String medium;
    private String marks;
    private String passingYear;

    public Class10Details(String board, String schoolName, String medium,
                          String marks, String passingYear) {
        this.board = board;
        this.schoolName = schoolName;
        this.medium = medium;
        this.marks = marks;
        this.passingYear = passingYear;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
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

    public String getPassingYear() {
        return passingYear;
    }

    public void setPassingYear(String passingYear) {
        this.passingYear = passingYear;
    }

    @NonNull
    @Override
    public String toString() {
        return "Class10Details{" +
                "board='" + board + '\'' +
                ", schoolName='" + schoolName + '\'' +
                ", medium='" + medium + '\'' +
                ", marks='" + marks + '\'' +
                ", passingYear='" + passingYear + '\'' +
                '}';
    }
}
