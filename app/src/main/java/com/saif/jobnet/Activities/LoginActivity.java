package com.saif.jobnet.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Database.JobDao;
import com.saif.jobnet.Utils.Config;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Models.UserLoginCredentials;
import com.saif.jobnet.Api.ApiService;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityLoginBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false; // Track visibility state
    ActivityLoginBinding binding;
    SharedPreferences sharedPreferences;
    private AppDatabase appDatabase;
    private JobDao jobDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorActionBarBackground));
        //hide action bar
        if(getSupportActionBar() != null)
            getSupportActionBar().hide();


        sharedPreferences = getSharedPreferences("JobNetPrefs", MODE_PRIVATE);
        appDatabase = DatabaseClient.getInstance(this).getAppDatabase();
        jobDao=appDatabase.jobDao();


        //to check user is registered or not
        binding.signupText.setVisibility(View.VISIBLE);
        // Check if user is logged in
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
//        if (isLoggedIn) {
//            // Redirect to LoginActivity
//            //restoring the user details from shared prefs
//            String name = sharedPreferences.getString("name", "User Name");
//            String username = sharedPreferences.getString("userName", "username");
//            String email = sharedPreferences.getString("userEmail", "email@example.com");
//            String phoneNumber = sharedPreferences.getString("phoneNumber", "9050346306");
//            String password = sharedPreferences.getString("password", "password");
//            String userId = sharedPreferences.getString("userId", "1");
//            User user = new User(name,username, email, phoneNumber, password);
////            System.out.println("saved [password: "+password);
//            user.setId(userId);
//            redirectToProfile(user);
//            return;
//        }
        // Set up password visibility toggle with accessibility-compliant performClick
        binding.password.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = binding.password.getCompoundDrawablesRelative()[2]; // Right drawable
                if (drawableEnd != null) {
                    int drawableWidth = drawableEnd.getBounds().width();
                    if (event.getRawX() >= (binding.password.getRight() - drawableWidth - binding.password.getPaddingEnd())) {
                        togglePasswordVisibility();
                        v.performClick(); // Ensure accessibility
                        return true;
                    }
                }
            }
            return false;
        });

        // Handling login button click
        binding.loginButton.setOnClickListener(v -> {
            boolean hasError = false;

            if (binding.usernameOrEmail.getText().toString().isEmpty() ) {
                binding.usernameOrEmail.setError("Username or email is required");
                hasError = true;
            }

            if (binding.password.getText().toString().isEmpty()) {
                binding.password.setError("Password is required");
                hasError = true;
            }

            // Reapply the password visibility icon after error
            updatePasswordVisibilityIcon();

            if (!hasError) {
                //to check credentials
                checkCredentials();
            }
        });

        // Handling signup text click
        binding.signupText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        binding.usernameOrEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.invalidCredentials.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private void checkCredentials() {
        String usernameOrEmail = binding.usernameOrEmail.getText().toString().trim();
        String password = binding.password.getText().toString();
            // Create login credentials object
            UserLoginCredentials credentials = new UserLoginCredentials();
            credentials.setUserNameOrEmail(usernameOrEmail);
            credentials.setPassword(password);
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage("Checking credentials...");
            dialog.show();

            // Retrofit setup
             String BASE_URL = Config.BASE_URL;
             Retrofit retrofit = new Retrofit.Builder()
                     .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);
            Call<User> call = apiService.loginUser(credentials);

            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful()) {
                        User user = response.body();
                        if (user != null) {
                            Toast.makeText(LoginActivity.this, "Welcome to JobNet, " + user.getName(), Toast.LENGTH_SHORT).show();
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.putString("userId", user.getId());
                            editor.putString("name", user.getName());
                            editor.putString("userName", user.getUserName());
                            editor.putString("userEmail", user.getEmail());
                            editor.putString("phoneNumber", user.getPhoneNumber());
                            editor.putString("password", user.getPassword());
                            editor.putBoolean("userStored", true);
                            new Thread(() -> jobDao.insertOrUpdateUser(user)).start();
                            editor.apply();
                            dialog.dismiss();
                            redirectToProfile(user);
                        }
                    } else {
                        binding.invalidCredentials.setVisibility(View.VISIBLE);
                        binding.invalidCredentials.setText("Invalid username or password");
                        dialog.dismiss();
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Toast.makeText(LoginActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });

    }


    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            binding.password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            isPasswordVisible = false;
        } else {
            // Show password
            binding.password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            isPasswordVisible = true;
        }
        // Update the visibility icon
        updatePasswordVisibilityIcon();
        // Maintain cursor position
        binding.password.setSelection(binding.password.getText().length());
    }

    private void updatePasswordVisibilityIcon() {
        Drawable eyeIcon = isPasswordVisible
                ? ContextCompat.getDrawable(this, R.drawable.icon_open_eye)
                : ContextCompat.getDrawable(this, R.drawable.icon_close_eye);

        binding.password.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(this, R.drawable.baseline_lock_24), // Start drawable
                null, // Top drawable
                eyeIcon, // End drawable
                null // Bottom drawable
        );
    }

    private void redirectToProfile(User user) {
        Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
        Log.d("LoginActivity","user id: "+user.getId());
        Log.d("LoginActivity","user name: "+user.getName());
        Log.d("LoginActivity","user email: "+user.getEmail());
//        intent.putExtra("user", user);
        //store the user details in shared prefs
        sharedPreferences.edit().putString("userId", user.getId()).apply();
        sharedPreferences.edit().putString("name", user.getName()).apply();
        sharedPreferences.edit().putString("userEmail", user.getEmail()).apply();
        sharedPreferences.edit().putString("phoneNumber", user.getPhoneNumber()).apply();
        sharedPreferences.edit().putString("password", user.getPassword()).apply();
        sharedPreferences.edit().putString("userName", user.getUserName()).apply();
        sharedPreferences.edit().putBoolean("isLoggedIn", true).apply();
        sharedPreferences.edit().putBoolean("isResumeUploaded", user.isResumeUploaded()).apply();
        sharedPreferences.edit().putString("resumeUrl", user.getResumeUrl()).apply();
        sharedPreferences.edit().putString("resumeUploadDate", user.getResumeUploadDate()).apply();
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        setTitle("Login");
        super.onResume();
    }
}
