package com.saif.jobnet.Models.Resume;

import androidx.annotation.NonNull;

public class Resume {
    private String resumeName;
    private String resumeUrl="";
    private String resumeUploadDate;
    private String resumeSize;

    public Resume() {
    }

    public Resume(String resumeName, String resumeUrl, String resumeUploadDate, String resumeSize) {
        this.resumeName = resumeName;
        this.resumeUrl = resumeUrl;
        this.resumeUploadDate = resumeUploadDate;
        this.resumeSize = resumeSize;
    }

    public String getResumeName() {
        return resumeName;
    }

    public void setResumeName(String resumeName) {
        this.resumeName = resumeName;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }

    public String getResumeUploadDate() {
        return resumeUploadDate;
    }

    public void setResumeUploadDate(String resumeUploadDate) {
        this.resumeUploadDate = resumeUploadDate;
    }

    public String getResumeSize() {
        return resumeSize;
    }

    public void setResumeSize(String resumeSize) {
        this.resumeSize = resumeSize;
    }

    @NonNull
    @Override
    public String toString() {
        // Convert the Resume object to a string representation
        return "Resume{" +
                " resumeName='" + resumeName + '\'' +
                ", resumeUrl='" + resumeUrl + '\'' +
                ", resumeUploadDate='" + resumeUploadDate + '\'' +
                ", resumeSize='" + resumeSize + '\'' +
                '}';
    }
}
