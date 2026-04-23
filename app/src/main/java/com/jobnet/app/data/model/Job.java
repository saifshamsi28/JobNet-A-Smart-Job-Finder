package com.jobnet.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Job implements Serializable {
    private String id;
    private String title;
    private String company;
    private String location;
    private String salary;
    private String jobType;
    private String workMode;
    private String description;
    private String postedDate;
    private String url;
    private String deadline;
    private int applicantsCount;
    private int logoRes;
    private boolean saved;
    private float rating;
    private String experience;
    private String openings;
    private String category;
    private List<String> requiredSkills = new ArrayList<>();
    private String status;
    private String source;
    private String dateTime;
    private String updatedAt;

    public Job() {}

    public Job(String id, String title, String company, String location,
               String salary, String jobType, String workMode,
               String description, String postedDate, int logoRes) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.salary = salary;
        this.jobType = jobType;
        this.workMode = workMode;
        this.description = description;
        this.postedDate = postedDate;
        this.logoRes = logoRes;
        this.saved = false;
        this.rating = 4.5f;
        this.experience = "2+ years";
        this.applicantsCount = 127;
        this.deadline = "Jan 30, 2025";
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    // Compatibility accessor used across older UI classes.
    public String getType() { return jobType; }

    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPostedDate() { return postedDate; }
    public void setPostedDate(String postedDate) { this.postedDate = postedDate; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    // Compatibility accessor used across older UI classes.
    public String getPostedTime() { return postedDate; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public int getApplicantsCount() { return applicantsCount; }
    public void setApplicantsCount(int applicantsCount) { this.applicantsCount = applicantsCount; }

    public int getLogoRes() { return logoRes; }
    public void setLogoRes(int logoRes) { this.logoRes = logoRes; }

    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getOpenings() { return openings; }
    public void setOpenings(String openings) { this.openings = openings; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills == null ? new ArrayList<>() : new ArrayList<>(requiredSkills);
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
