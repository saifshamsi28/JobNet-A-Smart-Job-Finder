package com.jobnet.app.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.network.ApiClient;
import com.jobnet.app.data.network.JobNetApiService;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.network.dto.ApplicationStatusRequestDto;
import com.jobnet.app.data.network.dto.ApplyJobRequestDto;
import com.jobnet.app.data.network.dto.JobDto;
import com.jobnet.app.data.network.dto.RecruiterJobCreateRequestDto;
import com.jobnet.app.data.network.dto.RecruiterJobStatusRequestDto;
import com.jobnet.app.data.network.dto.SaveJobRequestDto;
import com.jobnet.app.data.network.dto.UserDto;
import com.jobnet.app.data.network.dto.UserUpdateRequestDto;
import com.jobnet.app.data.session.SavedJobsLocalStore;
import com.jobnet.app.data.session.SessionManager;
import com.jobnet.app.util.SampleData;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.io.IOException;

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
    private static final long HOME_CACHE_TTL_MS = 30_000L;
    private static final long PROFILE_CACHE_TTL_MS = 20_000L;
    private final Object homeLock = new Object();
    private final List<DataCallback<HomeData>> homeCallbacks = new ArrayList<>();
    private boolean homeRequestInFlight;
    private HomeData cachedHomeData;
    private long cachedHomeDataAt;
    private final Object profileLock = new Object();
    private final List<DataCallback<UserDto>> profileCallbacks = new ArrayList<>();
    private boolean profileRequestInFlight;
    private UserDto cachedProfile;
    private long cachedProfileAt;

    private JobNetRepository(Context context) {
        ApiClient.initialize(context.getApplicationContext());
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
        HomeData freshHome = getFreshCachedHomeData();
        if (freshHome != null) {
            postSuccess(callback, freshHome);
            return;
        }

        synchronized (homeLock) {
            homeCallbacks.add(callback);
            if (homeRequestInFlight) {
                return;
            }
            homeRequestInFlight = true;
        }

        api.getSuggestedJobs().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<JobDto>> call, @NonNull Response<List<JobDto>> suggestedResp) {
                api.getRecentJobs().enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<List<JobDto>> call, @NonNull Response<List<JobDto>> recentResp) {
                        if (!suggestedResp.isSuccessful() && !recentResp.isSuccessful()) {
                            finishHomeRequest(fallbackHomeData(), null);
                            return;
                        }

                        List<Job> suggested = mapJobs(suggestedResp.body());
                        List<Job> recent = mapJobs(recentResp.body());

                        suggested = filterSeekerVisibleJobs(suggested);
                        recent = filterSeekerVisibleJobs(recent);

                        syncSavedFlags(suggested);
                        syncSavedFlags(recent);

                        mergeIntoCache(suggested, recent);

                        if (suggested.isEmpty() && recent.isEmpty()) {
                            finishHomeRequest(fallbackHomeData(), null);
                            return;
                        }

                        finishHomeRequest(new HomeData(limit(suggested, 8), limit(recent, 20)), null);
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<JobDto>> call, @NonNull Throwable t) {
                        finishHomeRequest(fallbackHomeData(), null);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<List<JobDto>> call, @NonNull Throwable t) {
                finishHomeRequest(fallbackHomeData(), null);
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
                            List<Job> jobs = filterSeekerVisibleJobs(mapJobs(response.body()));
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
        UserDto freshProfile = getFreshCachedProfile();
        if (freshProfile != null) {
            postSuccess(callback, freshProfile);
            return;
        }

        String token = sessionManager.getAuthToken();
        if (token == null || token.isBlank()) {
            postSuccess(callback, null);
            return;
        }

        synchronized (profileLock) {
            profileCallbacks.add(callback);
            if (profileRequestInFlight) {
                return;
            }
            profileRequestInFlight = true;
        }

        api.getLoggedInUser("Bearer " + token).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UserDto> call, @NonNull Response<UserDto> response) {
                if (response.isSuccessful()) {
                    finishProfileRequest(response.body(), null);
                } else {
                    finishProfileRequest(null, null);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserDto> call, @NonNull Throwable t) {
                finishProfileRequest(null, t);
            }
        });
    }

    public void updateProfile(
            String name,
            String email,
            String phoneNumber,
            @NonNull DataCallback<UserDto> callback
    ) {
        String token = sessionManager.getAuthToken();
        String userId = sessionManager.getUserId();
        if (isBlank(token) || isBlank(userId)) {
            postError(callback, new IllegalStateException("Please login first"));
            return;
        }

        UserUpdateRequestDto request = new UserUpdateRequestDto(
                userId,
                trimToNull(name),
                trimToNull(email),
                null,
                trimToNull(phoneNumber)
        );

        api.updateUser("Bearer " + token, request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UserDto> call, @NonNull Response<UserDto> response) {
                if (response.isSuccessful()) {
                    UserDto body = response.body();
                    if (body == null) {
                        body = new UserDto();
                        body.id = userId;
                        body.name = trimToNull(name);
                        body.email = trimToNull(email);
                        body.phoneNumber = trimToNull(phoneNumber);
                    }

                    cacheProfile(body);
                    sessionManager.saveUserIdentity(
                            body.name != null ? body.name : trimToNull(name),
                            body.email != null ? body.email : trimToNull(email)
                    );
                    postSuccess(callback, body);
                    return;
                }
                postError(callback, new IllegalStateException(extractApiErrorMessage(response, "Could not update profile")));
            }

            @Override
            public void onFailure(@NonNull Call<UserDto> call, @NonNull Throwable t) {
                postError(callback, t);
            }
        });
    }

    public void updateSkills(List<String> skills, @NonNull DataCallback<Boolean> callback) {
        String token = sessionManager.getAuthToken();
        String userId = sessionManager.getUserId();
        if (isBlank(token) || isBlank(userId)) {
            postError(callback, new IllegalStateException("Please login first"));
            return;
        }

        List<String> sanitizedSkills = new ArrayList<>();
        if (skills != null) {
            for (String skill : skills) {
                String clean = trimToNull(skill);
                if (clean != null && !sanitizedSkills.contains(clean)) {
                    sanitizedSkills.add(clean);
                }
            }
        }

        api.updateSkills("Bearer " + token, userId, sanitizedSkills).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    synchronized (profileLock) {
                        if (cachedProfile != null) {
                            cachedProfile.skills = new ArrayList<>(sanitizedSkills);
                            cachedProfileAt = System.currentTimeMillis();
                        }
                    }
                    postSuccess(callback, true);
                    return;
                }
                postError(callback, new IllegalStateException(extractApiErrorMessage(response, "Could not update skills")));
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
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
            String openings,
            String employmentType,
            String workMode,
            String shortDescription,
            String fullDescription,
            @NonNull DataCallback<Job> callback
    ) {
            createRecruiterJob(
                title,
                company,
                location,
                salary,
                openings,
                employmentType,
                workMode,
                null,
                List.of(),
                shortDescription,
                fullDescription,
                "PUBLISHED",
                callback
            );
    }

    public void createRecruiterJob(
            String title,
            String company,
            String location,
            String salary,
            String openings,
            String shortDescription,
            String fullDescription,
            @NonNull DataCallback<Job> callback
    ) {
            createRecruiterJob(
                title,
                company,
                location,
                salary,
                openings,
                "Full Time",
                "On-site",
                null,
                List.of(),
                shortDescription,
                fullDescription,
                "PUBLISHED",
                callback
            );
    }

    public void createRecruiterJob(
            String title,
            String company,
            String location,
            String salary,
            String openings,
            String employmentType,
            String workMode,
            String category,
            List<String> requiredSkills,
            String shortDescription,
            String fullDescription,
            String status,
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
                openings,
                shortDescription,
                fullDescription,
                employmentType,
                employmentType,
                workMode,
                category,
                requiredSkills,
                null,
                status
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
                String fallback = response.code() == 401
                        ? "Your session has expired. Please login again."
                        : "Could not create job post";
                postError(callback, new IllegalStateException(extractApiErrorMessage(response, fallback)));
            }

            @Override
            public void onFailure(@NonNull Call<JobDto> call, @NonNull Throwable t) {
                String message = (t instanceof IOException)
                        ? "Network error. Please check your internet and try again."
                        : "Could not connect to server. Please try again.";
                postError(callback, new IllegalStateException(message));
            }
        });
    }

    public void loadJobApplicants(String jobId, @NonNull DataCallback<List<ApplicationDto>> callback) {
        String token = sessionManager.getAuthToken();
        if (isBlank(token) || isBlank(jobId)) {
            postSuccess(callback, List.of());
            return;
        }

        api.getJobApplicants("Bearer " + token, jobId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<ApplicationDto>> call, @NonNull Response<List<ApplicationDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                    return;
                }
                postError(callback, new IllegalStateException(extractApiErrorMessage(response, "Could not load applicants")));
            }

            @Override
            public void onFailure(@NonNull Call<List<ApplicationDto>> call, @NonNull Throwable t) {
                postError(callback, t);
            }
        });
    }

    public void updateRecruiterJob(
            String jobId,
            String title,
            String company,
            String location,
            String salary,
            String openings,
            String shortDescription,
            String fullDescription,
            String employmentType,
            String workMode,
            @NonNull DataCallback<Job> callback
    ) {
        updateRecruiterJob(
                jobId,
                title,
                company,
                location,
                salary,
                openings,
                shortDescription,
                fullDescription,
                employmentType,
                workMode,
                null,
                List.of(),
                callback
        );
    }

    public void updateRecruiterJob(
            String jobId,
            String title,
            String company,
            String location,
            String salary,
            String openings,
            String shortDescription,
            String fullDescription,
            String employmentType,
            String workMode,
            String category,
            List<String> requiredSkills,
            @NonNull DataCallback<Job> callback
    ) {
        String token = sessionManager.getAuthToken();
        if (isBlank(token) || isBlank(jobId)) {
            postError(callback, new IllegalStateException("Please login first"));
            return;
        }

        RecruiterJobCreateRequestDto request = new RecruiterJobCreateRequestDto(
                title,
                company,
                location,
                salary,
                openings,
                shortDescription,
                fullDescription,
                employmentType,
                employmentType,
                workMode,
                category,
                requiredSkills,
                null
        );

        api.updateRecruiterJob("Bearer " + token, jobId, request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JobDto> call, @NonNull Response<JobDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Job updated = JobMapper.fromDto(response.body());
                    mergeIntoCache(List.of(updated));
                    postSuccess(callback, updated);
                    return;
                }
                postError(callback, new IllegalStateException(extractApiErrorMessage(response, "Failed to update job")));
            }

            @Override
            public void onFailure(@NonNull Call<JobDto> call, @NonNull Throwable t) {
                postError(callback, t);
            }
        });
    }

    public void updateJobStatus(String jobId, String status, @NonNull DataCallback<Job> callback) {
        String token = sessionManager.getAuthToken();
        if (isBlank(token) || isBlank(jobId) || isBlank(status)) {
            postError(callback, new IllegalStateException("Invalid request"));
            return;
        }

        api.updateRecruiterJobStatus(
                "Bearer " + token,
                jobId,
                new RecruiterJobStatusRequestDto(status)
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JobDto> call, @NonNull Response<JobDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Job updated = JobMapper.fromDto(response.body());
                    mergeIntoCache(List.of(updated));
                    postSuccess(callback, updated);
                    return;
                }
                postError(callback, new IllegalStateException(extractApiErrorMessage(response, "Failed to update job status")));
            }

            @Override
            public void onFailure(@NonNull Call<JobDto> call, @NonNull Throwable t) {
                postError(callback, t);
            }
        });
    }

    public void deleteRecruiterJob(String jobId, @NonNull DataCallback<Boolean> callback) {
        String token = sessionManager.getAuthToken();
        if (isBlank(token) || isBlank(jobId)) {
            postError(callback, new IllegalStateException("Please login first"));
            return;
        }

        api.deleteRecruiterJob("Bearer " + token, jobId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    removeFromCacheById(jobId);
                    postSuccess(callback, true);
                    return;
                }
                String fallback = response.code() == 404
                        ? "Job not found"
                        : "Failed to delete job";
                postError(callback, new IllegalStateException(extractApiErrorMessage(response, fallback)));
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                postError(callback, t);
            }
        });
    }

    public void updateApplicationStatus(
            String applicationId,
            String status,
            @NonNull DataCallback<ApplicationDto> callback
    ) {
        String token = sessionManager.getAuthToken();
        if (isBlank(token) || isBlank(applicationId) || isBlank(status)) {
            postError(callback, new IllegalStateException("Invalid request"));
            return;
        }

        api.updateApplicationStatus(
                "Bearer " + token,
                applicationId,
                new ApplicationStatusRequestDto(status)
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApplicationDto> call, @NonNull Response<ApplicationDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                    return;
                }
                postError(callback, new IllegalStateException(extractApiErrorMessage(response, "Failed to update applicant status")));
            }

            @Override
            public void onFailure(@NonNull Call<ApplicationDto> call, @NonNull Throwable t) {
                postError(callback, t);
            }
        });
    }

    public void withdrawApplication(String applicationId, @NonNull DataCallback<ApplicationDto> callback) {
        updateApplicationStatus(applicationId, "WITHDRAWN", callback);
    }

    private String extractApiErrorMessage(Response<?> response, String fallback) {
        try {
            if (response.errorBody() == null) {
                return fallback;
            }
            String raw = response.errorBody().string();
            if (raw == null || raw.isBlank()) {
                return fallback;
            }

            try {
                com.jobnet.app.data.network.dto.AuthResponseDto authError = new Gson().fromJson(raw, com.jobnet.app.data.network.dto.AuthResponseDto.class);
                if (authError != null && authError.message != null && !authError.message.isBlank()) {
                    return authError.message;
                }
            } catch (Exception ignored) {
                // Continue to generic parsing.
            }

            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            if (obj.has("message") && !obj.get("message").isJsonNull()) {
                String message = obj.get("message").getAsString();
                if (!message.isBlank()) {
                    return message;
                }
            }
            if (obj.has("error") && !obj.get("error").isJsonNull()) {
                String error = obj.get("error").getAsString();
                if (!error.isBlank()) {
                    return error;
                }
            }
        } catch (Exception ignored) {
            // fallback below
        }
        return fallback;
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

    @SafeVarargs
    private final void mergeIntoCache(List<Job>... lists) {
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

    private void removeFromCacheById(String jobId) {
        if (isBlank(jobId)) {
            return;
        }
        for (int i = cachedJobs.size() - 1; i >= 0; i--) {
            Job job = cachedJobs.get(i);
            if (job != null && jobId.equals(job.getId())) {
                cachedJobs.remove(i);
            }
        }
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

    private List<Job> filterSeekerVisibleJobs(List<Job> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<Job> filtered = new ArrayList<>();
        for (Job job : source) {
            String status = safe(job == null ? null : job.getStatus()).toUpperCase(Locale.ROOT);
            if (!"PUBLISHED".equals(status)) {
                continue;
            }
            filtered.add(job);
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

    private HomeData fallbackHomeData() {
        List<Job> featured = SampleData.getFeaturedJobs();
        List<Job> recommended = SampleData.getRecommendedJobs();
        syncSavedFlags(featured);
        syncSavedFlags(recommended);
        mergeIntoCache(featured, recommended);
        return new HomeData(featured, recommended);
    }

    private void finishHomeRequest(HomeData result, Throwable throwable) {
        List<DataCallback<HomeData>> callbacks;
        synchronized (homeLock) {
            homeRequestInFlight = false;
            if (result != null) {
                cachedHomeData = result;
                cachedHomeDataAt = System.currentTimeMillis();
            }
            callbacks = new ArrayList<>(homeCallbacks);
            homeCallbacks.clear();
        }

        if (throwable != null) {
            for (DataCallback<HomeData> cb : callbacks) {
                postError(cb, throwable);
            }
        }
        for (DataCallback<HomeData> cb : callbacks) {
            postSuccess(cb, result);
        }
    }

    private HomeData getFreshCachedHomeData() {
        synchronized (homeLock) {
            if (cachedHomeData == null) {
                return null;
            }
            long ageMs = System.currentTimeMillis() - cachedHomeDataAt;
            if (ageMs <= HOME_CACHE_TTL_MS) {
                return cachedHomeData;
            }
            return null;
        }
    }

    private void finishProfileRequest(UserDto result, Throwable throwable) {
        List<DataCallback<UserDto>> callbacks;
        synchronized (profileLock) {
            profileRequestInFlight = false;
            if (result != null) {
                cachedProfile = result;
                cachedProfileAt = System.currentTimeMillis();
            }
            callbacks = new ArrayList<>(profileCallbacks);
            profileCallbacks.clear();
        }

        if (throwable != null) {
            for (DataCallback<UserDto> cb : callbacks) {
                postError(cb, throwable);
            }
            return;
        }

        for (DataCallback<UserDto> cb : callbacks) {
            postSuccess(cb, result);
        }
    }

    private UserDto getFreshCachedProfile() {
        synchronized (profileLock) {
            if (cachedProfile == null) {
                return null;
            }
            long ageMs = System.currentTimeMillis() - cachedProfileAt;
            if (ageMs <= PROFILE_CACHE_TTL_MS) {
                return cachedProfile;
            }
            return null;
        }
    }

    private void cacheProfile(UserDto profile) {
        if (profile == null) {
            return;
        }
        synchronized (profileLock) {
            cachedProfile = profile;
            cachedProfileAt = System.currentTimeMillis();
        }
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
