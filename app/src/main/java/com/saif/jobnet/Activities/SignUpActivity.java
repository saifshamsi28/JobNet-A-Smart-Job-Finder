package com.saif.jobnet.Activities;

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

import com.saif.jobnet.Models.User;
import com.saif.jobnet.Network.ApiService;
import com.saif.jobnet.Network.RetrofitClient;
import com.saif.jobnet.R;
import com.saif.jobnet.SimpleTextWatcher;
import com.saif.jobnet.databinding.ActivitySignUpBinding;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignUpActivity extends AppCompatActivity {

    ActivitySignUpBinding binding;
    SharedPreferences sharedPreferences;
    private boolean isPasswordVisible = false;

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
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build()))
                .addConverterFactory(GsonConverterFactory.create()).build();

        ApiService apiService=retrofit.create(ApiService.class);
        Call<Boolean> response=apiService.checkUserName(userName);
        response.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
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
            public void onFailure(Call<Boolean> call, Throwable throwable) {
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

        if (!phoneNumber.isEmpty() && phoneNumber.length() < 10) {
            binding.phoneNumber.setError("Phone number must be at least 10 digits.");
            return;
        }

        // Save user data in SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString("name",name);
//        editor.putString("userName", username);
//        editor.putString("userEmail", email);
//        editor.putString("userPassword", password);
//        editor.putString("userPhoneNumber", phoneNumber);
//

        editor.putBoolean("isRegistered", true);
        editor.putBoolean("isLoggedIn", false);
        editor.apply();
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendUserToBackend(name, username, email, password, phoneNumber);
            }
        }).start();

        Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();

        // Redirect to LoginActivity
        startActivity(new Intent(this, LoginActivity.class));
        finish();
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
        User user=new User(System.currentTimeMillis()+"",name,username,email,password,phoneNumber);
        Call<User> response=apiService.registerUser(user);
        response.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
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
                    } else{
                        Toast.makeText(SignUpActivity.this, "User Not Registered", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable throwable) {

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
