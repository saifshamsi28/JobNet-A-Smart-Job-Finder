package com.saif.jobnet.Models.Education;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "education_details")
@TypeConverters(EducationTypeConverter.class)
public class EducationDetails {

    @PrimaryKey
    @NonNull
    private String id;
    private String educationLevel; // UG, PG, 12th, 10th
    private String passingYear;
    private String educationType;

    public EducationDetails() {} // Default constructor

    public EducationDetails(@NonNull String id, String educationLevel, String passingYear, String educationType) {
        this.id = id;
        this.educationLevel = educationLevel;
        this.passingYear = passingYear;
        this.educationType = educationType;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
    public String getPassingYear() { return passingYear; }
    public void setPassingYear(String passingYear) { this.passingYear = passingYear; }

    public String getEducationType() {
        return educationType;
    }

    public void setEducationType(String educationType) {
        this.educationType = educationType;
    }

    @NonNull
    @Override
    public String toString() {
        return "EducationDetails{" +
                "id='" + id + '\'' +
                ", educationLevel='" + educationLevel + '\'' +
                ", passingYear='" + passingYear + '\'' +
                ", educationType='" + educationType + '\'' +
                '}';
    }
}
