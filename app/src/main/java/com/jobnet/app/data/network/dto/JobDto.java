package com.jobnet.app.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class JobDto {

    @SerializedName("id")
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

    @SerializedName("applicants")
    public String applicants;

    @SerializedName(value = "post_date", alternate = {"postDate"})
    public String postDate;

    @SerializedName(value = "description", alternate = {"shortDescription"})
    public String shortDescription;

    @SerializedName(value = "full_description", alternate = {"fullDescription"})
    public String fullDescription;
}
