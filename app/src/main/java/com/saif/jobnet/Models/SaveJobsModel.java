package com.saif.jobnet.Models;

public class SaveJobsModel {
    private String userId;
    private String jobId;
    private boolean wantToSave;

    public SaveJobsModel(String userId, String jobId, boolean wantToSave) {
        this.userId = userId;
        this.jobId = jobId;
        this.wantToSave =wantToSave;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
