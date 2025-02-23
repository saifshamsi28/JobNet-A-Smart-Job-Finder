package com.saif.jobnet.Api;

import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.JobUpdateDTO;
import com.saif.jobnet.Models.SaveJobsModel;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Models.UserLoginCredentials;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
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

    @POST("/user")
    Call<User> registerUser(@Body User user);

    @GET("/user/id/{id}")
    Call<User> getUserById(@Path("id") String id);

    @POST("/user/login")
    Call<User> loginUser(@Body UserLoginCredentials credentials);

    @POST("user/username/{username}")
    Call<Boolean> checkUserName(@Path("username") String username);

    @POST("user/email/{email}")
    Call<Boolean> checkEmailAlreadyExist(@Path("email") String email);

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

}
