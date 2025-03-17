package com.saif.jobnet.Models;

import androidx.annotation.NonNull;

import com.saif.jobnet.EducationDetails;

public class Class12Details implements EducationDetails {
    private String board;
    private String schoolName;
    private String medium;
    private String stream;
    private String totalMarks;
    private String englishMarks;
    private String mathsMarks;
    private String passingYear;

    public Class12Details(String board, String schoolName, String medium,
                          String stream, String totalMarks, String englishMarks,
                          String mathsMarks, String passingYear) {
        this.board = board;
        this.schoolName = schoolName;
        this.medium = medium;
        this.stream = stream;
        this.totalMarks = totalMarks;
        this.englishMarks = englishMarks;
        this.mathsMarks = mathsMarks;
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

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(String totalMarks) {
        this.totalMarks = totalMarks;
    }

    public String getEnglishMarks() {
        return englishMarks;
    }

    public void setEnglishMarks(String englishMarks) {
        this.englishMarks = englishMarks;
    }

    public String getMathsMarks() {
        return mathsMarks;
    }

    public void setMathsMarks(String mathsMarks) {
        this.mathsMarks = mathsMarks;
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
        return "Class12Details{" +
                "board='" + board + '\'' +
                ", schoolName='" + schoolName + '\'' +
                ", medium='" + medium + '\'' +
                ", stream='" + stream + '\'' +
                ", totalMarks='" + totalMarks + '\'' +
                ", englishMarks='" + englishMarks + '\'' +
                ", mathsMarks='" + mathsMarks + '\'' +
                ", passingYear='" + passingYear + '\'' +
                '}';
    }
}
