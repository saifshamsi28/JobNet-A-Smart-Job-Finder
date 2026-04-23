package com.jobnet.app.ui.notifications;

public class NotificationItem {

    public static final String TYPE_RECRUITER_APPLICANT = "RECRUITER_APPLICANT";
    public static final String TYPE_SEEKER_STATUS = "SEEKER_STATUS";

    public final String id;
    public final String type;
    public final String title;
    public final String message;
    public final String status;
    public final String timestamp;
    public final long sortTimeMs;
    public final String jobId;
    public final String jobTitle;
    public final String company;
    public final String readKey;
    public boolean unread;

    public NotificationItem(
            String id,
            String type,
            String title,
            String message,
            String status,
            String timestamp,
            long sortTimeMs,
            String jobId,
            String jobTitle,
            String company,
            String readKey,
            boolean unread
    ) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
        this.sortTimeMs = sortTimeMs;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.company = company;
        this.readKey = readKey;
        this.unread = unread;
    }
}
