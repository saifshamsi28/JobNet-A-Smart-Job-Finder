package com.jobnet.app.data.network.dto;

public class ApplyJobRequestDto {

    public String userId;
    public String jobId;
    public String resumeUrl;
    public String coverLetter;

    public ApplyJobRequestDto(String userId, String jobId, String resumeUrl, String coverLetter) {
        this.userId = userId;
        this.jobId = jobId;
        this.resumeUrl = resumeUrl;
        this.coverLetter = coverLetter;
    }
}
