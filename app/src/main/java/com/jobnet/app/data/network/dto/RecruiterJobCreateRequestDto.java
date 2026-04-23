package com.jobnet.app.data.network.dto;

import java.util.ArrayList;
import java.util.List;

public class RecruiterJobCreateRequestDto {

    public String title;
    public String company;
    public String location;
    public String salary;
    public String openings;
    public String shortDescription;
    public String fullDescription;
    public String employmentType;
    public String jobType;
    public String workMode;
    public String category;
    public List<String> requiredSkills = new ArrayList<>();
    public String url;
    public String status;

    public RecruiterJobCreateRequestDto(String title,
                                        String company,
                                        String location,
                                        String salary,
                                        String openings,
                                        String shortDescription,
                                        String fullDescription,
                                        String employmentType,
                                        String jobType,
                                        String workMode,
                                        String category,
                                        List<String> requiredSkills,
                                        String url) {
        this(title, company, location, salary, openings, shortDescription, fullDescription, employmentType, jobType, workMode, category, requiredSkills, url, null);
    }

    public RecruiterJobCreateRequestDto(String title,
                                        String company,
                                        String location,
                                        String salary,
                                        String openings,
                                        String shortDescription,
                                        String fullDescription,
                                        String employmentType,
                                        String jobType,
                                        String workMode,
                                        String category,
                                        List<String> requiredSkills,
                                        String url,
                                        String status) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.salary = salary;
        this.openings = openings;
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.employmentType = employmentType;
        this.jobType = jobType;
        this.workMode = workMode;
        this.category = category;
        this.requiredSkills = requiredSkills == null ? new ArrayList<>() : new ArrayList<>(requiredSkills);
        this.url = url;
        this.status = status;
    }
}
