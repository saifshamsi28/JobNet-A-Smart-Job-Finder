package com.saif.jobnet.Activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.saif.jobnet.JobNetPermissions.REQUEST_MEDIA_PERMISSION;
import static com.saif.jobnet.Utils.Config.BASE_URL;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImage;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Database.JobDao;
import com.saif.jobnet.JobNetPermissions;
import com.saif.jobnet.Models.AuthResponse;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.Resume;
import com.saif.jobnet.Models.ResumeResponseEntity;
import com.saif.jobnet.Models.UserUpdateDTO;
import com.saif.jobnet.Utils.Config;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Api.ApiService;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityProfileBinding;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
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
    private JobDao jobDao;
    private static final int PICK_PDF_REQUEST = 100;
    private String resumeName="";
    private String resumeUrl="";
    private String resumeDate="";
    private String resumeSize="";
    private String userId;
    private Uri selectedImg;
    private JobNetPermissions jobNetPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("JobNetPrefs", MODE_PRIVATE);
        jobDao= DatabaseClient.getInstance(this).getAppDatabase().jobDao();
        progressDialog=new ProgressDialog(this);
        userId = sharedPreferences.getString("userId", null);
        jobNetPermissions=new JobNetPermissions();

        binding.updateButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorActionBarBackground));
        binding.cancelButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));

        // Check if user is logged in
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        System.out.println("ProfileActivity " +isLoggedIn);
        if (!isLoggedIn) {
            // Redirect to LoginActivity
            Toast.makeText(this, "Redirected to login", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        }else{
            System.out.println("ProfileActivity ,");
            loadUserProfile();
        }

        binding.basicDetailsEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(ProfileActivity.this,EditBasicDetailsActivity.class);
                intent.putExtra("source", "Profile");
                intent.putExtra("userId",user.getId());
                startActivity(intent);
            }
        });

        binding.userEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //enable if email field is not enable
                if(!TextUtils.isEmpty(s) && !Patterns.EMAIL_ADDRESS.matcher(s).matches()){
                    binding.updateButton.setEnabled(false);
                }else{
                    binding.updateButton.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.uploadResumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
                    Log.d("Resume Upload", "Android 13+ detected, no permission required.");
                    openFilePicker(); // Directly open file picker, no need for permissions
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6 to 12
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        openFilePicker();
                    } else {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                    }
                } else {
                    openFilePicker(); // Below Android 6, permissions are granted at install time
                }
            }
        });

        binding.savedJobsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, SavedJobsActivity.class);
                intent.putExtra("source", "Profile");
                startActivity(intent);
            }
        });
        binding.updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUserNameOrEmail();
            }
        });

        binding.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userFieldsAccessibility(false);
            }
        });

        binding.resumeUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

        binding.resumeLayout.setOnClickListener(v -> {
            String resumeUri = sharedPreferences.getString("resumeUrl", "");

            if (!resumeUri.isEmpty()) {
                Uri fileUri = Uri.parse(resumeUri);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(ProfileActivity.this, "No app found to open PDF", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ProfileActivity.this, "No resume found!", Toast.LENGTH_SHORT).show();
            }
        });

        binding.userProfileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check and request media permission
                if(jobNetPermissions.isStoragePermissionOk(ProfileActivity.this)){
                    openProfileImagePicker();
                }else {
//                    Toast.makeText(ProfileActivity.this, "Permission not granted", Toast.LENGTH_SHORT).show();
                    if (!jobNetPermissions.isStoragePermissionOk(ProfileActivity.this)) {
                        jobNetPermissions.requestStoragePermission(ProfileActivity.this);
                    }
                }
            }
        });
    }

    private void synchronizeUserDetails(String id) {
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

        ApiService apiService=retrofit.create(ApiService.class);
        Call<User> response=apiService.getUserProfile(id);
        response.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if(response.isSuccessful()){
                    User user1=response.body();
                    if(user1!=null && user!=null){
                        System.out.println("previous user basic details: "+user.getBasicDetails());
                        user=user1;
                        //save to local database
                        new Thread(() -> jobDao.insertOrUpdateUser(user)).start();
                        setUpProfile(user);
                        System.out.println("updated user basic details: "+user.getBasicDetails());
//                        Toast.makeText(ProfileActivity.this, "Profile synchronised Successfully", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Log.d("ProfileActivity", "Error synchronizing user details");
                    try{
                        if(response.errorBody()!=null){
                            AuthResponse errorResponse=new Gson().fromJson(response.errorBody().string(),AuthResponse.class);
                            Log.d("ProfileActivity", "Error synchronizing user details: "+errorResponse);
                        }
                    }catch (IOException e){
                        e.printStackTrace();

                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable throwable) {
                Log.e("ProfileActivity", "Error synchronizing user details: "+throwable);
                Toast.makeText(ProfileActivity.this, "Error synchronizing user details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_MEDIA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openProfileImagePicker(); // Retry image selection after permission granted
            } else {
                showSettingsDialog("Image selection requires permission. Enable it in settings.");
            }
        } else if (requestCode == PICK_PDF_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker(); // Retry resume selection after permission granted
            } else {
                showSettingsDialog("Resume upload requires permission. Enable it in settings.");
            }
        }
    }

    // Show a dialog directing the user to app settings
    private void showSettingsDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required")
                .setMessage(message)
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void openProfileImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        selectedImg = imageUri;
                        startCropImageActivity(imageUri);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri croppedImageUri = Uri.parse(result.getData().getStringExtra("croppedImageUri"));
                    if (croppedImageUri != null) {
                        selectedImg = croppedImageUri;
                        sharedPreferences.edit().putString("profileImg", croppedImageUri.toString()).apply();
                        Log.d("ProfileActivity", "croppedImageUri: " + croppedImageUri);

//                        GlideApp.with(this)
//                                .load(croppedImageUri)
//                                .into(binding.userProfileImg); // Ensure this is your ImageView
                        user.setProfileImage(String.valueOf(selectedImg));
                        uploadProfileImageInChunks(selectedImg);
                        new Thread(() -> jobDao.insertOrUpdateUser(user)).start();
                        Toast.makeText(this, "setting image", Toast.LENGTH_SHORT).show();
                        Glide.with(this).load(croppedImageUri)
                                .placeholder(R.drawable.profile_icon)
                                .error(R.drawable.profile_icon)
                                .circleCrop()
                                .into(binding.userProfileImg);
                    }
                }
            });

    //Send selected image to CropImageActivity
    private void startCropImageActivity(Uri imageUri) {
        Intent intent = new Intent(this, CropImageActivity.class);
        intent.putExtra("imageUri", imageUri.toString());
        cropImageLauncher.launch(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                System.out.println("storing local uri: "+ fileUri);
                Log.d("Resume Upload", "Selected File URI: " + fileUri.toString());
                uploadResumeInChunks(fileUri);
            }else {
                Log.d("Resume Upload", "file uri is null");
            }
        }
    }

    private void openFilePicker() {
        Intent resumeSelectIntent = new Intent(Intent.ACTION_GET_CONTENT);
        resumeSelectIntent.addCategory(Intent.CATEGORY_OPENABLE);
        resumeSelectIntent.setType("application/pdf");
        startActivityForResult(Intent.createChooser(resumeSelectIntent, "Select Resume"), PICK_PDF_REQUEST);
    }


