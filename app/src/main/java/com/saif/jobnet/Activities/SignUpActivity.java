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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.saif.jobnet.Models.User;
import com.saif.jobnet.Network.ApiService;
import com.saif.jobnet.R;
import com.saif.jobnet.SimpleTextWatcher;
import com.saif.jobnet.databinding.ActivitySignUpBinding;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignUpActivity extends AppCompatActivity {

    ActivitySignUpBinding binding;
    SharedPreferences sharedPreferences;
    private boolean isPasswordVisible = false;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorActionBarBackground));

        sharedPreferences = getSharedPreferences("JobNetPrefs", MODE_PRIVATE);

        // Add TextWatchers for dynamic validation
        addTextWatchers();

        binding.username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                checkUserNameAvailableOrNot(s.toString());
            }
        });

        // Register Button Click
        binding.registerButton.setOnClickListener(v -> registerUser());

        // Password Visibility Toggle
        binding.password.setOnTouchListener((v, event) -> handlePasswordVisibility(event));
    }

    private void checkUserNameAvailableOrNot(String userName) {
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl("http://10.162.1.53:8080/")
                .client((new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .build()))
                .addConverterFactory(GsonConverterFactory.create()).build();

        ApiService apiService=retrofit.create(ApiService.class);
        Call<Boolean> response=apiService.checkUserName(userName);
        response.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(@NonNull Call<Boolean> call, @NonNull Response<Boolean> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean isAvailable = response.body();
                    if (!isAvailable) {
                        binding.usernameAvailable.setVisibility(View.VISIBLE);
                        binding.usernameAvailable.setText("Username already exist");
                        binding.registerButton.setEnabled(false);
                    } else {
                        binding.usernameAvailable.setVisibility(View.GONE);
                        binding.registerButton.setEnabled(true);
                        binding.username.setError(null);
                    }
                }else {
                    System.out.println("Response not successful");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Boolean> call, @NonNull Throwable throwable) {
                Toast.makeText(SignUpActivity.this, "Failed to check username", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTextWatchers() {
        binding.email.addTextChangedListener(createSimpleTextWatcher());
        binding.password.addTextChangedListener(createSimpleTextWatcher());
    }

    private TextWatcher createSimpleTextWatcher() {
       return new SimpleTextWatcher() {
           @Override
           public void onTextChanged(String newText) {
               validateFields();
           }
       };
    }


    private void validateFields() {
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();

        boolean isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        boolean isPasswordValid = !password.isEmpty();

        boolean isFormValid = isEmailValid && isPasswordValid;

        // Enable or disable the register button
        binding.registerButton.setEnabled(isFormValid);
        binding.registerButton.setBackgroundColor(isFormValid
                ? ContextCompat.getColor(this, R.color.colorActionBarBackground)
                : ContextCompat.getColor(this, R.color.disable_btn));
    }

    private void registerUser() {
        String name = binding.name.getText().toString().trim();
        String username = binding.username.getText().toString().trim();
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();
        String phoneNumber = binding.phoneNumber.getText().toString().trim();

        if (!isPasswordStrong(password)) {
            binding.password.setError("Password must include at least 8 characters, an uppercase letter, a digit, and a special character.");
            return;
        }

        if (isValidPhoneNumber(phoneNumber)) {
            return;
        }
        progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("Registering user...");
        progressDialog.show();

        checkEmailAlreadyExist(name,username,email,password,phoneNumber);
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        // Remove leading and trailing spaces
        phoneNumber = phoneNumber.trim();

        // Check if it's empty
        if (phoneNumber.isEmpty()) {
            binding.phoneNumber.setError("Phone number cannot be empty.");
            return false;
        }

        // Check for minimum and maximum length (10-15 digits)
        if (phoneNumber.length() < 10 || phoneNumber.length() > 15) {
            binding.phoneNumber.setError("Invalid phone number");
            return false;
        }

        // Check if it contains only digits (allowing "+" at the start for international numbers)
        if (!phoneNumber.matches("\\+?\\d+")) {
            binding.phoneNumber.setError("Phone number must contain only digits, optionally starting with '+'.");
            return false;
        }

        // Optional: Check for valid starting digit (e.g., avoiding numbers starting with 0 in some regions)
        if (!phoneNumber.matches("^\\+?[1-9]\\d{9,14}$")) {
            binding.phoneNumber.setError("Phone number must start with a valid digit.");
            return false;
        }

        // If all checks pass
        return true;
    }
    private void checkEmailAlreadyExist(String name, String username, String email, String password, String phoneNumber) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.162.1.53:8080/")
                .client(new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Boolean> response = apiService.checkEmailAlreadyExist(email);
        boolean isv=true;

        response.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(@NonNull Call<Boolean> call, @NonNull Response<Boolean> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    boolean isAvailable = response.body();
                    if (!isAvailable) {
                        // Email already exists
                        binding.email.setError("Email already exists");
                        binding.registerButton.setEnabled(true);
                        Toast.makeText(SignUpActivity.this, "Email already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        // Email is available
                        binding.email.setError(null);
                        sendUserToBackend(name, username, email, password, phoneNumber);
                    }
                } else {
                    Toast.makeText(SignUpActivity.this, "Error checking email availability.", Toast.LENGTH_SHORT).show();
                    binding.registerButton.setEnabled(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Boolean> call, @NonNull Throwable throwable) {
                progressDialog.dismiss();
                Toast.makeText(SignUpActivity.this, "Failed to check email. Try again.", Toast.LENGTH_SHORT).show();
                binding.registerButton.setEnabled(true);
            }
        });
    }

    private void sendUserToBackend(String name, String username, String email, String password, String phoneNumber) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.162.1.53:8080/")
                .client(new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();


        ApiService apiService=retrofit.create(ApiService.class);
        User user=new User(name,username,email,password,phoneNumber);
        Call<User> response=apiService.registerUser(user);
        response.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                progressDialog.dismiss();
                if(response.isSuccessful()){
                    User user1=response.body();
                    if(user1!=null){
                        Toast.makeText(SignUpActivity.this, "User Registered", Toast.LENGTH_SHORT).show();
                        //print user details received in response
                        System.out.println("Name: "+user1.getName());
                        System.out.println("user name: "+user1.getUserName());
                        System.out.println("Email: "+user1.getEmail());
                        System.out.println("Password: "+user1.getPassword());
                        System.out.println("Phone number: "+user1.getPhoneNumber());
                        System.out.println("saved jobs: "+user1.getSavedJobs());

                        //save user details in shared prefs
                        sharedPreferences.edit().putBoolean("userStored", true).apply();

                        //save user details in shared prefs
                        sharedPreferences.edit().putString("userId", user1.getId()).apply();
                        sharedPreferences.edit().putString("name", user1.getName()).apply();
                        sharedPreferences.edit().putString("userName", user1.getUserName()).apply();
                        sharedPreferences.edit().putString("userEmail", user1.getEmail()).apply();
                        sharedPreferences.edit().putString("phoneNumber", user1.getPhoneNumber()).apply();
                        sharedPreferences.edit().putString("password", user1.getPassword()).apply();

                        // going to Login Activity
                        Intent intent=new Intent(SignUpActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else{
                        Toast.makeText(SignUpActivity.this, "User Not Registered", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable throwable) {
                progressDialog.dismiss();
                Toast.makeText(SignUpActivity.this, "User Not Registered", Toast.LENGTH_SHORT).show();
                Log.e("SignUpActivity", "Error registering user", throwable);

            }
        });

    }

    private boolean isPasswordStrong(String password) {
        return password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[@#$%^&+=!].*");
    }

    private boolean handlePasswordVisibility(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            Drawable drawableEnd = binding.password.getCompoundDrawablesRelative()[2]; // Right drawable
            if (drawableEnd != null) {
                int drawableWidth = drawableEnd.getBounds().width();
                if (event.getRawX() >= (binding.password.getRight() - drawableWidth - binding.password.getPaddingEnd())) {
                    togglePasswordVisibility();
                    return true;
                }
            }
        }
        return false;
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

    @Override
    protected void onResume() {
        setTitle("Sign Up");
        super.onResume();
    }
}
