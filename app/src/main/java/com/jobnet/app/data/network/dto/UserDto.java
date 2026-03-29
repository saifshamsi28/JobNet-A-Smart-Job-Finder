package com.jobnet.app.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class UserDto {

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("userName")
    public String userName;

    @SerializedName("email")
    public String email;

    @SerializedName(value = "role", alternate = {"userRole", "accountType"})
    public String role;

    @SerializedName("profileImage")
    public String profileImage;

    @SerializedName("skills")
    public List<String> skills = new ArrayList<>();

    @SerializedName("savedJobs")
    public List<JobDto> savedJobs = new ArrayList<>();
}