//    private void uploadProfileImage(Uri imageUri) {
//        File imageFile = null;
//        try {
//            imageFile = convertUriToFile(this, imageUri,"profile");
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
//        MultipartBody.Part body = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);
//
//        Retrofit retrofit=new Retrofit.Builder()
//                .baseUrl(BASE_URL)
//                        .addConverterFactory(GsonConverterFactory.create())
//                                .build();
//        ApiService apiService=retrofit.create(ApiService.class);
//
//        apiService.uploadProfileImage(user.getId(),body).enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
//                if (response.isSuccessful()) {
//                    try {
//                        if (response.body() != null) {
//                        User user1 = new Gson().fromJson(response.body().string(), User.class);
//                        user.setProfileImage(user1.getProfileImage());
//                            System.out.println("uploaded profile img to: "+user1.getProfileImage());
//                        new Thread(() -> jobDao.insertOrUpdateUser(user)).start();
//                        Toast.makeText(ProfileActivity.this, "Profile Image Updated", Toast.LENGTH_SHORT).show();
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    Toast.makeText(ProfileActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
//                    try {
//                        if (response.errorBody() != null) {
//                            AuthResponse errorResponse = new Gson().fromJson(response.errorBody().string(), AuthResponse.class);
//                            Log.e("ProfileActivity", "fail to upload profile: " + errorResponse);
//                        }
//                    }catch (IOException e){
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//                Log.e("ProfileActivity", "fail to upload profile: " + t.getMessage());
//            }
//        });
//    }

    private void uploadProfileImageInChunks(Uri imageUri) {
        try {
            File imageFile = convertUriToFile(this, imageUri, "profile");
            long chunkSize = 512 * 1024; // 512kb
            long fileSize = imageFile.length();
            int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                long startByte = chunkIndex * chunkSize;
                long endByte = Math.min(startByte + chunkSize, fileSize);

                byte[] chunkData = readFileChunk(imageFile, startByte, endByte);
                File chunkFile = new File(getCacheDir(), "chunk_" + chunkIndex + ".jpg");
                FileOutputStream fos = new FileOutputStream(chunkFile);
                fos.write(chunkData);
                fos.close();

                uploadChunkToBackend(user.getId(), chunkFile, chunkIndex, totalChunks);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to split file", Toast.LENGTH_SHORT).show();
        }
    }

    // Read file chunk
    private byte[] readFileChunk(File file, long start, long end) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        raf.seek(start);
        byte[] buffer = new byte[(int) (end - start)];
        raf.readFully(buffer);
        raf.close();
        return buffer;
    }

    private void uploadChunkToBackend(String userId, File chunkFile, int chunkIndex, int totalChunks) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), chunkFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", chunkFile.getName(), requestFile);

        RequestBody indexPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(chunkIndex));
        RequestBody totalPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(totalChunks));

        ApiService apiService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);

        apiService.uploadProfileImageChunk(userId, body, indexPart, totalPart).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("UploadChunk", "Chunk " + chunkIndex + " uploaded.");
                } else {
                    Log.e("UploadChunk", "Failed to upload chunk " + chunkIndex);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("UploadChunk", "Error: " + t.getMessage());
            }
        });
    }

    private void uploadResumeInChunks(Uri fileUri) {
        progressDialog.setMessage("Uploading resume...");
        progressDialog.show();

        try {
            File file = convertUriToFile(this, fileUri,"resume");
            resumeName = sanitizeFileName(resumeName);
            long fileSize = file.length();
            int chunkSize = 512 * 1024; // 512 KB
            int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

            // Check if size is greater than 5 MB
            if (totalChunks > 10) {
                Toast.makeText(ProfileActivity.this, "File size is too large", Toast.LENGTH_SHORT).show();
                binding.resumeName.setText("Resume too large");
                binding.resumeName.setTextColor(Color.RED);
                binding.resumeUploadDate.setText("Please upload file less than 5mb");
                binding.resumeUploadDate.setTextColor(Color.RED);
                progressDialog.dismiss();
                return;
            }

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[chunkSize];
            int bytesRead;
            int chunkIndex = 0;
            AtomicInteger uploadedChunks = new AtomicInteger(0);  // Track uploaded chunks

            while ((bytesRead = fis.read(buffer)) > 0) {
                byte[] chunkData = Arrays.copyOf(buffer, bytesRead); // Ensure correct data length

                RequestBody requestFile = RequestBody.create(MediaType.parse("application/pdf"), chunkData);
                MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", resumeName, requestFile);

                Call<ResponseBody> call = apiService.uploadResumeChunk(
                        RequestBody.create(MediaType.parse("text/plain"), user.getId()),
                        RequestBody.create(MediaType.parse("text/plain"), resumeName),
                        RequestBody.create(MediaType.parse("text/plain"), String.valueOf(chunkIndex)),
                        RequestBody.create(MediaType.parse("text/plain"), String.valueOf(totalChunks)),
                        filePart
                );

                int finalChunkIndex = chunkIndex; // For lambda
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful()) {
//                            Log.e("Upload", "Chunk " + finalChunkIndex + " upload failed: " + response);
                            try {
                                if (response.errorBody() != null) {
                                    String errorJson = response.errorBody().string(); // Read JSON string
                                    Gson gson = new Gson();
                                    ResumeResponseEntity errorResponse = gson.fromJson(errorJson, ResumeResponseEntity.class);

                                    Log.e("Upload", "Chunk Upload failed : " + errorResponse.getMessage());
                                    Toast.makeText(ProfileActivity.this, "Error: " + errorResponse.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.e("Upload", "Error parsing response: " + e.getMessage());
                                Toast.makeText(ProfileActivity.this, "Unknown error occurred", Toast.LENGTH_LONG).show();
                            }
                            return;
                        }
                        Log.d("Upload", "Chunk " + finalChunkIndex + " uploaded successfully");
                        Log.d("Upload", "Chunk " + finalChunkIndex + " size: " + chunkData.length);


                        // Check if all chunks are uploaded before finalizing
                        if (uploadedChunks.incrementAndGet() == totalChunks) {
                            finalizeUpload(apiService, user.getId(), resumeName, fileUri, totalChunks);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        progressDialog.dismiss();
                        Log.e("Upload", "Chunk upload error: " + t.getMessage());
                    }
                });

                chunkIndex++;
            }
            fis.close();
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e("Upload", "File processing error: " + e.getMessage());
        }
    }

    // Finalize Upload Only When All Chunks Are Sent
    private void finalizeUpload(ApiService apiService, String userId, String resumeName, Uri fileUri, int totalChunks) {
        Call<ResponseBody> finalizeCall = apiService.finalizeUpload(userId, resumeName, getCurrentDate(), getFileSize(fileUri), totalChunks);
        finalizeCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    Log.d("Upload", "Resume uploaded successfully!");
                    setUpResumeFile(response);
                    Toast.makeText(ProfileActivity.this, "Resume uploaded successfully", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            Gson gson = new Gson();
                            ResumeResponseEntity errorResponse = gson.fromJson(errorJson, ResumeResponseEntity.class);

                            Log.e("Upload", "Chunk finalize failed: " + errorResponse.getMessage());
                            Toast.makeText(ProfileActivity.this, "Error: " + errorResponse.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("Upload", "Error parsing response: " + e.getMessage());
                        Toast.makeText(ProfileActivity.this, "Unknown error occurred", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("Upload", "Upload error: " + t.getMessage());
            }
        });
    }

    private void setUpResumeFile(Response<ResponseBody> response){
        try {
            // Parse the Resume object from the response
            Resume resume = new Gson().fromJson(response.body().string(), Resume.class);
            Log.d("Upload", "Resume uploaded successfully! Response: " + resume);
            Log.d("Upload", "Resume has some Response: " + response);
            Toast.makeText(ProfileActivity.this, "Resume uploaded successfully", Toast.LENGTH_SHORT).show();

            // Store the resume URL from response
            resumeUrl = resume.getResumeUrl();

            user.setResumeUploaded(true);
            user.setResumeName(resume.getResumeName());
            user.setResumeUrl(resume.getResumeUrl());
            user.setResumeUploadDate(resume.getResumeUploadDate());
            user.setResumeSize(resume.getResumeSize());

            new Thread(() -> jobDao.insertOrUpdateUser(user)).start();

            // Store in SharedPreferences
            sharedPreferences.edit()
                    .putBoolean("isResumeUploaded", true)
                    .putString("resumeName", resume.getResumeName())
                    .putString("resumeName", resume.getResumeName())
                    .putString("resumeUrl", resume.getResumeUrl())
                    .putString("resumeDate", resume.getResumeUploadDate())
                    .putString("resumeSize", resume.getResumeSize())
                    .apply();

            binding.resumeName.setText(resume.getResumeName());
            binding.resumeName.setTextColor(Color.BLACK);
            binding.resumeUploadDate.setTextColor(Color.BLACK);
            binding.resumeUploadDate.setText(resume.getResumeUploadDate());
            binding.resumeSize.setText(formatResumeSize(resume.getResumeSize()));

            binding.uploadResumeButton.setVisibility(GONE);
            binding.resumeUpdateButton.setVisibility(VISIBLE);
            binding.resumeLayout.setVisibility(VISIBLE);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Upload", "Error parsing response");
            Toast.makeText(ProfileActivity.this, "Upload failed: Response parsing error", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatResumeSize(String resumeSize) {
        long size = Long.parseLong(resumeSize);
        double formattedSize;
        String unit;

        if (size >= 1024 * 1024 * 1024) {  // Convert to GB
            formattedSize = size / (1024.0 * 1024 * 1024);
            unit = "GB";
        } else if (size >= 1024 * 1024) {  // Convert to MB
            formattedSize = size / (1024.0 * 1024);
            unit = "MB";
        } else if (size >= 1024) {  // Convert to KB
            formattedSize = size / 1024.0;
            unit = "KB";
        } else {  // Size in bytes
            return size + " Bytes";
        }

        return String.format("%.2f %s", formattedSize, unit);
    }

    private String sanitizeFileName(String fileName) {
        // Remove (number) pattern like (1), (2), etc.
        fileName = fileName.replaceAll("\\s*\\(\\d+\\)", "");

        // Remove [number] pattern like [1], [2], etc.
        fileName = fileName.replaceAll("\\[\\d+\\]", "");

        // Replace spaces and special characters with underscores
        fileName = fileName.replaceAll("[\\s]+", "_");

        // Remove any remaining invalid characters except letters, numbers, underscores, hyphens, and periods
        fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "");

        return fileName;
    }

    private String getFileSize(Uri fileUri) {
        Cursor cursor = getContentResolver().query(fileUri, null, null, null, null);
        int sizeIndex = 0;
        long sizeInBytes=0;
        if (cursor != null) {
            sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            cursor.moveToFirst();
            sizeInBytes= cursor.getLong(sizeIndex);
            cursor.close();
        }
        return sizeInBytes+"";
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri!=null && uri.getScheme()!=null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(nameIndex);
                }
            }
        if (result == null) {
            result = uri.getPath();
            int cut = 0;
            if (result != null) {
                cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        }
        return result;
    }

    public File convertUriToFile(Context context, Uri uri,String fileType) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        File file;
        if(fileType.contains("resume")){
             file= new File(context.getCacheDir(), "resume.pdf");
            resumeName=getFileName(uri);
        }else {
            file= new File(context.getCacheDir(), "profile.png");
        }

        FileOutputStream outputStream = new FileOutputStream(file);

        byte[] buffer = new byte[1024];
        int length;

        if (inputStream != null) {
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
        }
        return file;
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
                //clear the local database also
                new Thread(()
                        -> {
                    jobDao.clearUser();
                }
                ).start();
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
        if (!isValidPhoneNumber(phoneNumber)) {
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.userEmail.setError("Invalid email address");
        }else {
            String emailFromShared=sharedPreferences.getString("userEmail",null);
            if(emailFromShared!=null && emailFromShared.equals(email)){
                updateUserDetails(email,phoneNumber,name);
            }else {
                checkEmailAlreadyExistsAndProceed(email,phoneNumber,name);
            }
        }
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
        if (phoneNumber.length() < 10) {
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

        //check number not exceed length if exceed then check country code given
        if(phoneNumber.length()>10){
            if(!phoneNumber.startsWith("+")){
                binding.contactNumber.setError("Number with Country code must start with '+'");
                return false;
            }else {
                if(phoneNumber.length()==11){
                    binding.contactNumber.setError("Country code missing");
                    return false;
                }else {
                    if(phoneNumber.charAt(1)=='0'){
                        binding.contactNumber.setError("Invalid country code");
                        return false;
                    }else {
                        binding.contactNumber.setError(null);
                    }
                }
            }
        }

        // If all checks pass
        return true;
    }

    private void checkEmailAlreadyExistsAndProceed(String email, String phoneNumber, String name) {
        ProgressDialog progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("Checking email...");
        progressDialog.show();
        String BASE_URL = Config.BASE_URL;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
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
                        binding.userEmail.setError("Email already exists");
                        Toast.makeText(ProfileActivity.this, "Email already exists", Toast.LENGTH_SHORT).show();
                        binding.updateButton.setEnabled(false);
                    } else {
                        // Email is available
                        binding.userEmail.setError(null);
                        updateUserDetails(email,phoneNumber,name);
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Error checking email availability.", Toast.LENGTH_SHORT).show();
                    System.out.println("response: "+response);
                    binding.updateButton.setEnabled(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Boolean> call, @NonNull Throwable throwable) {
                progressDialog.dismiss();
                Toast.makeText(ProfileActivity.this, "Failed to check email. Try again.", Toast.LENGTH_SHORT).show();
                binding.updateButton.setEnabled(true);
            }
        });
    }

    private void updateUserDetails(String email, String phoneNumber, String name) {

        ProgressDialog progressDialog=new ProgressDialog(this);
//        saveButton.setOnClickListener(v -> {
        progressDialog.setMessage("Updating profile...");
        progressDialog.show();
        SharedPreferences.Editor editor = sharedPreferences.edit();

        UserUpdateDTO userUpdateDTO=new UserUpdateDTO(user.getId(),name,email,user.getPassword(),phoneNumber);
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
//        Toast.makeText(ProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();

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
        Call<User> response=apiService.updateUser(userUpdateDTO);
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
                        System.out.println("Resume uploaded: " + user1.isResumeUploaded());
                        System.out.println("Resume url: " + user1.getResumeUrl());
                        System.out.println("Resume name: " + user1.getResumeName());
                        System.out.println("Resume upload date: " + user1.getResumeUploadDate());
                        System.out.println("Resume size: " + user1.getResumeSize());

                        //update the user in local database
                        progressDialog.dismiss();
                        new Thread(() -> jobDao.insertOrUpdateUser(user1)).start();
                        userFieldsAccessibility(false);
                    }
                }else{
                    AuthResponse authResponse = null;
                    System.out.println("received raw response: "+response);
                    if(response.errorBody()!=null)
                        authResponse=new Gson().fromJson(response.errorBody().charStream(),AuthResponse.class);
                    Log.e("ProfileActivity","response: "+authResponse);
                    Toast.makeText(ProfileActivity.this, "Failed to update profile not email", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    userFieldsAccessibility(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable throwable) {
                Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                System.out.println("Error updating user");
                progressDialog.dismiss();
                System.out.println(throwable);
                userFieldsAccessibility(false);
                throwable.printStackTrace();
            }
        });
