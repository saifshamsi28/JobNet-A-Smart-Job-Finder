package com.saif.jobnet.Activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Network.ApiService;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityProfileBinding;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileActivity extends AppCompatActivity {

    ActivityProfileBinding binding;
    SharedPreferences sharedPreferences;
    private User user;
    private ProgressDialog progressDialog;
    private Dialog passwordUpdateDialog;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("JobNetPrefs", MODE_PRIVATE);

        System.out.println("saved password: "+sharedPreferences.getString("password",null));
        // Check if user is logged in
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (!isLoggedIn) {
            // Redirect to LoginActivity
            redirectToLogin();
        }else{
            loadUserProfile();
        }

        // Set up Log Out button
//        binding.logoutButton.setOnClickListener(v -> {
//            showConfirmationDialogue();
//        });

//        binding.editButton.setOnClickListener(v -> {
//            updateUserNameOrEmail();
//        });
    }

    private void loadUserFromSharedPreferences() {
        String userId = sharedPreferences.getString("userId", null);
        String name = sharedPreferences.getString("userName", null);
        String email = sharedPreferences.getString("userEmail", null);
        String phoneNumber = sharedPreferences.getString("phoneNumber", null);
        String password = sharedPreferences.getString("password", null);

        if (userId == null || name == null || email == null) {
            redirectToLogin();
        } else {
            user = new User(name, name, email, phoneNumber, password);
            user.setId(userId);
        }
    }

    private void showConfirmationDialogue() {
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
                editor.clear();
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
    }

    private void updateUserNameOrEmail() {
        Dialog dialog = new Dialog(ProfileActivity.this);
        dialog.setContentView(R.layout.update_profile_layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(this,R.drawable.custom_update_bg));
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }

        EditText nameInput = dialog.findViewById(R.id.dialog_name_input);
        nameInput.setText(binding.profileName.getText().toString().trim());
        EditText emailInput = dialog.findViewById(R.id.dialog_email_input);
        emailInput.setText(binding.userEmail.getText().toString().trim());
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        // check if email is valid
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput.getText().toString().trim()).matches()) {
            emailInput.setError("Invalid email address");
        }
        EditText phoneNumberInput = dialog.findViewById(R.id.dialog_phone_input);
        phoneNumberInput.setText(binding.contactNumber.getText().toString().trim());
        phoneNumberInput.setInputType(InputType.TYPE_CLASS_PHONE);

        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        Button saveButton = dialog.findViewById(R.id.save_button);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        ProgressDialog progressDialog=new ProgressDialog(this);
        saveButton.setOnClickListener(v -> {
            progressDialog.setMessage("Updating profile...");
            progressDialog.show();
            String newName = nameInput.getText().toString();
            String newEmail = emailInput.getText().toString();
            String newPhoneNumber = phoneNumberInput.getText().toString();

            if (!newName.isEmpty()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("userName", newName);
                editor.apply();
                user.setName(newName);
                binding.profileName.setText(newName);
            }
            if (!newEmail.isEmpty()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("userEmail", newEmail);
                editor.apply();
                user.setEmail(newEmail);
                binding.userEmail.setText(newEmail);
                Toast.makeText(ProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
            }
            if (!newPhoneNumber.isEmpty()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("phoneNumber", newPhoneNumber);
                editor.apply();
                user.setPhoneNumber(newPhoneNumber);
                binding.contactNumber.setText(newPhoneNumber);
            }

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
            Call<User> response=apiService.registerUser(user);
            response.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if(response.isSuccessful()){
                        User user1=response.body();
                        if(user1!=null) {
                            Toast.makeText(ProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                            //print user details received in response
                            System.out.println("User Updated Successfully");
                            System.out.println("Name: " + user1.getName());
                            System.out.println("user name: " + user1.getUserName());
                            System.out.println("Email: " + user1.getEmail());
                            System.out.println("User id: " + user1.getId());
                            System.out.println("Password: " + user1.getPassword());
                            System.out.println("Phone number: " + user1.getPhoneNumber());
                            System.out.println("saved jobs: " + user1.getSavedJobs());
                            progressDialog.dismiss();

                        }
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable throwable) {
                    Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    System.out.println("Error updating user");
                    progressDialog.dismiss();
                    System.out.println(throwable);
                    throwable.printStackTrace();
                }
            });
            dialog.dismiss();
        });
        dialog.show();
    }

    private void loadUserProfile() {
        String userId = sharedPreferences.getString("userId", null);
        String name = sharedPreferences.getString("name", null);
        String userName = sharedPreferences.getString("userName", null);
        String email = sharedPreferences.getString("userEmail", null);
        String phoneNumber = sharedPreferences.getString("phoneNumber", null);
        String password = sharedPreferences.getString("password", null);
        user = new User(name, userName, email, password,phoneNumber);
        user.setId(userId);
        binding.profileName.setText(name);
        binding.username.setText(userName);
        binding.userEmail.setText(email);
        if(phoneNumber!=null && !phoneNumber.isEmpty()){
            binding.contactNumber.setVisibility(View.VISIBLE);
            binding.contactNumber.setText(phoneNumber);
        }else{
            binding.contactNumber.setVisibility(View.GONE);
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void sendEmail(View view) {
        String emailAddress = binding.userEmail.getText().toString();
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + emailAddress));
        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email client installed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_profile_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.update_profile){
            updateUserNameOrEmail();
        }else if(item.getItemId()==R.id.changed_password){
            passwordUpdateDialog=new Dialog(ProfileActivity.this);
            passwordUpdateDialog.setContentView(R.layout.password_update_layout);
            if(passwordUpdateDialog.getWindow()!=null){
                passwordUpdateDialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(this,R.drawable.custom_update_bg));
            }
            EditText editText=passwordUpdateDialog.findViewById(R.id.old_password);
            System.out.println("old pass: "+editText.getText().toString());

            updatePassword();
            passwordUpdateDialog.show();
        }else if(item.getItemId()==R.id.logout){
            showConfirmationDialogue();
        }
        return super.onOptionsItemSelected(item);
    }

    private void updatePassword() {
        System.out.println("in updatePassword() method");
        Button passwordVerifyButton=passwordUpdateDialog.findViewById(R.id.password_verify_button);
        Button cancelButton=passwordUpdateDialog.findViewById(R.id.cancel_button);
        Button confirmButton=passwordUpdateDialog.findViewById(R.id.confirm_button);
        TextInputLayout oldPasswordHeader=passwordUpdateDialog.findViewById(R.id.old_password_header);
        EditText oldPasswordEdittext=passwordUpdateDialog.findViewById(R.id.old_password);
        String oldPassword=oldPasswordEdittext.getText().toString();
        oldPasswordEdittext.setOnTouchListener((v, event) -> handlePasswordVisibility(event));
        String storedPassword=sharedPreferences.getString("password",null);
        System.out.println("stored pass: "+storedPassword+" old pass: "+oldPassword+"inside verify button ");
        progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("checking password...");
        passwordVerifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                System.out.println("stored pass: "+storedPassword+" old pass: "+oldPassword+"inside verify button ");
                if(storedPassword!=null && storedPassword.equals(oldPassword)){
                    progressDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "Password verified", Toast.LENGTH_SHORT).show();
                    //show all the fields and hide old password and verify button
                    oldPasswordHeader.setVisibility(View.GONE);
                    oldPasswordEdittext.setVisibility(View.GONE);
                    passwordVerifyButton.setVisibility(View.GONE);

                    passwordUpdateDialog.findViewById(R.id.new_password_header).setVisibility(View.VISIBLE);
                    passwordUpdateDialog.findViewById(R.id.new_password).setVisibility(View.VISIBLE);
                    passwordUpdateDialog.findViewById(R.id.confirm_password_header).setVisibility(View.VISIBLE);
                    passwordUpdateDialog.findViewById(R.id.confirm_password).setVisibility(View.VISIBLE);
                    passwordUpdateDialog.findViewById(R.id.cancel_confirm_buttons_layout).setVisibility(View.VISIBLE);

                }else{
                    progressDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "Password is incorrect", Toast.LENGTH_SHORT).show();
                    oldPasswordEdittext.requestFocus();
                }
            }
        });
        cancelButton.setOnClickListener(v -> passwordUpdateDialog.dismiss());
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText oldPassword=passwordUpdateDialog.findViewById(R.id.old_password);
                String oldPasswordText=oldPassword.getText().toString();
                String storedPassword=sharedPreferences.getString("password",null);
                if(storedPassword!=null && storedPassword.equals(oldPasswordText)){
                    checkAndUpdatePassword();
                }
            }
        });
    }

    private void checkAndUpdatePassword() {
        EditText oldPassword=passwordUpdateDialog.findViewById(R.id.old_password);
        String oldPasswordText=oldPassword.getText().toString();
        EditText newPassword=passwordUpdateDialog.findViewById(R.id.new_password);
        String newPasswordText=newPassword.getText().toString();
        EditText confirmPassword=passwordUpdateDialog.findViewById(R.id.confirm_password);
        String confirmPasswordText=confirmPassword.getText().toString();
        if(!newPasswordText.equals(confirmPasswordText)){
            confirmPassword.setError("New password and confirm password must be same");
            return;
        }
        progressDialog.show();
        //restore password from shared and match with old password
        String storedPassword=sharedPreferences.getString("password",null);
        if(storedPassword!=null && storedPassword.equals(oldPasswordText)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("password", newPasswordText);
            editor.apply();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.162.1.53:8080/")
                    .client(new OkHttpClient
                            .Builder().connectTimeout(10, TimeUnit.SECONDS)
                            .callTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(10, TimeUnit.SECONDS)
                            .writeTimeout(10, TimeUnit.SECONDS)
                            .build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);
            Toast.makeText(this, "Password Updated Successfully", Toast.LENGTH_SHORT).show();
        }else{
            progressDialog.dismiss();
            Toast.makeText(this, "Old password is incorrect", Toast.LENGTH_SHORT).show();
            passwordUpdateDialog.findViewById(R.id.old_password).requestFocus();
        }

    }

    private boolean handlePasswordVisibility(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            EditText oldPasswordEdittext=passwordUpdateDialog.findViewById(R.id.old_password);
            Drawable drawableEnd =oldPasswordEdittext.getCompoundDrawablesRelative()[2]; // Right drawable
            if (drawableEnd != null) {
                int drawableWidth = drawableEnd.getBounds().width();
                if (event.getRawX() >= (oldPasswordEdittext.getRight() - drawableWidth - oldPasswordEdittext.getPaddingEnd())) {
                    togglePasswordVisibility(oldPasswordEdittext);
                    return true;
                }
            }
        }
        return false;
    }

    private void togglePasswordVisibility(EditText oldPasswordEdittext) {
        if (isPasswordVisible) {
            // Hide password
            oldPasswordEdittext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            isPasswordVisible = false;
        } else {
            // Show password
            oldPasswordEdittext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            isPasswordVisible = true;
        }
        // Update the visibility icon
        updatePasswordVisibilityIcon(oldPasswordEdittext);
        // Maintain cursor position
        oldPasswordEdittext.setSelection(oldPasswordEdittext.getText().length());
    }

    private void updatePasswordVisibilityIcon(EditText oldPasswordEdittext) {
        Drawable eyeIcon = isPasswordVisible
                ? ContextCompat.getDrawable(this, R.drawable.icon_open_eye)
                : ContextCompat.getDrawable(this, R.drawable.icon_close_eye);

        oldPasswordEdittext.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, // Start drawable
                null, // Top drawable
                eyeIcon, // End drawable
                null // Bottom drawable
        );
    }
    @Override
    protected void onResume() {
        setTitle("Profile");
        super.onResume();
    }
}
