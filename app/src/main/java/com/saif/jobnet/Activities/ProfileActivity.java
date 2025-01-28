package com.saif.jobnet.Activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Utils.Config;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Network.ApiService;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityProfileBinding;

import java.util.List;
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

        binding.updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUserNameOrEmail();
            }
        });
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
        String name=binding.profileName.getText().toString().trim();
        String email=binding.userEmail.getText().toString().trim();
        String phoneNumber=binding.contactNumber.getText().toString().trim();
        if(name.isEmpty()){
            binding.profileName.setError("Please enter your name");
            return;
        }
        if(email.isEmpty()){
            binding.userEmail.setError("Please enter your email");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.userEmail.setError("Invalid email address");
            return;
        }
        if (isValidPhoneNumber(phoneNumber)) {
            binding.contactNumber.setError("Phone number must be at least 10 digits.");
            return;
        }

        ProgressDialog progressDialog=new ProgressDialog(this);
//        saveButton.setOnClickListener(v -> {
            progressDialog.setMessage("Updating profile...");
            progressDialog.show();
            SharedPreferences.Editor editor = sharedPreferences.edit();

            //name updation
            editor.putString("name", name);
            editor.apply();
            user.setName(name);
            binding.profileName.setText(name);

            //email updation
            editor.putString("userEmail", email);
            editor.apply();
            user.setEmail(email);
            binding.userEmail.setText(email);
            Toast.makeText(ProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();

            if (!phoneNumber.isEmpty()) {
                editor.putString("phoneNumber", phoneNumber);
                editor.apply();
                user.setPhoneNumber(phoneNumber);
                binding.contactNumber.setText(phoneNumber);
            }

        String BASE_URL = Config.BASE_URL;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
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
                public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
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
                public void onFailure(@NonNull Call<User> call, @NonNull Throwable throwable) {
                    Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    System.out.println("Error updating user");
                    progressDialog.dismiss();
                    System.out.println(throwable);
                    throwable.printStackTrace();
                }
            });
