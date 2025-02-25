package com.saif.jobnet.Api;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface SupabaseStorageApi {

    @Multipart
    @POST("storage/v1/object/{bucketName}/{filePath}")
    Call<ResponseBody> uploadResume(
            @Path("bucketName") String bucketName,
            @Path("filePath") String filePath,
            @Header("Authorization") String authToken,
            @Part MultipartBody.Part file
    );

    @GET("storage/v1/object/public/resumes/{fileName}")
    Call<ResponseBody> getResumeUrl(@Path("fileName") String fileName);
}


//https://ynsrmwwmlwmagvanssnx.supabase.co/storage/v1/object/public/resumes//saif_resume.pdf