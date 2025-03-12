package com.saif.jobnet.Api;

import com.saif.jobnet.Models.AuthResponse;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.JobUpdateDTO;
import com.saif.jobnet.Models.SaveJobsModel;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Models.UserLoginCredentials;
import com.saif.jobnet.Models.UserUpdateDTO;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.List;

public interface ApiService {
    @GET("home")
    Call<List<Job>> showJobs();

    @GET("jobs")
    Call<List<Job>> searchJobs(@Query("job_title") String stringTitle);

    @GET("home/jobs/description/{id}")
    Call<Job> getJobDescription(@Path("id") String jobId, @Query("url") String jobUrl);

    @GET("url")
    Call<Job> getJobDescriptionFromFlask(@Query("url") String url);

    @POST("/auth/register")
    Call<AuthResponse> registerUser(@Body User user);

    @POST("/auth/login")
    Call<User> loginUser(@Body UserLoginCredentials credentials);

    @POST("user/username/{username}")
    Call<Boolean> checkUserName(@Path("username") String username);

    @GET("user/email")
    Call<Boolean> checkEmailAlreadyExist(@Query("email") String email);

    @PUT("user/update")
    Call<User> updateUser(@Body UserUpdateDTO user);

    @PATCH("user/id/{id}/update-basic-details")
    Call<ResponseBody> updateBasicDetails(@Path("id") String id, @Body User user);

    @Multipart
    @POST("user/{id}/upload-profile-image")
    Call<ResponseBody> uploadProfileImage(@Path("id") String id, @Part MultipartBody.Part file);

    @Multipart
    @POST("user/{id}/upload-profile-chunk")
    Call<ResponseBody> uploadProfileImageChunk(
            @Path("id") String userId,
            @Part MultipartBody.Part file,
            @Part("chunkIndex") RequestBody chunkIndex,
            @Part("totalChunks") RequestBody totalChunks
    );

    @GET("user/{id}/profile")
    Call<User> getUserProfile(@Path("id") String id);

    @PUT("user/save-jobs")
    Call<ResponseBody> saveJobs(@Body SaveJobsModel saveJobsModel);

    //get job by title and other fields
    @GET("home/jobs")
    Call<List<Job>> fetchJobsByTitle(@Query("title") String title,
                                     @Query("location") String location,
                                     @Query("company") String company,
                                     @Query("minSalary") Integer minSalary,
                                     @Query("jobType") String jobType);

    @PATCH("home/job/{id}/update-description")
    Call<Void> updateJobDescription(@Path("id") String id, @Body JobUpdateDTO jobUpdateDTO);

    @Multipart
    @POST("/user/resume/upload-chunk")
    Call<ResponseBody> uploadResumeChunk(
            @Part("userId") RequestBody userId,
            @Part("resumeName") RequestBody resumeName,
            @Part("chunkIndex") RequestBody chunkIndex,
            @Part("totalChunks") RequestBody totalChunks,
            @Part MultipartBody.Part file
    );

    @FormUrlEncoded
    @POST("user/resume/finalize-upload")
    Call<ResponseBody> finalizeUpload(
            @Field("userId") String userId,
            @Field("resumeName") String resumeName,
            @Field("resumeDate") String resumeDate,
            @Field("resumeSize") String resumeSize,
            @Field("totalChunks") Integer totalChunks
    );
}
