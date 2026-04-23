package com.jobnet.app.data.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

public class RecruiterJobDraftStore {

    private static final String PREFS = "jobnet_recruiter_draft";
    private static final String KEY_DRAFT = "post_job_draft";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public RecruiterJobDraftStore(@NonNull Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(@NonNull DraftJob draft) {
        prefs.edit().putString(KEY_DRAFT, gson.toJson(draft)).apply();
    }

    @Nullable
    public DraftJob load() {
        String raw = prefs.getString(KEY_DRAFT, null);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(raw, DraftJob.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void clear() {
        prefs.edit().remove(KEY_DRAFT).apply();
    }

    public boolean hasDraft() {
        return load() != null;
    }

    public static class DraftJob {
        public String title;
        public String company;
        public String location;
        public String salary;
        public String openings;
        public String employmentType;
        public String workMode;
        public String category;
        public String requiredSkills;
        public String shortDescription;
        public String fullDescription;

        public boolean isEmpty() {
            return isBlank(title)
                    && isBlank(company)
                    && isBlank(location)
                    && isBlank(salary)
                    && isBlank(openings)
                    && isBlank(employmentType)
                    && isBlank(workMode)
                    && isBlank(category)
                    && isBlank(requiredSkills)
                    && isBlank(shortDescription)
                    && isBlank(fullDescription);
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
