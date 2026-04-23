package com.jobnet.app.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class JobDto {

    @SerializedName(value = "id", alternate = {"_id"})
    public String id;

    @SerializedName("title")
    public String title;

    @SerializedName(value = "company", alternate = {"company_name"})
    public String company;

    @SerializedName("location")
    public String location;

    @SerializedName("salary")
    public String salary;

    @SerializedName("minSalary")
    public long minSalary;

    @SerializedName("maxSalary")
    public long maxSalary;

    @SerializedName(value = "link", alternate = {"url"})
    public String url;

    @SerializedName("rating")
    public String rating;

    @SerializedName("reviews")
    public String reviews;

    @SerializedName("openings")
    public String openings;

    @SerializedName("category")
    public String category;

    @SerializedName("requiredSkills")
    public List<String> requiredSkills = new ArrayList<>();

    @SerializedName("applicants")
    public String applicants;

    @SerializedName(value = "post_date", alternate = {"postDate"})
    public String postDate;

    @SerializedName(value = "description", alternate = {"shortDescription"})
    public String shortDescription;

    @SerializedName(value = "full_description", alternate = {"fullDescription"})
    public String fullDescription;

    @SerializedName("employmentType")
    public String employmentType;

    @SerializedName("jobType")
    public String jobType;

    @SerializedName("workMode")
    public String workMode;

    @SerializedName("status")
    public String status;

    @SerializedName("source")
    public String source;

    @SerializedName("postedByUserId")
    public String postedByUserId;

    @SerializedName("dateTime")
    public String dateTime;

    @SerializedName("updatedAt")
    public String updatedAt;
}
