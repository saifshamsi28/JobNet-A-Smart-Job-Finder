package com.saif.jobnet.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false; // Track visibility state
    ActivityLoginBinding binding;
    SharedPreferences sharedPreferences;

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

        //to check user is registered or not
        boolean isRegistered = sharedPreferences.getBoolean("isRegistered", false);
        if (!isRegistered) {
            // Redirect to SignUpActivity
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
            finish();
        }else {
            binding.signupText.setVisibility(View.GONE);
        }
        // Check if user is logged in
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (isLoggedIn) {
            // Redirect to LoginActivity
            redirectToProfile();
            return;
        }
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
                binding.usernameOrEmail.setError("Username is required");
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

        binding.username.addTextChangedListener(new TextWatcher() {
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
//        Toast.makeText(this, "Welcome to JobNet " + binding.username.getText().toString().trimDD(), Toast.LENGTH_SHORT).show();
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String usernameOrEmail = binding.usernameOrEmail.getText().toString().trim();
        String password = binding.password.getText().toString().trim();
        String storedUsername = sharedPreferences.getString("userName", "");
        String storedEmail = sharedPreferences.getString("userEmail", "");
        String storedPassword = sharedPreferences.getString("userPassword", "");

        //to check credentials if wrong then set error in textview
        if (usernameOrEmail.equals(storedUsername) || usernameOrEmail.equals(storedEmail) && password.equals(storedPassword)) {
            Toast.makeText(this, "Welcome to JobNet " , Toast.LENGTH_SHORT).show();
            editor.putBoolean("isLoggedIn", true);
            editor.apply();
            redirectToProfile();
        } else {
            binding.invalidCredentials.setVisibility(View.VISIBLE);
            binding.invalidCredentials.setText("Invalid username or password");
        }
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

    private void redirectToProfile() {
        Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        setTitle("Login");
        super.onResume();
    }
}
