package com.jobnet.app.data.network.dto;

public class RecruiterJobCreateRequestDto {

    public String title;
    public String company;
    public String location;
    public String salary;
    public String shortDescription;
    public String fullDescription;
    public String jobType;
    public String workMode;
    public String url;

    public RecruiterJobCreateRequestDto(String title,
                                        String company,
                                        String location,
                                        String salary,
                                        String shortDescription,
                                        String fullDescription,
                                        String jobType,
                                        String workMode,
                                        String url) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.salary = salary;
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.jobType = jobType;
        this.workMode = workMode;
        this.url = url;
    }
}
