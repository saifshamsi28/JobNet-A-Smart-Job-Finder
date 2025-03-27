package com.saif.jobnet.Models.Resume;

import androidx.annotation.NonNull;

public class ResumeResponseEntity {
    private String message;
    private int status;

    public ResumeResponseEntity(String message, int status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    @NonNull
    @Override
    public String toString() {
        return "ResumeResponseEntity{" + "message=" + message + ", status= "+ status+ "}";
    }
}
