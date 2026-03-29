package com.jobnet.app.data.repository;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.dto.JobDto;

public final class JobMapper {

    private JobMapper() {
    }

    public static Job fromDto(JobDto dto) {
        Job job = new Job();
        if (dto == null) {
            return job;
        }

        String id = safe(dto.id);
        if (id.isEmpty()) {
            id = Integer.toHexString(safe(dto.url).hashCode());
        }

        job.setId(id);
        job.setTitle(nonEmpty(dto.title, "Untitled Role"));
        job.setCompany(nonEmpty(dto.company, "Unknown Company"));
        job.setLocation(nonEmpty(dto.location, "Location not specified"));
        job.setSalary(nonEmpty(dto.salary, "Salary not disclosed"));
        job.setJobType(inferJobType(dto));
        job.setWorkMode(inferWorkMode(dto));
        job.setDescription(nonEmpty(dto.fullDescription, nonEmpty(dto.shortDescription, "No description available yet.")));
        job.setPostedDate(nonEmpty(dto.postDate, "Recently posted"));
        job.setExperience("2+ years");
        job.setApplicantsCount(parseIntSafe(dto.applicants, 0));
        job.setRating(parseFloatSafe(dto.rating, 4.4f));
        job.setLogoRes(resolveLogo(dto.company));
        job.setUrl(safe(dto.url));
        return job;
    }

    private static String inferJobType(JobDto dto) {
        String haystack = (safe(dto.title) + " " + safe(dto.shortDescription)).toLowerCase();
        if (haystack.contains("intern")) {
            return "Internship";
        }
        if (haystack.contains("part time") || haystack.contains("part-time")) {
            return "Part Time";
        }
        return "Full Time";
    }

    private static String inferWorkMode(JobDto dto) {
        String location = safe(dto.location).toLowerCase();
        if (location.contains("remote")) {
            return "Remote";
        }
        if (location.contains("hybrid")) {
            return "Hybrid";
        }
        return "On-site";
    }

    private static int resolveLogo(String company) {
        String c = safe(company).toLowerCase();
        if (c.contains("google") || c.contains("meta") || c.contains("microsoft") || c.contains("amazon")) {
            return R.drawable.ic_briefcase;
        }
        if (c.contains("design") || c.contains("figma") || c.contains("notion")) {
            return R.drawable.ic_edit;
        }
        if (c.contains("data") || c.contains("analytics")) {
            return R.drawable.ic_chart;
        }
        return R.drawable.ic_briefcase;
    }

    private static String nonEmpty(String candidate, String fallback) {
        String value = safe(candidate);
        return value.isEmpty() ? fallback : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static int parseIntSafe(String raw, int fallback) {
        try {
            String digits = raw == null ? "" : raw.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                return fallback;
            }
            return Integer.parseInt(digits);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float parseFloatSafe(String raw, float fallback) {
        try {
            String normalized = raw == null ? "" : raw.replace(',', '.').replaceAll("[^0-9.]", "");
            if (normalized.isEmpty()) {
                return fallback;
            }
            return Float.parseFloat(normalized);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
