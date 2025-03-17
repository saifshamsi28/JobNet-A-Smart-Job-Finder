package com.saif.jobnet.Models;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;


import com.saif.jobnet.Utils.Converters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "user")
public class User {
    @PrimaryKey
    @NonNull
    private String id; // Same as the backend-provided ID

    private String name;
    private String userName;
    private String email;
    private String password;
    private String profileImage;
    private String phoneNumber;
    private boolean isResumeUploaded;
    private String resumeUrl;
    private String resumeName;
    private String resumeUploadDate;
    private String resumeSize;

    @TypeConverters(Converters.class) // Convert List<Job> to a storable format
    private List<Job> savedJobs = new ArrayList<>();

    @Embedded
    private BasicDetails basicDetails;

    @TypeConverters(EducationTypeConverter.class)
    private List<Education> educationList = new ArrayList<>();

    // Constructor for Room
    public User(@NonNull String id, String name, String userName, String email,
                String password, String phoneNumber,
                boolean isResumeUploaded, String resumeUrl, String resumeUploadDate,
                String resumeName, String resumeSize) {
        this.id = id;
        this.name = name;
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.isResumeUploaded = isResumeUploaded;
        this.resumeUrl = resumeUrl;
        this.resumeUploadDate = resumeUploadDate;
        this.resumeName = resumeName;
        this.resumeSize = resumeSize;
    }

    // Constructor for new users (e.g., registration)
    @Ignore
    public User(String name, String userName, String email, String password, String phoneNumber) {
        this.name = name;
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.isResumeUploaded = false;
        this.resumeUrl = "";
        this.resumeUploadDate="";
        this.resumeName="";
        this.resumeSize="";
    }

    // Getters and Setters
    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public List<Job> getSavedJobs() {
        return savedJobs;
    }

    public void setSavedJobs(List<Job> savedJobs) {
        this.savedJobs = savedJobs;
    }

    public boolean isResumeUploaded() {
        return isResumeUploaded;
    }

    public void setResumeUploaded(boolean resumeUploaded) {
        isResumeUploaded = resumeUploaded;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }

    public String getResumeUploadDate() {
        return resumeUploadDate;
    }

    public void setResumeUploadDate(String resumeUploadDate) {
        this.resumeUploadDate = resumeUploadDate;
    }

    public String getResumeName() {
        return resumeName;
    }

    public void setResumeName(String resumeName) {
        this.resumeName = resumeName;
    }

    public String getResumeSize() {
        return resumeSize;
    }

    public void setResumeSize(String resumeSize) {
        this.resumeSize = resumeSize;
    }

    public BasicDetails getBasicDetails() {
        return basicDetails;
    }

    public void setBasicDetails(BasicDetails basicDetails) {
        this.basicDetails = basicDetails;
    }

    public List<Education> getEducationList() {
        return educationList;
    }

    public void setEducationList(List<Education> educationList) {
        this.educationList = educationList;
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", isResumeUploaded=" + isResumeUploaded +
                ", resumeUrl='" + resumeUrl + '\'' +
                ", resumeName='" + resumeName + '\'' +
                ", resumeUploadDate='" + resumeUploadDate + '\'' +
                ", resumeSize='" + resumeSize + '\'' +
                ", basicDetails=" + basicDetails +
                ", educationList=" + educationList +
                '}';
    }
}

