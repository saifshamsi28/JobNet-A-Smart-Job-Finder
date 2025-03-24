package com.saif.jobnet.Models.Education;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

//@Entity(tableName = "education_10th")
@TypeConverters(Class10TypeConverter.class)
public class Class10Details {
    private String board;
    private String schoolName10th;
    private String medium;
    private String marks;
//    @PrimaryKey
//    @NonNull
    private String passingYear;

    public Class10Details() {
    }

    public Class10Details(String board, String schoolName, String medium,
                          String marks, @NonNull String passingYear) {
        this.board = board;
        this.schoolName10th = schoolName;
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

    public String getSchoolName10th() {
        return schoolName10th;
    }

    public void setSchoolName10th(String schoolName10th) {
        this.schoolName10th = schoolName10th;
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

    @NonNull
    public String getPassingYear() {
        return passingYear;
    }

    public void setPassingYear(@NonNull String passingYear) {
        this.passingYear = passingYear;
    }

    @NonNull
    @Override
    public String toString() {
        return "Class10Details{" +
                "board='" + board + '\'' +
                ", schoolName='" + schoolName10th + '\'' +
                ", medium='" + medium + '\'' +
                ", marks='" + marks + '\'' +
                ", passingYear='" + passingYear + '\'' +
                '}';
    }
}
