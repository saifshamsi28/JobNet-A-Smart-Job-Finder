package com.jobnet.app.data.network.dto;

public class RegisterRequestDto {

    private String name;
    private String userName;
    private String email;
    private String password;
    private String phoneNumber;
    private String role;

    public RegisterRequestDto(String name, String userName, String email, String password, String phoneNumber, String role) {
        this.name = name;
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getRole() {
        return role;
    }
}