//        });
    }

    private void loadUserProfile() {
        System.out.println("user id received: "+userId);
        //fetch user from local database
        new Thread(new Runnable() {
            @Override
            public void run() {
                user= jobDao.getCurrentUser(userId);
                runOnUiThread(() -> {
                    System.out.println("user got in database: "+user);
                    setUpProfile(user);
                    //synchronise user details from server
                    synchronizeUserDetails(userId);
                });
            }
        }).start();
    }

    private void setUpProfile(User user) {
        if(user == null){
            Log.e("Profile Activity","user is null");
            Toast.makeText(ProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            finish();
            return;
        }

        if(user.getProfileImage()!=null){
            Uri profileImageUri = Uri.parse(user.getProfileImage());
            System.out.println("profile image uri: "+profileImageUri);

            System.out.println("file size of image: "+getFileSize(profileImageUri));
//            Toast.makeText(ProfileActivity.this, "profile image uri: "+profileImageUri, Toast.LENGTH_SHORT).show();
            Glide.with(ProfileActivity.this)
                    .load(profileImageUri)
                    .placeholder(R.drawable.profile_icon)
                    .error(R.drawable.profile_icon)
                    .circleCrop()
                    .into(binding.userProfileImg);
        }
        binding.profileName.setText(user.getName());
        binding.username.setText(user.getUserName());
        binding.userEmail.setText(user.getEmail());
        if(user.getPhoneNumber()!=null && !user.getPhoneNumber().isEmpty()){
            binding.contactNumber.setVisibility(VISIBLE);
            binding.contactNumber.setText(user.getPhoneNumber());
        }else{
            binding.contactNumber.setVisibility(GONE);
        }
        if (user.isResumeUploaded() || user.getResumeUrl()!=null || !user.getResumeUrl().isEmpty()) {

            sharedPreferences.edit().putString("resumeUrl",user.getResumeUrl()).apply();
            binding.uploadResumeButton.setVisibility(GONE);
            binding.resumeLayout.setVisibility(VISIBLE);
            binding.resumeUpdateButton.setVisibility(VISIBLE);

            binding.resumeName.setText(user.getResumeName());
            binding.resumeSize.setText(formatResumeSize(user.getResumeSize()));
            binding.resumeUploadDate.setText(user.getResumeUploadDate());
        }else {
            System.out.println("resume name is null or empty");
            binding.uploadResumeButton.setVisibility(VISIBLE);
            binding.resumeUpdateButton.setVisibility(GONE);
            binding.resumeLayout.setVisibility(GONE);
//                binding.btnUploadResume.setText("Upload Resume");
        }
        if(!user.getSavedJobs().isEmpty()){
            binding.savedJobsNumber.setText(user.getSavedJobs().size()+"");
            binding.savedJobsLayout.setVisibility(VISIBLE);
        }else {
            binding.savedJobsLayout.setVisibility(GONE);
        }

        if(user.getBasicDetails()!=null){
            binding.basicDetailsLayout.setVisibility(VISIBLE);
            binding.gender.setText(user.getBasicDetails().getGender());

            //set the date of birth in words
            binding.dateOfBirth.setText(formatDate(user.getBasicDetails().getDateOfBirth()));

            binding.currentCity.setText(user.getBasicDetails().getCurrentCity());
        }

        //enable/disable the editing of fields
        userFieldsAccessibility(false);
    }

    private String formatDate(String inputDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("d'%s' MMMM yyyy", Locale.getDefault());

            Date date = inputFormat.parse(inputDate);
            if (date != null) {
                int day = Integer.parseInt(new SimpleDateFormat("d", Locale.getDefault()).format(date));
                String daySuffix = getDaySuffix(day);
                return String.format(outputFormat.format(date), daySuffix);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return inputDate; // Return original if parsing fails
    }

    // Helper method to get the correct suffix for the day (st, nd, rd, th)
    private String getDaySuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th"; // Special case for 11th, 12th, 13th
        }
        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }
    private void userFieldsAccessibility(boolean b) {
        if(b){
            binding.profileName.setEnabled(true);
            binding.username.setEnabled(false); //username can't be changed
            binding.userEmail.setEnabled(true);
            binding.contactNumber.setEnabled(true);
            binding.updateButton.setVisibility(VISIBLE);
            binding.cancelButton.setVisibility(VISIBLE);
            binding.currentCity.setEnabled(true);
            binding.dateOfBirth.setEnabled(true);
            binding.gender.setEnabled(true);


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
            binding.updateButton.setVisibility(GONE);
            binding.cancelButton.setVisibility(GONE);
            binding.currentCity.setEnabled(false);
            binding.dateOfBirth.setEnabled(false);
            binding.gender.setEnabled(false);
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
                oldPasswordHeader.setVisibility(GONE);
                oldPasswordEdittext.setVisibility(GONE);
                passwordVerifyButton.setVisibility(GONE);

                passwordUpdateDialog.findViewById(R.id.new_password_header).setVisibility(VISIBLE);
                passwordUpdateDialog.findViewById(R.id.new_password).setVisibility(VISIBLE);
                passwordUpdateDialog.findViewById(R.id.confirm_password_header).setVisibility(VISIBLE);
                passwordUpdateDialog.findViewById(R.id.confirm_password).setVisibility(VISIBLE);
                passwordUpdateDialog.findViewById(R.id.cancel_confirm_buttons_layout).setVisibility(VISIBLE);
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
//        Call<User> response = apiService.updateUser(user);
//        response.enqueue(new Callback<User>() {
//            @Override
//            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
//                progressDialog.dismiss();
//                if(response.isSuccessful()){
//                    User user1=response.body();
//                    if(user1!=null){
//                        Toast.makeText(ProfileActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
//                        //print user details
//                        System.out.println("Password Updated Successfully");
//                        System.out.println("Password: " + user1.getPassword());
//                    }
//                }else {
//                    AuthResponse authResponse = null;
//                    if(response.errorBody()!=null)
//                        authResponse=new Gson().fromJson(response.errorBody().charStream(),AuthResponse.class);
//                    Log.e("ProfileActivity","response: "+authResponse);
//                    Toast.makeText(ProfileActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<User> call, @NonNull Throwable throwable) {
//                progressDialog.dismiss();
//                Toast.makeText(ProfileActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
//                System.out.println(throwable);
//                throwable.printStackTrace();
//            }
//        });
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

//        binding.jobTable.setVisibility(View.VISIBLE);
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

        //refresh layout when activity is resumed
        loadUserProfile();
        super.onResume();
    }
}
