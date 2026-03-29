package com.jobnet.app.data.model;

public class JobCategory {
    private String name;
    private int iconRes;
    private int jobCount;
    private int tintColor;

    public JobCategory(String name, int iconRes, int jobCount, int tintColor) {
        this.name = name;
        this.iconRes = iconRes;
        this.jobCount = jobCount;
        this.tintColor = tintColor;
    }

    public String getName() { return name; }
    public int getIconRes() { return iconRes; }
    public int getJobCount() { return jobCount; }
    public int getTintColor() { return tintColor; }
}
