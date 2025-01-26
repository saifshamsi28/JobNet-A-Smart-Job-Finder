package com.saif.jobnet.Models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.lang.annotation.Annotation;

@Entity(
        tableName = "jobs",
        indices = {
                @Index(value = "title"), // Index on title for faster searches
                @Index(value = "jobId", unique = true), // Unique index on jobId
                @Index(value = "url") // Index on URL for faster lookups
        }
)
public class Job implements SerializedName{
    @PrimaryKey
    @SerializedName("id")
    @NonNull
    private String jobId;

    @SerializedName("title")
    private String title;

    @SerializedName("company")
    private String company;

    @SerializedName("location")
    private String location;

    @SerializedName("salary")
    private String salary;

    @SerializedName("link")
    private String url;

    @SerializedName("rating")
    private String rating;

    @SerializedName("reviews")
    private String review;

    @SerializedName("post_date")
    private String postDate;

    @SerializedName("openings")
    @ColumnInfo(name = "openings")
    private String openings;

    @SerializedName("applicants")
    @ColumnInfo(name = "applicants")
    private String applicants;

    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    @SerializedName("description")
    private String shortDescription;

    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    @SerializedName("full_description")
    private String fullDescription;


    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public void setPostDate(String postDate) {
        this.postDate = postDate;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setOpenings(String openings) {
        this.openings = openings;
    }
    public void setApplicants(String applicants) {
        this.applicants = applicants;
    }

    public String getJobId() {
        return jobId;
    }
    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getLocation() {
        return location;
    }

    public String getSalary() {
        return salary;
    }

    public String getOpenings() {
        return openings;
    }
    public String getApplicants() {
        return applicants;
    }
    public String getUrl() {
        return url;
    }

    public String getRating() {
        return rating;
    }
    public String getPostDate() {
        return postDate;
    }

    public String getShortDescription() {
        return shortDescription;
    }
    public String getReview() {
        return review;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }

    @Override
    public String value() {
        return "";
    }

    @Override
    public String[] alternate() {
        return new String[0];
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }
}
