package com.saif.jobnet.Models;

public class Education {
    private String title;
    private String college;
    private String graduationYear;

    public Education(String title, String college, String graduationYear) {
        this.title = title;
        this.college = college;
        this.graduationYear = graduationYear;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }
    public String getGraduationYear() {
        return graduationYear;
    }


    public void setGraduationYear(String graduationYear) {
        this.graduationYear = graduationYear;
    }
}
