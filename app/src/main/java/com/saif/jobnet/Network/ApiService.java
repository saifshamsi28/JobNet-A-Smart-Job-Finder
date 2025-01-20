package com.saif.jobnet.Network;

import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.SaveJobsModel;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Models.UserLoginCredentials;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.List;

public interface ApiService {
    @GET("home")
    Call<List<Job>> showJobs();

    @GET("jobs")
    Call<List<Job>> searchJobs(@Query("job_title") String jobTitle);

    @GET("home/jobs/description/{id}")
    Call<Job> getJobDescription(@Path("id") String jobId, @Query("url") String flaskUrl);

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
    Call<User> saveJobs(@Body SaveJobsModel saveJobsModel);

}
