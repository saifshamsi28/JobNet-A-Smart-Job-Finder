package com.saif.jobnet.Activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.saif.jobnet.Models.User;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {

    ActivityProfileBinding binding;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("JobNetPrefs", MODE_PRIVATE);

        // Check if user is logged in
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (!isLoggedIn) {
            // Redirect to LoginActivity
            redirectToLogin();
            return;
        }

        Intent intent = getIntent();
        User user = intent.getParcelableExtra("user");
        // Load user details
        loadUserProfile(user);

        // Set up Log Out button
        binding.logoutButton.setOnClickListener(v -> {
            // Clear user data and redirect to login
            //a confirmation dialogue to confirm the logout
            Dialog dialog=new Dialog(ProfileActivity.this);
            dialog.setContentView(R.layout.confirmation_dialogue_layout);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            }
            dialog.show();
            TextView confirmButton = dialog.findViewById(R.id.confirm_button);
            TextView dismissButton = dialog.findViewById(R.id.dismiss_button);

            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isLoggedIn", false);
                    editor.apply();
                    dialog.dismiss();
                    dialog.cancel();
                    Toast.makeText(ProfileActivity.this, "Logged Out Successfully", Toast.LENGTH_SHORT).show();
                    redirectToLogin();
                    finish();
                }
            });
            dismissButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        });

        binding.editButton.setOnClickListener(v -> {
            updateUserNameOrEmail();
        });
    }

    private void updateUserNameOrEmail() {
        Dialog dialog = new Dialog(ProfileActivity.this);
        dialog.setContentView(R.layout.update_profile_layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }

        EditText nameInput = dialog.findViewById(R.id.dialog_name_input);
        nameInput.setText(binding.profileName.getText().toString());
        EditText emailInput = dialog.findViewById(R.id.dialog_email_input);
        emailInput.setText(binding.profileEmail.getText().toString());
        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        Button saveButton = dialog.findViewById(R.id.save_button);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            String newName = nameInput.getText().toString();
            String newEmail = emailInput.getText().toString();

            if (!newName.isEmpty()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("userName", newName);
                editor.apply();
                binding.profileName.setText(newName);
            }
            if (!newEmail.isEmpty()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("userEmail", newEmail);
                editor.apply();
                binding.profileEmail.setText(newEmail);
                Toast.makeText(ProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        dialog.show();
    }

    private void loadUserProfile(User user) {
        binding.profileName.setText(user.getName());
        binding.profileEmail.setText(user.getEmail());
        binding.phoneNumber.setText(user.getPhoneNumber());
        binding.username.setText(user.getUserName());
    }

    private void redirectToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void sendEmail(View view) {
        String emailAddress = binding.profileEmail.getText().toString();
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + emailAddress));
        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email client installed.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onResume() {
        setTitle("Profile");
        super.onResume();
    }
}
