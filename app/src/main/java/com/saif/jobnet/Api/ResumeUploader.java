package com.saif.jobnet.Api;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ResumeUploader {

    private static final String TAG = "ResumeUploader";
    private static final String BUCKET_NAME = "resumes"; // Change to your bucket name
    private static final String AUTH_TOKEN = "Bearer your-supabase-auth-token";

    public static void uploadResume(Context context, Uri fileUri) {
        File file = convertUriToFile(context, fileUri);

        if (file == null) {
            Log.e(TAG, "Failed to get file from Uri");
            return;
        }

        // Create request body
        RequestBody requestFile = RequestBody.create(MediaType.parse("application/pdf"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        // Generate file path in Supabase Storage
        String filePath = "user-resumes/" + file.getName();  // Modify as needed

        // Get Retrofit client
        SupabaseStorageApi api = RetrofitClient.getClient().create(SupabaseStorageApi.class);
        Call<ResponseBody> call = api.uploadResume(BUCKET_NAME, filePath, AUTH_TOKEN, body);

        // Execute the request
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Resume uploaded successfully: " + filePath);
                } else {
                    Log.e(TAG, "Upload failed: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Upload error: " + t.getMessage());
            }
        });
    }

    private static File convertUriToFile(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("resume", ".pdf", context.getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();
            return tempFile;
        } catch (IOException e) {
            Log.e("ResumeUploader", "File conversion error: " + e.getMessage());
            return null;
        }
    }
}
