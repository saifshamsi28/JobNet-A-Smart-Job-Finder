package com.jobnet.app.data.network.dto;

public class LoginRequestDto {

    private String userNameOrEmail;
    private String password;

    public LoginRequestDto(String userNameOrEmail, String password) {
        this.userNameOrEmail = userNameOrEmail;
        this.password = password;
    }

    public String getUserNameOrEmail() {
        return userNameOrEmail;
    }

    public String getPassword() {
        return password;
    }
}
