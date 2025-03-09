package com.saif.jobnet.Models;

public class BasicDetails {
    private String gender;
    private String dateOfBirth;
    private String currentCity;
    private String homeTown;

    public BasicDetails() {
    }

    public BasicDetails(String gender, String dateOfBirth, String currentCity, String homeTown) {
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.currentCity = currentCity;
        this.homeTown = homeTown;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getCurrentCity() {
        return currentCity;
    }

    public void setCurrentCity(String currentCity) {
        this.currentCity = currentCity;
    }

    public String getHomeTown() {
        return homeTown;
    }

    public void setHomeTown(String homeTown) {
        this.homeTown = homeTown;
    }
}
