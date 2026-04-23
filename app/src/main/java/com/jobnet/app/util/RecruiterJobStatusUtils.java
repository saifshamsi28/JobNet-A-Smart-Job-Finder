package com.jobnet.app.util;

import com.jobnet.app.data.model.Job;

import java.util.Locale;

public final class RecruiterJobStatusUtils {

    public enum Bucket {
        ACTIVE,
        CLOSED,
        DRAFT
    }

    private RecruiterJobStatusUtils() {
    }

    public static Bucket resolveBucket(Job job) {
        String status = safe(job == null ? null : job.getStatus()).toUpperCase(Locale.ROOT);
        if ("DRAFT".equals(status)) {
            return Bucket.DRAFT;
        }
        if ("CLOSED".equals(status)) {
            return Bucket.CLOSED;
        }
        if ("PUBLISHED".equals(status) || "ACTIVE".equals(status) || "OPEN".equals(status)) {
            return Bucket.ACTIVE;
        }
        return inferActive(job) ? Bucket.ACTIVE : Bucket.CLOSED;
    }

    public static boolean inferActive(Job job) {
        String status = safe(job == null ? null : job.getStatus()).toUpperCase(Locale.ROOT);
        if (!status.isEmpty()) {
            return "PUBLISHED".equals(status) || "ACTIVE".equals(status) || "OPEN".equals(status);
        }
        String signal = (safe(job == null ? null : job.getTitle()) + " "
                + safe(job == null ? null : job.getJobType()) + " "
                + safe(job == null ? null : job.getWorkMode())).toLowerCase(Locale.ROOT);
        return !(signal.contains("closed") || signal.contains("inactive") || signal.contains("expired"));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
