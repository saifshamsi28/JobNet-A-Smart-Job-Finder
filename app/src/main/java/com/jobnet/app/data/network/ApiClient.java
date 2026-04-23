package com.jobnet.app.data.network;

import com.jobnet.app.BuildConfig;
import com.google.gson.Gson;
import com.jobnet.app.data.network.dto.AuthResponseDto;
import com.jobnet.app.data.network.dto.RefreshTokenRequestDto;
import com.jobnet.app.data.session.SessionManager;

import android.content.Context;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static volatile JobNetApiService apiService;
    private static volatile Context appContext;
    private static final Object refreshLock = new Object();
    private static final Gson gson = new Gson();

    private ApiClient() {
    }

    public static void initialize(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static JobNetApiService getApiService() {
        if (apiService == null) {
            synchronized (ApiClient.class) {
                if (apiService == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)
                            .writeTimeout(20, TimeUnit.SECONDS)
                            .addInterceptor(chain -> {
                                Request request = chain.request();
                                Response response = chain.proceed(request);

                                if (response.code() != 401 || appContext == null || isAuthEndpoint(request.url().encodedPath())) {
                                    return response;
                                }

                                String originalAuth = request.header("Authorization");
                                if (originalAuth == null || !originalAuth.startsWith("Bearer ")) {
                                    return response;
                                }

                                SessionManager session = new SessionManager(appContext);
                                synchronized (refreshLock) {
                                    String latestToken = session.getAuthToken();
                                    String latestAuth = latestToken == null || latestToken.isBlank() ? null : "Bearer " + latestToken;

                                    if (latestAuth != null && !latestAuth.equals(originalAuth)) {
                                        response.close();
                                        Request retryWithLatest = request.newBuilder()
                                                .header("Authorization", latestAuth)
                                                .build();
                                        return chain.proceed(retryWithLatest);
                                    }

                                    AuthResponseDto refreshed = refreshAccessToken(session.getRefreshToken());
                                    if (refreshed == null || refreshed.accessToken == null || refreshed.accessToken.isBlank()) {
                                        session.clear();
                                        return response;
                                    }

                                    session.saveAuthToken(refreshed.accessToken);
                                    if (refreshed.refreshToken != null && !refreshed.refreshToken.isBlank()) {
                                        session.saveRefreshToken(refreshed.refreshToken);
                                    }

                                    response.close();
                                    Request retry = request.newBuilder()
                                            .header("Authorization", "Bearer " + refreshed.accessToken)
                                            .build();
                                    return chain.proceed(retry);
                                }
                            })
                            .addInterceptor(logging)
                            .build();

                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BuildConfig.BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(client)
                            .build();

                    apiService = retrofit.create(JobNetApiService.class);
                }
            }
        }
        return apiService;
    }

    private static boolean isAuthEndpoint(String path) {
        if (path == null) {
            return false;
        }
        String p = path.toLowerCase();
        return p.contains("/auth/login") || p.contains("/auth/register") || p.contains("/auth/refresh");
    }

    private static AuthResponseDto refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        try {
            OkHttpClient refreshClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .build();

            String bodyJson = gson.toJson(new RefreshTokenRequestDto(refreshToken));
            Request refreshRequest = new Request.Builder()
                    .url(BuildConfig.BASE_URL + "auth/refresh")
                    .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
                    .build();

            try (Response refreshResponse = refreshClient.newCall(refreshRequest).execute()) {
                if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) {
                    return null;
                }
                String raw = refreshResponse.body().string();
                if (raw == null || raw.isBlank()) {
                    return null;
                }
                return gson.fromJson(raw, AuthResponseDto.class);
            }
        } catch (IOException e) {
            return null;
        }
    }
}
