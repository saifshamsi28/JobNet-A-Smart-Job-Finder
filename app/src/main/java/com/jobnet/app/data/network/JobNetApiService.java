package com.jobnet.app.data.network;

import com.jobnet.app.data.network.dto.JobDto;
import com.jobnet.app.data.network.dto.LoginRequestDto;
import com.jobnet.app.data.network.dto.RegisterRequestDto;
import com.jobnet.app.data.network.dto.SaveJobRequestDto;
import com.jobnet.app.data.network.dto.UserDto;
import com.jobnet.app.data.network.dto.AuthResponseDto;
import com.jobnet.app.data.network.dto.ApplicationDto;
import com.jobnet.app.data.network.dto.ApplicationStatusRequestDto;
import com.jobnet.app.data.network.dto.ApplyJobRequestDto;
import com.jobnet.app.data.network.dto.RecruiterJobCreateRequestDto;
import com.jobnet.app.data.network.dto.RecruiterJobStatusRequestDto;
import com.jobnet.app.data.network.dto.RefreshTokenRequestDto;
import com.jobnet.app.data.network.dto.UserUpdateRequestDto;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface JobNetApiService {

        @POST("auth/login")
        Call<AuthResponseDto> login(@Body LoginRequestDto request);

        @POST("auth/refresh")
        Call<AuthResponseDto> refreshToken(@Body RefreshTokenRequestDto request);

        @POST("auth/register")
        Call<AuthResponseDto> register(@Body RegisterRequestDto request);

        @POST("user/username/{username}")
        Call<Boolean> checkUsername(@Path("username") String username);

        @GET("user/email")
        Call<Boolean> checkEmail(@Query("email") String email);

    @GET("home/suggested-jobs")
    Call<List<JobDto>> getSuggestedJobs();

    @GET("home/recent-jobs")
    Call<List<JobDto>> getRecentJobs();

    @GET("home/jobs")
    Call<List<JobDto>> searchJobs(
            @Query("title") String title,
            @Query("location") String location,
            @Query("company") String company,
            @Query("minSalary") Integer minSalary,
            @Query("jobType") String jobType
    );

    @GET("home/jobs/description/{id}")
    Call<JobDto> getJobDescription(
            @Path("id") String id,
            @Query("url") String url
    );

    @GET("home/id/{id}")
    Call<JobDto> getJobById(@Path("id") String id);

    @GET("auth/me")
    Call<UserDto> getLoggedInUser(@Header("Authorization") String authHeader);

    @PUT("user/save-jobs")
    Call<ResponseBody> saveJob(
            @Header("Authorization") String authHeader,
            @Body SaveJobRequestDto request
    );

    @PATCH("user/id/{id}/update-skills")
    Call<ResponseBody> updateSkills(
            @Header("Authorization") String authHeader,
            @Path("id") String userId,
            @Body List<String> skills
    );

    @PUT("user/update")
    Call<UserDto> updateUser(
            @Header("Authorization") String authHeader,
            @Body UserUpdateRequestDto request
    );

    @POST("applications/apply")
    Call<ApplicationDto> applyToJob(
            @Header("Authorization") String authHeader,
            @Body ApplyJobRequestDto request
    );

    @GET("applications/me")
    Call<List<ApplicationDto>> getMyApplications(
            @Header("Authorization") String authHeader,
            @Query("userId") String userId
    );

    @GET("applications/me/job/{jobId}")
    Call<ApplicationDto> getMyApplicationForJob(
            @Header("Authorization") String authHeader,
            @Path("jobId") String jobId,
            @Query("userId") String userId
    );

    @POST("recruiter/jobs")
    Call<JobDto> createRecruiterJob(
            @Header("Authorization") String authHeader,
            @Body RecruiterJobCreateRequestDto request
    );

    @GET("recruiter/jobs/me")
    Call<List<JobDto>> getRecruiterPostedJobs(
            @Header("Authorization") String authHeader
    );

    @GET("recruiter/jobs/{jobId}/applications")
    Call<List<ApplicationDto>> getJobApplicants(
            @Header("Authorization") String authHeader,
            @Path("jobId") String jobId
    );

    @PUT("recruiter/jobs/{jobId}")
    Call<JobDto> updateRecruiterJob(
            @Header("Authorization") String authHeader,
            @Path("jobId") String jobId,
            @Body RecruiterJobCreateRequestDto request
    );

    @PATCH("recruiter/jobs/{jobId}/status")
    Call<JobDto> updateRecruiterJobStatus(
            @Header("Authorization") String authHeader,
            @Path("jobId") String jobId,
            @Body RecruiterJobStatusRequestDto request
    );

    @DELETE("recruiter/jobs/{jobId}")
    Call<ResponseBody> deleteRecruiterJob(
            @Header("Authorization") String authHeader,
            @Path("jobId") String jobId
    );

    @PATCH("applications/{id}/status")
    Call<ApplicationDto> updateApplicationStatus(
            @Header("Authorization") String authHeader,
            @Path("id") String applicationId,
            @Body ApplicationStatusRequestDto request
    );
}
