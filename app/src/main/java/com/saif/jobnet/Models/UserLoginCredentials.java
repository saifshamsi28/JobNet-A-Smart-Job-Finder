package com.saif.jobnet.Models;

public class UserLoginCredentials {
    String userNameOrEmail;
    String password;

    public UserLoginCredentials(){
    }

    public String getUserNameOrEmail() {
        return userNameOrEmail;
    }

    public void setUserNameOrEmail(String userNameOrEmail) {
        this.userNameOrEmail = userNameOrEmail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
