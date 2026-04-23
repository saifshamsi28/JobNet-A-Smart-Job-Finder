package com.jobnet.app.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class AuthResponseDto {

    @SerializedName("message")
    public String message;

    @SerializedName("status")
    public int status;

    @SerializedName("accessToken")
    public String accessToken;

    @SerializedName("refreshToken")
    public String refreshToken;
}