//        });
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        // Remove leading and trailing spaces
        phoneNumber = phoneNumber.trim();

        // Check if it's empty
        if (phoneNumber.isEmpty()) {
            binding.contactNumber.setError("Phone number cannot be empty.");
            return false;
        }

        // Check for minimum and maximum length (10-15 digits)
        if (phoneNumber.length() < 10 || phoneNumber.length() > 15) {
            binding.contactNumber.setError("Invalid phone number");
            return false;
        }

        // Check if it contains only digits (allowing "+" at the start for international numbers)
        if (!phoneNumber.matches("\\+?\\d+")) {
            binding.contactNumber.setError("Phone number must contain only digits, optionally starting with '+'.");
            return false;
        }

        // Optional: Check for valid starting digit (e.g., avoiding numbers starting with 0 in some regions)
        if (!phoneNumber.matches("^\\+?[1-9]\\d{9,14}$")) {
            binding.contactNumber.setError("Phone number must start with a valid digit.");
            return false;
        }

        // If all checks pass
        return true;
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
        //enable/disable the editing of fields
        userFieldsAccessibility(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String BASE_URL = Config.BASE_URL;
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(new OkHttpClient().newBuilder()
                                .connectTimeout(15,TimeUnit.SECONDS)
                                .callTimeout(15,TimeUnit.SECONDS)
                                .readTimeout(15,TimeUnit.SECONDS)
                                .writeTimeout(15,TimeUnit.SECONDS)
                                .build())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                ApiService apiService=retrofit.create(ApiService.class);
                Call<User> response=apiService.getUserById(userId);
                response.enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                        if(response.isSuccessful()){
                            User user1=response.body();
                            if(user1!=null){
                                binding.savedJobsContainer.setVisibility(View.VISIBLE);
                                user.setSavedJobs(user1.getSavedJobs());
                                populateTableWithJobs(user.getSavedJobs());
                            }else {
                                binding.savedJobsContainer.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable throwable) {
                        System.out.println("Error getting user");
                        System.out.println(throwable);
                        throwable.printStackTrace();
                        binding.savedJobsContainer.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
        populateTableWithJobs(user.getSavedJobs());
    }

    private void userFieldsAccessibility(boolean b) {
        if(b){
            binding.profileName.setEnabled(true);
            binding.username.setEnabled(true);
            binding.userEmail.setEnabled(true);
            binding.contactNumber.setEnabled(true);
            binding.updateButton.setVisibility(View.VISIBLE);

            binding.profileName.requestFocus();
            binding.profileName.setSelection(binding.profileName.getText().length());
            //disable on click method of email
            binding.userEmail.setOnClickListener(null);
            //open the soft keyboard pop up
            new Handler().postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(binding.profileName, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 100);

        }else {
            binding.profileName.setEnabled(false);
            binding.username.setEnabled(false);
            binding.userEmail.setEnabled(false);
            binding.contactNumber.setEnabled(false);
            binding.updateButton.setVisibility(View.GONE);
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
        } catch (ActivityNotFoundException ex) {
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
//            updateUserNameOrEmail();
            userFieldsAccessibility(true);
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
        Button passwordVerifyButton = passwordUpdateDialog.findViewById(R.id.password_verify_button);
        Button cancelButton = passwordUpdateDialog.findViewById(R.id.cancel_button);
        Button confirmButton = passwordUpdateDialog.findViewById(R.id.confirm_button);
        TextInputLayout oldPasswordHeader = passwordUpdateDialog.findViewById(R.id.old_password_header);
        EditText oldPasswordEdittext = passwordUpdateDialog.findViewById(R.id.old_password);

        oldPasswordEdittext.setOnTouchListener((v, event) -> handlePasswordVisibility(event,"old_password"));

        String storedPassword = sharedPreferences.getString("password", null);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Checking password...");

        passwordVerifyButton.setOnClickListener(v -> {
            // Dynamically fetch the entered old password
            String oldPassword = oldPasswordEdittext.getText().toString().trim();
            System.out.println("old pass: "+oldPassword+" , stored pass: "+storedPassword+"inside updatePassword() method");

            if (oldPassword.isEmpty()) {
                oldPasswordEdittext.setError("Please enter your old password");
                oldPasswordEdittext.requestFocus();
                return;
            }

            progressDialog.show();

            if (storedPassword != null && storedPassword.equals(oldPassword)) {
                progressDialog.dismiss();
                Toast.makeText(ProfileActivity.this, "Password verified", Toast.LENGTH_SHORT).show();

                // Hide old password fields and show new password fields
                oldPasswordHeader.setVisibility(View.GONE);
                oldPasswordEdittext.setVisibility(View.GONE);
                passwordVerifyButton.setVisibility(View.GONE);

                passwordUpdateDialog.findViewById(R.id.new_password_header).setVisibility(View.VISIBLE);
                passwordUpdateDialog.findViewById(R.id.new_password).setVisibility(View.VISIBLE);
                passwordUpdateDialog.findViewById(R.id.confirm_password_header).setVisibility(View.VISIBLE);
                passwordUpdateDialog.findViewById(R.id.confirm_password).setVisibility(View.VISIBLE);
                passwordUpdateDialog.findViewById(R.id.cancel_confirm_buttons_layout).setVisibility(View.VISIBLE);
                passwordUpdateDialog.findViewById(R.id.new_password).setOnTouchListener((v1, event) -> handlePasswordVisibility(event, "new_password"));
                passwordUpdateDialog.findViewById(R.id.confirm_password).setOnTouchListener((v1, event) -> handlePasswordVisibility(event, "confirm_password"));

            } else {
                progressDialog.dismiss();
                Toast.makeText(ProfileActivity.this, "Password is incorrect", Toast.LENGTH_SHORT).show();
                oldPasswordEdittext.setError("Password is incorrect");
                oldPasswordEdittext.requestFocus();
            }
        });

        cancelButton.setOnClickListener(v -> passwordUpdateDialog.dismiss());
        confirmButton.setOnClickListener(v -> checkAndUpdatePassword());
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
        if (!isPasswordStrong(confirmPasswordText)) {
            confirmPassword.setError("Password must include at least 8 characters, an uppercase letter, a digit, and a special character.");
            return;
        }
        progressDialog.show();
        //restore password from shared and match with old password
        senUserToBackend(newPasswordText);
    }

    private boolean isPasswordStrong(String password) {
        return password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[@#$%^&+=!].*");
    }

    private void senUserToBackend(String newPasswordText) {
        //restore user from shared prefs and updating password with new password and send to backend
        passwordUpdateDialog.dismiss();
        sharedPreferences.edit().putString("password",newPasswordText).apply();
        String userId = sharedPreferences.getString("userId", null);
        String name = sharedPreferences.getString("name", null);
        String userName = sharedPreferences.getString("userName", null);
        String email = sharedPreferences.getString("userEmail", null);
        String phoneNumber = sharedPreferences.getString("phoneNumber", null);
        user = new User(name, userName, email, newPasswordText,phoneNumber);
        user.setId(userId);
        String BASE_URL = Config.BASE_URL;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(new OkHttpClient
                        .Builder().connectTimeout(10, TimeUnit.SECONDS)
                        .callTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<User> response = apiService.registerUser(user);
        response.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                progressDialog.dismiss();
                if(response.isSuccessful()){
                    User user1=response.body();
                    if(user1!=null){
                        Toast.makeText(ProfileActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                        //print user details
                        System.out.println("Password Updated Successfully");
                        System.out.println("Password: " + user1.getPassword());
                    }
                }else {
                    Toast.makeText(ProfileActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable throwable) {
                progressDialog.dismiss();
                Toast.makeText(ProfileActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                System.out.println(throwable);
                throwable.printStackTrace();
            }
        });
    }

    private boolean handlePasswordVisibility(MotionEvent event, String oldPassword) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            EditText passwordEdittext ;
            if(oldPassword.equals("old_password")){
                passwordEdittext=passwordUpdateDialog.findViewById(R.id.old_password);
            }else if(oldPassword.equals("new_password")){
                passwordEdittext=passwordUpdateDialog.findViewById(R.id.new_password);
            }else{
                passwordEdittext=passwordUpdateDialog.findViewById(R.id.confirm_password);
            }
            System.out.println("old pass: "+passwordEdittext.getText().toString()+"inside handlePasswordVisibility() method");
            Drawable drawableEnd =passwordEdittext.getCompoundDrawablesRelative()[2]; // Right drawable
            if (drawableEnd != null) {
                int drawableWidth = drawableEnd.getBounds().width();
                if (event.getRawX() >= (passwordEdittext.getRight() - drawableWidth - passwordEdittext.getPaddingEnd())) {
                    togglePasswordVisibility(passwordEdittext);
                    return true;
                }
            }
        }
        return false;
    }

    private void togglePasswordVisibility(EditText passwordEditText) {
        if (isPasswordVisible) {
            // Hide password
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            isPasswordVisible = false;
        } else {
            // Show password
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            isPasswordVisible = true;
        }
        // Update the visibility icon
        updatePasswordVisibilityIcon(passwordEditText);
        // Maintain cursor position
        passwordEditText.setSelection(passwordEditText.getText().length());
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

    // Function to dynamically add rows to the TableLayout
    private void populateTableWithJobs(List<Job> jobs) {
        TableLayout tableLayout = findViewById(R.id.job_table);
        Log.d("Database", "fetched from database " + jobs.size() + " jobs");

        // Clear previous rows (if any), while keeping the header row
        if (tableLayout.getChildCount() > 1) {
            tableLayout.removeViews(1, tableLayout.getChildCount() - 1);
        }

        int index = 1; // Starting serial number for jobs

        binding.jobTable.setVisibility(View.VISIBLE);
        for (Job job : jobs) {
            // Create a new row
            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            // Serial Number
            TextView sno = new TextView(this);
            sno.setText(String.valueOf(index++));
            sno.setGravity(Gravity.CENTER);
            sno.setTextColor(Color.BLACK);
            sno.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
            sno.setPadding(8, 8, 8, 8);
//            sno.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.table_cell_border,null));
            row.addView(sno);

            // to set the Job Title
            TextView jobTitle = new TextView(this);
            jobTitle.setText(job.getTitle());
            jobTitle.setLayoutParams(new TableRow.LayoutParams(500, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 3
            jobTitle.setPadding(8, 8, 8, 8);
            jobTitle.setTextColor(Color.BLACK);
            row.addView(jobTitle);

            // to set the Company
            TextView company = new TextView(this);
            company.setText(job.getCompany());
            company.setLayoutParams(new TableRow.LayoutParams(300, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 2
            company.setPadding(8, 8, 8, 8);
            company.setMaxLines(jobTitle.getMaxLines());
            company.setTextColor(Color.BLACK);
            row.addView(company);

            // to set the Location
            TextView location = new TextView(this);
            location.setText(job.getLocation());
            location.setLayoutParams(new TableRow.LayoutParams(300, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 2
            location.setPadding(8, 8, 8, 8);
            location.setMaxLines(jobTitle.getMaxLines());
            location.setEllipsize(TextUtils.TruncateAt.END);
            location.setTextColor(Color.BLACK);
            row.addView(location);

            // to set the Salary
            TextView salary = new TextView(this);
            salary.setText(job.getSalary());
            salary.setLayoutParams(new TableRow.LayoutParams(300, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 1
            salary.setPadding(8, 8, 8, 8);
            salary.setMaxLines(jobTitle.getMaxLines());
            salary.setEllipsize(TextUtils.TruncateAt.END);
            salary.setTextColor(Color.BLACK);
            row.addView(salary);

            //to set the ratings
            TextView rating = new TextView(this);
            rating.setText(String.valueOf(job.getRating()));
            rating.setGravity(Gravity.CENTER);
            rating.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 1
            rating.setPadding(8, 8, 8, 8);
            rating.setMaxLines(jobTitle.getMaxLines());
            rating.setEllipsize(TextUtils.TruncateAt.END);
            rating.setTextColor(Color.BLACK);
            row.addView(rating);

            //to set the reviews
            TextView review = new TextView(this);
            review.setText(String.valueOf(job.getReview()));
            review.setGravity(Gravity.CENTER);
            review.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 1
            review.setPadding(8, 8, 8, 8);
            review.setMaxLines(jobTitle.getMaxLines());
            review.setEllipsize(TextUtils.TruncateAt.END);
            review.setTextColor(Color.BLACK);
            row.addView(review);

            //to set the job post date
            TextView postDate = new TextView(this);
            postDate.setText(String.valueOf(job.getPostDate()));
            postDate.setGravity(Gravity.CENTER);
            postDate.setLayoutParams(new TableRow.LayoutParams(300, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 1
            postDate.setPadding(8, 8, 8, 8);
            postDate.setMaxLines(jobTitle.getMaxLines());
            postDate.setEllipsize(TextUtils.TruncateAt.END);
            postDate.setTextColor(Color.BLACK);
            row.addView(postDate);

            //to set the URL
            TextView url = new TextView(this);
            url.setText(job.getUrl());
            url.setLayoutParams(new TableRow.LayoutParams(800, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 2
            url.setPadding(8, 8, 8, 8);
            url.setTextColor(getResources().getColor(R.color.blue));
            url.setMaxLines(3);
            url.setClickable(true);
            url.setMovementMethod(LinkMovementMethod.getInstance()); // Enable clicking the link
            Linkify.addLinks(url, Linkify.WEB_URLS); // Automatically convert text to clickable link
            row.addView(url);

            //to set the shortDescription
            TextView description = new TextView(this);
            description.setText(job.getShortDescription());
            description.setLayoutParams(new TableRow.LayoutParams(800, TableRow.LayoutParams.WRAP_CONTENT));
            description.setPadding(8, 8, 8, 8);
            description.setMaxLines(3);
            description.setEllipsize(TextUtils.TruncateAt.END);
            description.setTextColor(Color.BLACK);
            row.addView(description);

            //to set the shortDescription
            TextView jobId = new TextView(this);
            jobId.setText(job.getJobId());
            jobId.setLayoutParams(new TableRow.LayoutParams(500, TableRow.LayoutParams.WRAP_CONTENT));
            jobId.setPadding(8, 8, 8, 8);
            jobId.setMaxLines(3);
            jobId.setEllipsize(TextUtils.TruncateAt.END);
            jobId.setTextColor(Color.BLACK);
            row.addView(jobId);

            // Adding OnClickListener to open Job Details activity
            row.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, JobDetailActivity.class);
                System.out.println("job id: "+ job.getJobId()+"inside onclick method and url="+ job.getUrl());
                intent.putExtra("stringId", job.getJobId());
                intent.putExtra("jobTitle", job.getTitle());
                intent.putExtra("company", job.getCompany());
                intent.putExtra("location", job.getLocation());
                intent.putExtra("salary", job.getSalary());
                intent.putExtra("description", job.getShortDescription());
                intent.putExtra("rating", job.getRating());
                intent.putExtra("reviews", job.getReview());
                intent.putExtra("url", job.getUrl());
                startActivity(intent);
            });

            // Add the row to the TableLayout
            tableLayout.addView(row);
            tableLayout.setStretchAllColumns(true);
        }
    }
    @Override
    protected void onResume() {
        setTitle("Profile");
        super.onResume();
    }
}
