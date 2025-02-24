package com.saif.jobnet.Api;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class SupabaseClient {

    private static final String BASE_URL = "https://ynsrmwwmlwmagvanssnx.supabase.co/";
    public static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inluc3Jtd3dtbHdtYWd2YW5zc254Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDA0MTg1MzQsImV4cCI6MjA1NTk5NDUzNH0.S7456cwW1ecWD83uTKdECbldeGy0rv49FhkdMpdTTNc";  // Replace with your actual API Key

    private static Retrofit retrofit;

    public static SupabaseStorageApi getStorageApi() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request newRequest = chain.request().newBuilder()

                                    .addHeader("apikey", API_KEY)
                                    .addHeader("Authorization", "Bearer " + API_KEY)  // ✅ FIXED AUTH HEADER
                                    .addHeader("Content-Type", "application/octet-stream")  // ✅ Required for file uploads
                                    .build();
                            return chain.proceed(newRequest);
                        }
                    }).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(SupabaseStorageApi.class);
    }
}

