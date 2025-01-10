package com.saif.jobnet.Network;

import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Models.UserLoginCredentials;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import java.util.List;

public interface ApiService {
    @GET("home")
    Call<List<Job>> showJobs(@Query("job_title") String jobTitle);

    @GET("jobs")
    Call<List<Job>> searchJobs(@Query("job_title") String jobTitle);

    @GET("/getJobDescription")
    Call<Job> getJobDescription(@Query("job_url") String jobUrl);

    @POST("/user")
    Call<User> registerUser(@Body User user);

    @POST("/user/login")
    Call<User> loginUser(@Body UserLoginCredentials credentials);



}
