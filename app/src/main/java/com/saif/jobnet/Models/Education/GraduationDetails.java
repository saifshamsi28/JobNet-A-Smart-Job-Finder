package com.saif.jobnet.Models.Education;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ug_education")
public class GraduationDetails extends EducationDetails{

    private String course;
    private String specialization;
    private String college;
    private String courseType;  // Full-time / Part-time
    private String gpaScale;
    private String cgpaObtained;
    private String enrollmentYear;

    // Default constructor
    public GraduationDetails() {
    }

    // Constructor
    public GraduationDetails(String id, String educationLevel,String course, String specialization, String college,
                             String courseType, String gpaScale, String cgpaObtained, String enrollmentYear, String passingYear) {
//        this.id = id;
//        this.educationLevel = educationLevel;
        super(id, educationLevel, passingYear,"GraduationDetails");
        this.course = course;
        this.specialization = specialization;
        this.college = college;
        this.courseType = courseType;
        this.gpaScale = gpaScale;
        this.cgpaObtained = cgpaObtained;
        this.enrollmentYear = enrollmentYear;
    }

    // Getters and Setters
//
//    @NonNull
//    public String getId() {
//        return id;
//    }
//
//    public void setId(@NonNull String id) {
//        this.id = id;
//    }

//    public String getEducationLevel() {
//        return educationLevel;
//    }
//
//    public void setEducationLevel(String educationLevel) {
//        this.educationLevel = educationLevel;
//    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public String getGpaScale() {
        return gpaScale;
    }

    public void setGpaScale(String gpaScale) {
        this.gpaScale = gpaScale;
    }

    public String getCgpaObtained() {
        return cgpaObtained;
    }

    public void setCgpaObtained(String cgpaObtained) {
        this.cgpaObtained = cgpaObtained;
    }

    public String getEnrollmentYear() {
        return enrollmentYear;
    }

    public void setEnrollmentYear(String enrollmentYear) {
        this.enrollmentYear = enrollmentYear;
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
        return "GraduationDetails{" +
                "id=" + super.getId() +
                ", educationLevel='" + super.getEducationLevel() + '\'' +
                ", course='" + course + '\'' +
                ", specialization='" + specialization + '\'' +
                ", college='" + college + '\'' +
                ", courseType='" + courseType + '\'' +
                ", gpaScale='" + gpaScale + '\'' +
                ", cgpaObtained='" + cgpaObtained + '\'' +
                ", enrollmentYear='" + enrollmentYear + '\'' +
                ", passingYear='" + super.getPassingYear() + '\'' +
                '}';
    }
}