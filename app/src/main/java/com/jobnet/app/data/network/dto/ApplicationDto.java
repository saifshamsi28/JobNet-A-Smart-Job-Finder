package com.jobnet.app.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class ApplicationDto {

    @SerializedName("id")
    public String id;

    @SerializedName("userId")
    public String userId;

    @SerializedName("recruiterId")
    public String recruiterId;

    @SerializedName("jobId")
    public String jobId;

    @SerializedName("jobTitle")
    public String jobTitle;

    @SerializedName(value = "company", alternate = {"company_name"})
    public String company;

    @SerializedName("resumeUrl")
    public String resumeUrl;

    @SerializedName("coverLetter")
    public String coverLetter;

    @SerializedName("status")
    public String status;

    @SerializedName("appliedAt")
    public String appliedAt;

    @SerializedName("updatedAt")
    public String updatedAt;
}
