package com.jobnet.app.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class UserUpdateRequestDto {

    @SerializedName("id")
    public final String id;

    @SerializedName("name")
    public final String name;

    @SerializedName("email")
    public final String email;

    @SerializedName("password")
    public final String password;

    @SerializedName("phoneNumber")
    public final String phoneNumber;

    public UserUpdateRequestDto(String id, String name, String email, String password, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }
}
