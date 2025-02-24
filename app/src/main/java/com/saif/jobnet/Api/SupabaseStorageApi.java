package com.saif.jobnet.Api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface SupabaseStorageApi {

    @Multipart
    @PUT("storage/v1/object/{filePath}")  // ✅ Change POST to PUT
    Call<ResponseBody> uploadResume(
            @Path("filePath") String filePath,  // ✅ Ensure correct path formatting
            @Part MultipartBody.Part file,
            @Header("Authorization") String authToken
    );

    @GET("storage/v1/object/public/resumes/{fileName}")
    Call<ResponseBody> getResumeUrl(@Path("fileName") String fileName);
}


//https://ynsrmwwmlwmagvanssnx.supabase.co/storage/v1/object/public/resumes//saif_resume.pdf