package com.saif.jobnet.Activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
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
            Dialog dialog=new Dialog(ProfileActivity.this);
            dialog.setContentView(R.layout.password_update_layout);
            if(dialog.getWindow()!=null){
                dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.custom_update_bg));
            }
            dialog.show();
        }else if(item.getItemId()==R.id.logout){
            showConfirmationDialogue();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        setTitle("Profile");
        super.onResume();
    }
}
