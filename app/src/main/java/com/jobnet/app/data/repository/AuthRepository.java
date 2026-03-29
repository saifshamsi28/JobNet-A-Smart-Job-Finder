package com.jobnet.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jobnet.app.data.network.ApiClient;
import com.jobnet.app.data.network.JobNetApiService;
import com.jobnet.app.data.network.dto.AuthResponseDto;
import com.jobnet.app.data.network.dto.LoginRequestDto;
import com.jobnet.app.data.network.dto.RegisterRequestDto;
import com.jobnet.app.data.network.dto.UserDto;
import com.jobnet.app.data.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    public interface AuthCallback {
        void onSuccess();

        void onError(String message);
    }

    private static volatile AuthRepository instance;

    private final JobNetApiService api;
    private final SessionManager sessionManager;

    private AuthRepository(Context context) {
        this.api = ApiClient.getApiService();
        this.sessionManager = new SessionManager(context);
    }

    public static AuthRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (AuthRepository.class) {
                if (instance == null) {
                    instance = new AuthRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void login(String identifier, String password, @NonNull AuthCallback callback) {
        if (identifier == null || identifier.trim().isEmpty() || password == null || password.isEmpty()) {
            callback.onError("Please enter both email/username and password");
            return;
        }

        api.login(new LoginRequestDto(identifier.trim(), password)).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponseDto> call, @NonNull Response<AuthResponseDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Invalid credentials or server error");
                    return;
                }

                AuthResponseDto auth = response.body();
                String token = auth.message;
                if (token == null || token.isBlank() || auth.status != 200) {
                    callback.onError(auth.message == null ? "Login failed" : auth.message);
                    return;
                }

                sessionManager.saveAuthToken(token);
                fetchAndStoreUserProfile(callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponseDto> call, @NonNull Throwable t) {
                callback.onError("Network error during login");
            }
        });
    }

    public void register(String fullName, String userName, String email, String password, String role, @NonNull AuthCallback callback) {
        if (isBlank(fullName) || isBlank(userName) || isBlank(email) || isBlank(password) || isBlank(role)) {
            callback.onError("All fields are required");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            callback.onError("Please enter a valid email address");
            return;
        }

        if (password.length() < 6) {
            callback.onError("Password should be at least 6 characters");
            return;
        }

        RegisterRequestDto payload = new RegisterRequestDto(
                fullName.trim(),
                userName.trim(),
                email.trim(),
                password,
            "",
            role.trim()
        );

        api.register(payload).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponseDto> call, @NonNull Response<AuthResponseDto> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                    return;
                }
                String message = "Unable to create account";
                if (response.code() == 400) {
                    message = "Email or username already exists";
                }
                callback.onError(message);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponseDto> call, @NonNull Throwable t) {
                callback.onError("Network error during registration");
            }
        });
    }

    public SessionManager session() {
        return sessionManager;
    }

    private void fetchAndStoreUserProfile(@NonNull AuthCallback callback) {
        String token = sessionManager.getAuthToken();
        if (token == null || token.isBlank()) {
            callback.onError("Session token missing");
            return;
        }

        api.getLoggedInUser("Bearer " + token).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UserDto> call, @NonNull Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserDto user = response.body();
                    sessionManager.saveUserId(user.id);
                    sessionManager.saveUserIdentity(user.userName, user.email);
                    if (user.role != null && !user.role.isBlank()) {
                        sessionManager.saveUserRole(user.role);
                    }
                    callback.onSuccess();
                    return;
                }
                // Allow entering app with token even if profile fetch temporarily fails.
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<UserDto> call, @NonNull Throwable t) {
                // Allow entering app with token even if profile fetch temporarily fails.
                callback.onSuccess();
            }
        });
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
