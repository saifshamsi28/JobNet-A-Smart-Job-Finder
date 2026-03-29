package com.jobnet.app.data.network.dto;

public class SaveJobRequestDto {

    private String userId;
    private String jobId;
    private boolean wantToSave;

    public SaveJobRequestDto(String userId, String jobId, boolean wantToSave) {
        this.userId = userId;
        this.jobId = jobId;
        this.wantToSave = wantToSave;
    }

    public String getUserId() {
        return userId;
    }

    public String getJobId() {
        return jobId;
    }

    public boolean isWantToSave() {
        return wantToSave;
    }
}
