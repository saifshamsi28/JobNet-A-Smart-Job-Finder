package com.saif.jobnet.Models.Education;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "education_12th")
@TypeConverters(Class12TypeConverter.class)
public class Class12Details {
    private String board;
    private String schoolName;
    private String medium;
    private String stream;
    private String totalMarks;
    private String englishMarks;
    private String mathsMarks;
    @PrimaryKey
    @NonNull
    private String passingYear;

    public Class12Details() {
    }

    public Class12Details(String id, String board, String schoolName, String medium,
                          String stream, String totalMarks, String englishMarks,
                          String mathsMarks, @NonNull String passingYear, String educationLevel) {
//        super(id, educationLevel, passingYear,"Class12Details");
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

    @NonNull
    public String getPassingYear() {
        return passingYear;
    }

    public void setPassingYear(@NonNull String passingYear) {
        this.passingYear = passingYear;
    }
//
//    public String getEducationLevel() {
//        return level;
//    }
//
//    public void setEducationLevel(String educationLevel) {
//        this.level = educationLevel;
//    }

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

//    @Override
//    public String getEducationType() {
//        return "Class12th";
//    }
}
