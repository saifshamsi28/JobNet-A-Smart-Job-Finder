package com.saif.jobnet.Models.Education;

import androidx.annotation.NonNull;

public class UGDetails  {
    private String course;
    private String specialization;
    private String college;
    private String courseType;
    private String gpaScale;
    private String cgpaObtained;
    private String enrollmentYear;
    private String passingYear;

    // Constructor
    public UGDetails(String course, String specialization, String college, String courseType,
                     String gpaScale, String cgpa, String enrollmentYear,
                     String passingYear) {
        this.course = course;
        this.specialization = specialization;
        this.college = college;
        this.courseType = courseType;
        this.gpaScale = gpaScale;
        this.cgpaObtained = cgpa;
        this.enrollmentYear = enrollmentYear;
        this.passingYear = passingYear;
    }

    // Getters and Setters

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

    public String getPassingYear() {
        return passingYear;
    }

    public void setPassingYear(String passingYear) {
        this.passingYear = passingYear;
    }

    @NonNull
    @Override
    public String toString() {
        return "UGDetails{" +
                "course='" + course + '\'' +
                ", specialization='" + specialization + '\'' +
                ", college='" + college + '\'' +
                ", courseType='" + courseType + '\'' +
                ", gpaScale='" + gpaScale + '\'' +
                ", cgpaObtained='" + cgpaObtained + '\'' +
                ", enrollmentYear='" + enrollmentYear + '\'' +
                ", passingYear='" + passingYear + '\'' +
                '}';
    }

//    @Override
//    public String getEducationType() {
//        return "UGDetails";
//    }
}
