package com.jobnet.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
        ApiClient.initialize(context.getApplicationContext());
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
                    callback.onError(extractErrorMessage(response, loginFallbackMessage(response.code())));
                    return;
                }

                AuthResponseDto auth = response.body();
                String accessToken = auth.accessToken;
                String refreshToken = auth.refreshToken;

                if ((accessToken == null || accessToken.isBlank()) && auth.message != null && !auth.message.isBlank()) {
                    // Backward compatibility if backend still sends token in message.
                    accessToken = auth.message;
                }

                if (accessToken == null || accessToken.isBlank() || auth.status != 200) {
                    callback.onError(auth.message == null ? "Login failed" : auth.message);
                    return;
                }

                sessionManager.saveAuthToken(accessToken);
                if (refreshToken != null && !refreshToken.isBlank()) {
                    sessionManager.saveRefreshToken(refreshToken);
                }
                fetchAndStoreUserProfile(callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponseDto> call, @NonNull Throwable t) {
                callback.onError("Could not connect to server. Check your internet and try again.");
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

        String normalizedUserName = userName.trim();
        String normalizedEmail = email.trim().toLowerCase();

        api.checkUsername(normalizedUserName).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Boolean> call, @NonNull Response<Boolean> response) {
                if (response.isSuccessful() && Boolean.TRUE.equals(response.body())) {
                    callback.onError("This username is already taken");
                    return;
                }
                checkEmailAndRegister(fullName, normalizedUserName, normalizedEmail, password, role, callback);
            }

            @Override
            public void onFailure(@NonNull Call<Boolean> call, @NonNull Throwable t) {
                // Proceed to email check; backend register still performs final conflict validation.
                checkEmailAndRegister(fullName, normalizedUserName, normalizedEmail, password, role, callback);
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

    private void checkEmailAndRegister(String fullName,
                                       String userName,
                                       String email,
                                       String password,
                                       String role,
                                       @NonNull AuthCallback callback) {
        api.checkEmail(email).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Boolean> call, @NonNull Response<Boolean> response) {
                if (response.isSuccessful() && Boolean.FALSE.equals(response.body())) {
                    callback.onError("An account with this email already exists");
                    return;
                }
                performRegister(fullName, userName, email, password, role, callback);
            }

            @Override
            public void onFailure(@NonNull Call<Boolean> call, @NonNull Throwable t) {
                // Proceed to register and let server-side checks be source of truth.
                performRegister(fullName, userName, email, password, role, callback);
            }
        });
    }

    private void performRegister(String fullName,
                                 String userName,
                                 String email,
                                 String password,
                                 String role,
                                 @NonNull AuthCallback callback) {
        RegisterRequestDto payload = new RegisterRequestDto(
                fullName.trim(),
                userName.trim(),
                email.trim().toLowerCase(),
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
                callback.onError(extractErrorMessage(response, registerFallbackMessage(response.code())));
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponseDto> call, @NonNull Throwable t) {
                callback.onError("Could not connect to server. Check your internet and try again.");
            }
        });
    }

    private String extractErrorMessage(Response<?> response, String fallback) {
        try {
            if (response.errorBody() == null) {
                return fallback;
            }
            String raw = response.errorBody().string();
            if (raw == null || raw.isBlank()) {
                return fallback;
            }

            try {
                AuthResponseDto authError = new Gson().fromJson(raw, AuthResponseDto.class);
                if (authError != null && authError.message != null && !authError.message.isBlank()) {
                    return authError.message;
                }
            } catch (Exception ignored) {
                // Fall through to generic JSON parsing.
            }

            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            if (obj.has("message") && !obj.get("message").isJsonNull()) {
                String message = obj.get("message").getAsString();
                if (!message.isBlank()) {
                    return message;
                }
            }
            if (obj.has("error") && !obj.get("error").isJsonNull()) {
                String error = obj.get("error").getAsString();
                if (!error.isBlank()) {
                    return error;
                }
            }
        } catch (Exception ignored) {
            // Fallback to provided message.
        }
        return fallback;
    }

    private String loginFallbackMessage(int code) {
        switch (code) {
            case 400:
                return "Please enter a valid username/email and password";
            case 401:
                return "Incorrect password. Please try again";
            case 404:
                return "No account found with this email or username";
            case 429:
                return "Too many attempts. Please wait and try again";
            case 500:
            default:
                return "Server error during login. Please try again";
        }
    }

    private String registerFallbackMessage(int code) {
        switch (code) {
            case 400:
                return "Please check your registration details and try again";
            case 409:
                return "Username or email already exists";
            case 500:
            default:
                return "Unable to create account right now. Please try again";
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
