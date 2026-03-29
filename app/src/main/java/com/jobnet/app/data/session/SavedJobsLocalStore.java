package com.jobnet.app.data.session;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SavedJobsLocalStore {

    private static final String PREF_NAME = "JobNetSavedJobs";
    private static final String KEY_SAVED_IDS = "saved_job_ids";

    private final SharedPreferences prefs;

    public SavedJobsLocalStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized Set<String> getSavedIds() {
        Set<String> ids = prefs.getStringSet(KEY_SAVED_IDS, Collections.emptySet());
        return new HashSet<>(ids);
    }

    public synchronized boolean isSaved(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return false;
        }
        return getSavedIds().contains(jobId);
    }

    public synchronized void setSaved(String jobId, boolean saved) {
        if (jobId == null || jobId.isBlank()) {
            return;
        }
        Set<String> ids = getSavedIds();
        if (saved) {
            ids.add(jobId);
        } else {
            ids.remove(jobId);
        }
        prefs.edit().putStringSet(KEY_SAVED_IDS, ids).apply();
    }

    public synchronized void clear() {
        prefs.edit().clear().apply();
    }
}
