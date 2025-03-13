package com.saif.jobnet.Models;

import androidx.annotation.NonNull;

public class JobNetResponse {
    private String message;
    private int status;

    public JobNetResponse(String message, int status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @NonNull
    @Override
    public String toString() {
        return "JobNetResponse{" + "message='" + message + '\'' + ", status=" + status + '}';
    }
}