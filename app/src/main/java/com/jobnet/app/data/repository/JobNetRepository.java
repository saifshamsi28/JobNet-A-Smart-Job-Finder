package com.jobnet.app.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.ApiClient;
import com.jobnet.app.data.network.JobNetApiService;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.network.dto.ApplyJobRequestDto;
import com.jobnet.app.data.network.dto.JobDto;
import com.jobnet.app.data.network.dto.RecruiterJobCreateRequestDto;
import com.jobnet.app.data.network.dto.SaveJobRequestDto;
import com.jobnet.app.data.network.dto.UserDto;
import com.jobnet.app.data.session.SavedJobsLocalStore;
import com.jobnet.app.data.session.SessionManager;
import com.jobnet.app.util.SampleData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JobNetRepository {

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(Throwable throwable);
    }

    public static class HomeData {
        public final List<Job> featured;
        public final List<Job> recommended;

        public HomeData(List<Job> featured, List<Job> recommended) {
            this.featured = featured;
            this.recommended = recommended;
        }
    }

    private static volatile JobNetRepository instance;

    private final JobNetApiService api;
    private final SessionManager sessionManager;
    private final SavedJobsLocalStore localSavedStore;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<Job> cachedJobs = new ArrayList<>();

    private JobNetRepository(Context context) {
        this.api = ApiClient.getApiService();
        this.sessionManager = new SessionManager(context);
        this.localSavedStore = new SavedJobsLocalStore(context);
    }

    public static JobNetRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (JobNetRepository.class) {
                if (instance == null) {
                    instance = new JobNetRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void loadHomeData(@NonNull DataCallback<HomeData> callback) {
        api.getSuggestedJobs().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<JobDto>> call, @NonNull Response<List<JobDto>> suggestedResp) {
                api.getRecentJobs().enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<List<JobDto>> call, @NonNull Response<List<JobDto>> recentResp) {
                        if (!suggestedResp.isSuccessful() && !recentResp.isSuccessful()) {
                            fallbackHome(callback, null);
                            return;
                        }

                        List<Job> suggested = mapJobs(suggestedResp.body());
                        List<Job> recent = mapJobs(recentResp.body());

                        syncSavedFlags(suggested);
                        syncSavedFlags(recent);

                        mergeIntoCache(suggested, recent);

                        if (suggested.isEmpty() && recent.isEmpty()) {
                            fallbackHome(callback, null);
                            return;
                        }

                        postSuccess(callback, new HomeData(limit(suggested, 8), limit(recent, 20)));
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<JobDto>> call, @NonNull Throwable t) {
                        fallbackHome(callback, t);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<List<JobDto>> call, @NonNull Throwable t) {
                fallbackHome(callback, t);
            }
        });
    }

    public void searchJobs(String query, String activeFilter, @NonNull DataCallback<List<Job>> callback) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty() && cachedJobs.isEmpty()) {
            loadHomeData(new DataCallback<>() {
                @Override
                public void onSuccess(HomeData data) {
                    postSuccess(callback, applyLocalFilter(new ArrayList<>(cachedJobs), "", activeFilter));
                }

                @Override
                public void onError(Throwable throwable) {
                    postSuccess(callback, applyLocalFilter(SampleData.getAllJobs(), "", activeFilter));
                }
            });
            return;
        }

        String locationFilter = "Remote".equalsIgnoreCase(activeFilter) ? "Remote" : null;
        Integer minSalary = null;

        api.searchJobs(normalized.isEmpty() ? null : normalized, locationFilter, null, minSalary, null)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<List<JobDto>> call, @NonNull Response<List<JobDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Job> jobs = mapJobs(response.body());
                            syncSavedFlags(jobs);
                            mergeIntoCache(jobs);
                            postSuccess(callback, applyLocalFilter(jobs, normalized, activeFilter));
                            return;
                        }
                        postSuccess(callback, applyLocalFilter(new ArrayList<>(cachedJobs), normalized, activeFilter));
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<JobDto>> call, @NonNull Throwable t) {
                        List<Job> fallbackSource = cachedJobs.isEmpty() ? SampleData.getAllJobs() : new ArrayList<>(cachedJobs);
                        postSuccess(callback, applyLocalFilter(fallbackSource, normalized, activeFilter));
                    }
                });
    }

    public void loadSavedJobs(@NonNull DataCallback<List<Job>> callback) {
        String token = sessionManager.getAuthToken();
        if (token == null || token.isBlank()) {
            List<Job> fallback = applySavedOnly(cachedJobs.isEmpty() ? SampleData.getAllJobs() : new ArrayList<>(cachedJobs));
            postSuccess(callback, fallback);
            return;
        }

        api.getLoggedInUser("Bearer " + token).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UserDto> call, @NonNull Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().savedJobs != null) {
                    List<Job> jobs = mapJobs(response.body().savedJobs);
                    for (Job job : jobs) {
                        if (job.getId() != null) {
                            localSavedStore.setSaved(job.getId(), true);
                        }
                        job.setSaved(true);
                    }
                    mergeIntoCache(jobs);
                    postSuccess(callback, jobs);
                    return;
                }
                List<Job> fallback = applySavedOnly(cachedJobs.isEmpty() ? SampleData.getAllJobs() : new ArrayList<>(cachedJobs));
                postSuccess(callback, fallback);
            }

            @Override
            public void onFailure(@NonNull Call<UserDto> call, @NonNull Throwable t) {
                List<Job> fallback = applySavedOnly(cachedJobs.isEmpty() ? SampleData.getAllJobs() : new ArrayList<>(cachedJobs));
                postSuccess(callback, fallback);
            }
        });
    }

    public void fetchProfile(@NonNull DataCallback<UserDto> callback) {
        String token = sessionManager.getAuthToken();
        if (token == null || token.isBlank()) {
            postSuccess(callback, null);
            return;
        }

        api.getLoggedInUser("Bearer " + token).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UserDto> call, @NonNull Response<UserDto> response) {
                if (response.isSuccessful()) {
                    postSuccess(callback, response.body());
                } else {
                    postSuccess(callback, null);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserDto> call, @NonNull Throwable t) {
                postError(callback, t);
            }
        });
    }

    public void loadJobDetails(Job seed, @NonNull DataCallback<Job> callback) {
        if (seed == null || isBlank(seed.getId())) {
            postSuccess(callback, seed);
            return;
        }

        if (!isBlank(seed.getUrl())) {
            api.getJobDescription(seed.getId(), seed.getUrl()).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<JobDto> call, @NonNull Response<JobDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Job updated = mergeJobDetails(seed, JobMapper.fromDto(response.body()));
                        postSuccess(callback, updated);
                        return;
                    }
                    fallbackDetailById(seed, callback);
                }

                @Override
                public void onFailure(@NonNull Call<JobDto> call, @NonNull Throwable t) {
                    fallbackDetailById(seed, callback);
                }
            });
            return;
        }

        fallbackDetailById(seed, callback);
    }

    public void applyToJob(Job job, @NonNull DataCallback<ApplicationDto> callback) {
        if (job == null || isBlank(job.getId())) {
            postError(callback, new IllegalArgumentException("Invalid job"));
            return;
        }

        String token = sessionManager.getAuthToken();
        String userId = sessionManager.getUserId();
        if (isBlank(token) || isBlank(userId)) {
            postError(callback, new IllegalStateException("Please login first"));
            return;
        }

        ApplyJobRequestDto request = new ApplyJobRequestDto(userId, job.getId(), null, null);
        api.applyToJob("Bearer " + token, request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApplicationDto> call, @NonNull Response<ApplicationDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                    return;
                }
                postError(callback, new IllegalStateException("Failed to apply for this job"));
            }

            @Override
            public void onFailure(@NonNull Call<ApplicationDto> call, @NonNull Throwable t) {
                postError(callback, t);
            }
        });
    }

    public void loadMyApplications(@NonNull DataCallback<List<ApplicationDto>> callback) {
        String token = sessionManager.getAuthToken();
        String userId = sessionManager.getUserId();
        if (isBlank(token) || isBlank(userId)) {
            postSuccess(callback, List.of());
            return;
        }

        api.getMyApplications("Bearer " + token, userId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<ApplicationDto>> call, @NonNull Response<List<ApplicationDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                    return;
                }
                postSuccess(callback, List.of());
            }

            @Override
            public void onFailure(@NonNull Call<List<ApplicationDto>> call, @NonNull Throwable t) {
                postError(callback, t);
            }
        });
    }

    public void loadMyApplicationForJob(String jobId, @NonNull DataCallback<ApplicationDto> callback) {
        if (isBlank(jobId)) {
            postSuccess(callback, null);
            return;
        }
        String token = sessionManager.getAuthToken();
        String userId = sessionManager.getUserId();
        if (isBlank(token) || isBlank(userId)) {
            postSuccess(callback, null);
            return;
        }

        api.getMyApplicationForJob("Bearer " + token, jobId, userId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApplicationDto> call, @NonNull Response<ApplicationDto> response) {
                if (response.isSuccessful()) {
                    postSuccess(callback, response.body());
                    return;
                }
                postSuccess(callback, null);
            }

            @Override
            public void onFailure(@NonNull Call<ApplicationDto> call, @NonNull Throwable t) {
                postError(callback, t);
            }
        });
    }

    public void loadRecruiterPostedJobs(@NonNull DataCallback<List<Job>> callback) {
        String token = sessionManager.getAuthToken();
        if (isBlank(token)) {
            postSuccess(callback, List.of());
            return;
        }

        api.getRecruiterPostedJobs("Bearer " + token).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<JobDto>> call, @NonNull Response<List<JobDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Job> jobs = mapJobs(response.body());
                    mergeIntoCache(jobs);
                    postSuccess(callback, jobs);
                    return;
                }
                postSuccess(callback, List.of());
            }

            @Override
            public void onFailure(@NonNull Call<List<JobDto>> call, @NonNull Throwable t) {
                postError(callback, t);
            }
        });
    }

    public void createRecruiterJob(
            String title,
            String company,
            String location,
            String salary,
            String shortDescription,
            String fullDescription,
            @NonNull DataCallback<Job> callback
    ) {
        String token = sessionManager.getAuthToken();
        if (isBlank(token)) {
            postError(callback, new IllegalStateException("Please login first"));
            return;
        }

        RecruiterJobCreateRequestDto request = new RecruiterJobCreateRequestDto(
                title,
                company,
                location,
                salary,
                shortDescription,
                fullDescription,
                "Full Time",
                "On-site",
                null
        );

        api.createRecruiterJob("Bearer " + token, request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JobDto> call, @NonNull Response<JobDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Job created = JobMapper.fromDto(response.body());
                    mergeIntoCache(List.of(created));
                    postSuccess(callback, created);
                    return;
                }
                postError(callback, new IllegalStateException("Could not create job post"));
            }

            @Override
            public void onFailure(@NonNull Call<JobDto> call, @NonNull Throwable t) {
                postError(callback, t);
            }
        });
    }

    private void fallbackDetailById(Job seed, DataCallback<Job> callback) {
        api.getJobById(seed.getId()).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JobDto> call, @NonNull Response<JobDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Job mapped = mergeJobDetails(seed, JobMapper.fromDto(response.body()));
                    if (!isBlank(mapped.getUrl()) && isBlank(mapped.getDescription())) {
                        api.getJobDescription(mapped.getId(), mapped.getUrl()).enqueue(new Callback<>() {
                            @Override
                            public void onResponse(@NonNull Call<JobDto> call, @NonNull Response<JobDto> detailResponse) {
                                if (detailResponse.isSuccessful() && detailResponse.body() != null) {
                                    postSuccess(callback, mergeJobDetails(mapped, JobMapper.fromDto(detailResponse.body())));
                                    return;
                                }
                                postSuccess(callback, mapped);
                            }

                            @Override
                            public void onFailure(@NonNull Call<JobDto> call, @NonNull Throwable t) {
                                postSuccess(callback, mapped);
                            }
                        });
                        return;
                    }
                    postSuccess(callback, mapped);
                    return;
                }
                postSuccess(callback, seed);
            }

            @Override
            public void onFailure(@NonNull Call<JobDto> call, @NonNull Throwable t) {
                postSuccess(callback, seed);
            }
        });
    }

    private Job mergeJobDetails(Job seed, Job updated) {
        if (updated == null) {
            return seed;
        }
        if (isBlank(updated.getCompany())) {
            updated.setCompany(seed.getCompany());
        }
        if (isBlank(updated.getDescription())) {
            updated.setDescription(seed.getDescription());
        }
        if (isBlank(updated.getUrl())) {
            updated.setUrl(seed.getUrl());
        }
        if (isBlank(updated.getLocation())) {
            updated.setLocation(seed.getLocation());
        }
        if (isBlank(updated.getSalary())) {
            updated.setSalary(seed.getSalary());
        }
        updated.setSaved(seed.isSaved());
        return updated;
    }

    public void toggleSave(Job job, boolean wantToSave, @NonNull DataCallback<Boolean> callback) {
        if (job == null || isBlank(job.getId())) {
            postSuccess(callback, false);
            return;
        }

        localSavedStore.setSaved(job.getId(), wantToSave);
        job.setSaved(wantToSave);

        String token = sessionManager.getAuthToken();
        String userId = sessionManager.getUserId();

        if (isBlank(token) || isBlank(userId)) {
            postSuccess(callback, true);
            return;
        }

        SaveJobRequestDto request = new SaveJobRequestDto(userId, job.getId(), wantToSave);
        api.saveJob("Bearer " + token, request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    postSuccess(callback, true);
                    return;
                }
                // Keep local state for offline UX even if remote failed.
                postSuccess(callback, true);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                // Keep local state for offline UX even if remote failed.
                postSuccess(callback, true);
            }
        });
    }

    public void clearSessionData() {
        sessionManager.clear();
        localSavedStore.clear();
    }

    private List<Job> mapJobs(List<JobDto> dtoList) {
        List<Job> jobs = new ArrayList<>();
        if (dtoList == null) {
            return jobs;
        }
        for (JobDto dto : dtoList) {
            Job job = JobMapper.fromDto(dto);
            if (!isBlank(job.getId())) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    private void mergeIntoCache(List<Job>... lists) {
        Map<String, Job> merged = new LinkedHashMap<>();
        for (Job existing : cachedJobs) {
            if (!isBlank(existing.getId())) {
                merged.put(existing.getId(), existing);
            }
        }

        for (List<Job> list : lists) {
            if (list == null) {
                continue;
            }
            for (Job job : list) {
                if (!isBlank(job.getId())) {
                    merged.put(job.getId(), job);
                }
            }
        }

        cachedJobs.clear();
        cachedJobs.addAll(merged.values());
    }

    private void syncSavedFlags(List<Job> jobs) {
        if (jobs == null) {
            return;
        }
        Set<String> savedIds = localSavedStore.getSavedIds();
        for (Job job : jobs) {
            if (job != null && !isBlank(job.getId())) {
                job.setSaved(savedIds.contains(job.getId()));
            }
        }
    }

    private List<Job> applySavedOnly(List<Job> source) {
        Set<String> savedIds = localSavedStore.getSavedIds();
        List<Job> filtered = new ArrayList<>();

        for (Job job : source) {
            if (job == null || isBlank(job.getId())) {
                continue;
            }
            boolean isSaved = savedIds.contains(job.getId()) || job.isSaved();
            if (isSaved) {
                job.setSaved(true);
                filtered.add(job);
            }
        }

        return filtered;
    }

    private List<Job> applyLocalFilter(List<Job> source, String query, String activeFilter) {
        List<Job> filtered = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String filter = activeFilter == null ? "All Jobs" : activeFilter;

        for (Job job : source) {
            if (job == null) {
                continue;
            }
            String haystack = (safe(job.getTitle()) + " " + safe(job.getCompany()) + " " + safe(job.getLocation())).toLowerCase(Locale.ROOT);
            boolean matchesQuery = q.isEmpty() || haystack.contains(q);
            boolean matchesFilter = "All Jobs".equalsIgnoreCase(filter)
                    || safe(job.getJobType()).equalsIgnoreCase(filter)
                    || ("Remote".equalsIgnoreCase(filter) && "Remote".equalsIgnoreCase(job.getWorkMode()))
                    || ("Internship".equalsIgnoreCase(filter) && safe(job.getJobType()).toLowerCase(Locale.ROOT).contains("intern"));

            if (matchesQuery && matchesFilter) {
                filtered.add(job);
            }
        }

        return filtered;
    }

    private List<Job> limit(List<Job> source, int max) {
        if (source.size() <= max) {
            return source;
        }
        return new ArrayList<>(source.subList(0, max));
    }

    private void fallbackHome(DataCallback<HomeData> callback, Throwable throwable) {
        List<Job> featured = SampleData.getFeaturedJobs();
        List<Job> recommended = SampleData.getRecommendedJobs();
        syncSavedFlags(featured);
        syncSavedFlags(recommended);
        mergeIntoCache(featured, recommended);
        postSuccess(callback, new HomeData(featured, recommended));
    }

    private <T> void postSuccess(DataCallback<T> callback, T data) {
        mainHandler.post(() -> callback.onSuccess(data));
    }

    private <T> void postError(DataCallback<T> callback, Throwable throwable) {
        if (throwable == null) {
            return;
        }
        mainHandler.post(() -> callback.onError(throwable));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
