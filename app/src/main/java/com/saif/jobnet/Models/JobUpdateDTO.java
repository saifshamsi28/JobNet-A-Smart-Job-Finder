package com.saif.jobnet.Models;

public class JobUpdateDTO {
    private String url;
    private String fullDescription;

    // Constructor
    public JobUpdateDTO(String url,String fullDescription) {
        this.url = url;
        this.fullDescription = fullDescription;
    }
    public JobUpdateDTO() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }
}
