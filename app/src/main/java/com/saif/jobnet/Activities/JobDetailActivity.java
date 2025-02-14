package com.saif.jobnet.Activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.BulletSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.saif.jobnet.GeminiAPI;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.JobUpdateDTO;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Utils.Config;
import com.saif.jobnet.Network.ApiService;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Database.JobDao;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityJobDetailBinding;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class JobDetailActivity extends AppCompatActivity {

    ActivityJobDetailBinding binding;
    private AppDatabase appDatabase;
    private JobDao jobDao;
    private User currentUser;
    private Job currentJob;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJobDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        appDatabase = DatabaseClient.getInstance(this).getAppDatabase();
        jobDao = appDatabase.jobDao();
        SharedPreferences sharedPreferences = getSharedPreferences("JobNetPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        Intent intent = getIntent();
        String jobId = intent.getStringExtra("jobId");
        System.out.println("JobDetailActivity: jobId got from intent: "+jobId);
        String url = intent.getStringExtra("url");
        new Thread(new Runnable() {
            @Override
            public void run() {
                currentUser=jobDao.getCurrentUser(userId);
//                currentJob=jobDao.getJobByUrl(url);
            }
        }).start();
        setUpShimmerEffect(true);

        Log.d("JobDetailActivity", "Received URL: calling fetchFullDetails");
        fetchFullDetails(jobId,url);

        binding.applyNow.setOnClickListener(v -> {
            //to open the url in browser
            Intent intent1 = new Intent(Intent.ACTION_VIEW);
            intent1.setData(android.net.Uri.parse(url));
            startActivity(intent1);
        });

        //share the job url by whatsapp or any messaging app when click on share button
        binding.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1=new Intent(Intent.ACTION_SEND);
                intent1.setType("text/plain");
                intent1.putExtra(Intent.EXTRA_TEXT,url);
                startActivity(intent1);
            }
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                animateSequentially(
                        true,
                        binding.descriptionContent,
                        binding.descriptionHeading,
                        binding.dividerView
                );
                finish();
            }
        });
    }


    private void fetchFullDetails(String jobId, String url) {
        new Thread(() -> {
            currentJob = jobDao.getJobByUrl(url);
            System.out.println("JobDetailActivity: currentJob(fetched from local database: "+currentJob);
            runOnUiThread(() -> {
                if (currentJob !=null && currentJob.getFullDescription()!=null) {
                    // Job found in database; display it directly
                    displayJobDetails(currentJob);
                } else {
                    fetchFromApi(jobId,url);
                }
            });
        }).start();
    }

    private void fetchFromApi(String jobId, String url) {
        Log.d("JobDetailActivity", "Received URL: calling fetchFromApi");
        Log.d("JobDetailActivity", "jobId to fetch: "+jobId);
        Log.d("JobDetailActivity", "url to fetch: "+url);
        String BASE_URL = Config.BASE_URL; // // Spring Boot backend URL
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Job> call = apiService.getJobDescription(currentJob.getJobId(),url);
        System.out.println("JobDetailActivity: url hitting: "+retrofit.baseUrl());
        call.enqueue(new Callback<Job>() {
            @Override
            public void onResponse(@NonNull Call<Job> call, @NonNull Response<Job> response) {
                setUpShimmerEffect(false);
                if (response.isSuccessful()) {
                    Job job = response.body();
                    if (job != null) {
                        System.out.println("received shortDescription: \n" + job.getShortDescription());
                        setDescriptionInViews(job);
                    } else {
                        Log.d("API Response", "No job details found");
                    }
                } else {
                    Log.d("API Response in JobDetail", "Response not successful: " + response.errorBody());
                    Toast.makeText(JobDetailActivity.this, "Failed to fetch job details", Toast.LENGTH_SHORT).show();
                    setUpShimmerEffect(false);
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Job> call, @NonNull Throwable t) {
                Log.e("API Error", "Failed to connect to Spring Boot server", t);
                Toast.makeText(JobDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                setUpShimmerEffect(false);
                finish();
            }
        });
    }

    private void setDescriptionInViews(Job job) {
        if (!currentJob.getUrl().contains("indeed.com")) {
            setUpShimmerEffect(true); // Start shimmer effect

//            if(job.getShortDescription().length() > 25) {
                GeminiAPI.formatJobDescription(job.getShortDescription(), new GeminiAPI.GeminiCallback() {
                    @Override
                    public void onSuccess(String formattedText) {
                        runOnUiThread(() -> {
                            String formattedHtml = formatTextWithHtml(formattedText);
//                            binding.descriptionContent.setText(Html.fromHtml(formattedHtml, Html.FROM_HTML_MODE_LEGACY));

                            //save job in database
                            job.setFullDescription(formattedHtml);
                            currentJob.setFullDescription(formattedHtml);
                            new Thread(() -> jobDao.updateJobDescription(currentJob.getUrl(), formattedHtml)).start();
                            updateJobDescriptionOnServer(currentJob);
                            setUpShimmerEffect(false); // Stop shimmer effect
                            Toast.makeText(JobDetailActivity.this, "Job description updated", Toast.LENGTH_SHORT).show();
                            Log.d("JobDetailActivity", "Job description updated");
                            displayJobDetails(currentJob);
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            binding.descriptionContent.setText("Error: " + error);
                            setUpShimmerEffect(false);
                        });
                    }
                });
//            } else {
//                displayFormattedDescription(job);
//            }
        }else {
            displayFormattedDescription(job);
        }
    }


    private String formatTextWithHtml(String text) {
        // Replace Markdown-style bold **Heading** with HTML <b>Heading</b>
        text = text.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");

        // Convert new lines to <br> for proper line breaks in TextView
        text = text.replace("\n", "<br>");

        return text;
    }

    private void animateJobDetails(View view, boolean backPressed) {
        view.setVisibility(View.VISIBLE); // Ensure it's visible before animating

        if(!backPressed){
            // Move from left to right (Translation X)
            ObjectAnimator slideIn = ObjectAnimator.ofFloat(view, "translationX", -500f, 0f);
            slideIn.setDuration(500); // Duration 500ms

            // Fade-in Effect
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            fadeIn.setDuration(500);

            // Play both animations together
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(slideIn, fadeIn);
            animatorSet.start();
        }else {
            // Move from right to left (Translation X)
            ObjectAnimator slideOut = ObjectAnimator.ofFloat(view, "translationX", 0f, 500f);
            slideOut.setDuration(1000); // Duration 500ms
            slideOut.start();
        }
    }

    private void animateSequentially(boolean backPressed, View... views) {
        long delay = 300; // Initial delay
        for (View view : views) {
            view.setVisibility(View.INVISIBLE); // Hide initially
            new Handler(Looper.getMainLooper()).postDelayed(() -> animateJobDetails(view,backPressed), delay);
            delay += 200; // Increment delay for next item
        }
    }

    private void updateJobDescriptionOnServer(Job job) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.162.1.53:8080/")
                .client(new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService jobApiService = retrofit.create(ApiService.class);
        JobUpdateDTO jobUpdateDTO = new JobUpdateDTO(job.getUrl(), job.getFullDescription());

        Call<Void> call = jobApiService.updateJobDescription(job.getJobId(),jobUpdateDTO);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("JobUpdate", "Job description updated successfully");
                } else {
                    Log.e("JobUpdate", "Failed to update job description: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e("JobUpdate", "Error: " + t.getMessage());
            }
        });
    }

//    private void setDescriptionInViews(Job job) {
//        if(!job.getUrl().contains("indeed.com")) {
//        Resources res = getResources();
//            String[] headingsArray = res.getStringArray(R.array.job_heading_terms);
//            // Format shortDescription with bullet points
//            String description = job.getShortDescription().replaceAll("\n+", "\n");
//            String[] contentItems = description.split("\n");
//
//            SpannableStringBuilder spannableContent = new SpannableStringBuilder();
//            for (String item : contentItems) {
//                item = item.trim(); // Trim each item for cleaner formatting
//                boolean isHeading = false;
//                for (String heading : headingsArray) {
//                    if (item.length() < 25 && item.contains(heading)) {
//                        isHeading = true;
//                        break;
//                    }
//                }
//
//                if (isHeading) {
//                    SpannableString boldHeading = new SpannableString(item);
//                    boldHeading.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, boldHeading.length(), 0);
//                    boldHeading.setSpan(new RelativeSizeSpan(1.2f), 0, boldHeading.length(), 0);
//                    spannableContent.append(boldHeading).append("\n");
//                    continue;
//                }
//
//                // Check if the line contains a heading (e.g., "Key Skills:", "Experience:", etc.)
//                if (item.contains(":") && !item.contains("http")) {
//                    String[] parts = item.split(":", 2);
//                    String heading = parts[0] + ":";
//                    String content = parts.length > 1 ? parts[1] : "";
//
//                    // Apply bold and larger text to headings
//                    SpannableString headingSpannable = new SpannableString(heading);
//                    headingSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, headingSpannable.length(), 0);
//                    headingSpannable.setSpan(new RelativeSizeSpan(1.2f), 0, headingSpannable.length(), 0); // 1.2x size for headings
//
//                    // Append formatted heading and content
//                    spannableContent.append(headingSpannable).append(content).append("\n");
//                } else if (item.contains("http")) {
//                    // For clickable links
//                    SpannableString linkSpannable = new SpannableString(item);
//                    linkSpannable.setSpan(new URLSpan(item), 0, linkSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    spannableContent.append(linkSpannable).append("\n");
//                } else {
//                    SpannableString bulletItem = new SpannableString(item);
//                    bulletItem.setSpan(new BulletSpan(20), 0, bulletItem.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    spannableContent.append(bulletItem).append("\n");
//                }
//            }
//            // Set formatted text to TextView and save it in database
//            String rating = job.getRating();
//            if (rating == null || rating.equals("null")) {
//                binding.jobRating.setVisibility(View.GONE);
//                binding.ratingImg.setVisibility(View.GONE);
//            } else {
//                binding.jobRating.setVisibility(View.VISIBLE);
//                binding.jobRating.setText(rating);
//            }
//
//            //to set reviews
//            setJobReviews(job.getReview());
//
//            String openings = job.getOpenings();
//            if (openings == null || job.getOpenings().equals("N/A")) {
//                binding.openings.setVisibility(View.GONE);
//                binding.openingsLogo.setVisibility(View.GONE);
//            } else {
//                binding.openings.setVisibility(View.VISIBLE);
//                binding.openingsLogo.setVisibility(View.VISIBLE);
//                binding.openings.setText("Openings: " + job.getOpenings().trim());
//            }
//            String applicants = job.getApplicants();
//            if (applicants == null || job.getApplicants().equals("N/A")) {
//                binding.applicants.setVisibility(View.GONE);
//                binding.applicantsLogo.setVisibility(View.GONE);
//            } else {
//                binding.applicants.setVisibility(View.VISIBLE);
//                binding.applicantsLogo.setVisibility(View.VISIBLE);
//                binding.applicants.setText("Applicants: " + job.getApplicants().trim());
//            }
//            binding.postDate.setText("Posted: " + job.getPostDate().trim());
//            binding.jobTitle.setText(job.getTitle());
//            binding.companyName.setText(job.getCompany());
//            binding.location.setText(job.getLocation());
//            binding.salary.setText(job.getSalary());
//            binding.descriptionContent.setText(spannableContent);
////                        System.out.println("shortDescription: "+ spannableContent);
//            binding.descriptionContent.setMovementMethod(LinkMovementMethod.getInstance()); // Enable clickable links
//
//            // Save the job in the database with the formatted shortDescription
//            job.setShortDescription(description);
//            currentUser.getSavedJobs().add(job);
//            currentUser.setSavedJobs(currentUser.getSavedJobs());
//            new Thread(() -> jobDao.insertOrUpdateUser(currentUser)).start();
//        } else {
//            displayFormattedDescription(job);
////            new Thread(() -> jobDao.updateJobDescription(job.getUrl(), job.getShortDescription())).start();
//        }
//    }

    //set up shimmer effect
    private void setUpShimmerEffect(boolean toStart) {
        if(!toStart){
            binding.shimmerViewContainer.setVisibility(View.GONE);
            binding.shimmerViewContainer.stopShimmer();
            binding.jobDetailsCardview.setVisibility(View.VISIBLE);
            binding.descriptionCardview.setVisibility(View.VISIBLE);
            binding.descriptionHeading.setVisibility(View.VISIBLE);
            binding.dividerView.setVisibility(View.VISIBLE);
        }else{
            binding.shimmerViewContainer.setVisibility(View.VISIBLE);
            binding.shimmerViewContainer.startShimmer();
            binding.jobDetailsCardview.setVisibility(View.GONE);
            binding.descriptionCardview.setVisibility(View.GONE);
            binding.descriptionHeading.setVisibility(View.GONE);
            binding.dividerView.setVisibility(View.GONE);
        }
    }
    private void setJobReviews(String review) {
        if (review != null) {
            // Use regex to extract only numbers from the review string
            String numericReview = review.replaceAll("[^\\d]", "").trim(); // Remove all non-digit characters

            if (!numericReview.isEmpty()) {
                numericReview+=" Reviews";
                // Set only the numerical part if it exists
                binding.reviews.setVisibility(View.VISIBLE);
                binding.reviews.setText(numericReview);
            } else {
                // Optionally, set an empty string or some placeholder if no numerical value is present
                binding.reviews.setVisibility(View.GONE);
            }
        }
    }

    public void displayFormattedDescription(Job job) {
        SpannableStringBuilder spannableContent = new SpannableStringBuilder();
        String description = job.getShortDescription();
        System.out.println("JobDetailActivity: shortDescription: "+description);
        String[] lines = description.split("\n");

        for (String line : lines) {
            SpannableString spannableLine;

            if (line.startsWith("[HEADING]")) {
                String heading = line.replace("[HEADING]", "").trim();
                spannableLine = new SpannableString(heading + "\n");
                spannableLine.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, spannableLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableLine.setSpan(new RelativeSizeSpan(1.2f), 0, spannableLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (line.startsWith("[BULLET]")) {
                String bulletText = line.replace("[BULLET]", "").trim();
                spannableLine = new SpannableString(bulletText + "\n");

                if(!spannableLine.toString().startsWith("•"))
                        spannableLine.setSpan(new BulletSpan(15), 0, spannableLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                spannableLine = new SpannableString(line + "\n");
            }
            spannableContent.append(spannableLine);
        }

        binding.descriptionContent.setText(spannableContent);
        binding.postDate.setText("Posted: "+ job.getPostDate().trim());
        binding.jobTitle.setText(job.getTitle());
        binding.companyName.setText(job.getCompany());
        binding.location.setText(job.getLocation());
        binding.salary.setText(job.getSalary());
        binding.descriptionContent.setText(spannableContent);
        String rating = job.getRating();
        if (rating == null || rating.equals("N/A")) {
            binding.jobRating.setVisibility(View.GONE);
            binding.ratingImg.setVisibility(View.GONE);
        } else {
            binding.jobRating.setVisibility(View.VISIBLE);
            binding.jobRating.setText(rating);
        }
        setJobReviews(job.getReview());
        String openings = job.getOpenings();
        if (openings == null || job.getOpenings().equals("N/A")) {
            binding.openings.setVisibility(View.GONE);
            binding.openingsLogo.setVisibility(View.GONE);
        } else {
            binding.openings.setVisibility(View.VISIBLE);
            binding.openingsLogo.setVisibility(View.VISIBLE);
            binding.openings.setText("Openings: " + job.getOpenings().trim());
        }
        String applicants = job.getApplicants();
        if (applicants == null || job.getApplicants().equals("N/A")) {
            binding.applicants.setVisibility(View.GONE);
            binding.applicantsLogo.setVisibility(View.GONE);
        } else {
            binding.applicants.setVisibility(View.VISIBLE);
            binding.applicantsLogo.setVisibility(View.VISIBLE);
            binding.applicants.setText("Applicants: " + job.getApplicants().trim());
        }
        setUpShimmerEffect(false);
    }

    // Helper method to display job details from the database
    private void displayJobDetails(Job job) {
        System.out.println("job details in JobDetailActivity: "+job);
        if(job ==null){
            Toast.makeText(this, "No job details found", Toast.LENGTH_SHORT).show();
            return;
        }
        if(job.getUrl().contains("indeed.com")){
            displayFormattedDescription(job);
            return;
        }
        Resources res = getResources();
        String[] headingsArray = res.getStringArray(R.array.job_heading_terms);
        setUpShimmerEffect(false);
        String rating= job.getRating();
        System.out.println("rating : "+rating);
        if (rating == null || rating.equals("N/A")) {
            binding.jobRating.setVisibility(View.GONE);
            binding.ratingImg.setVisibility(View.GONE);
        } else {
            binding.jobRating.setVisibility(View.VISIBLE);
            binding.jobRating.setText(rating);
        }
        binding.jobTitle.setText(job.getTitle());
        binding.companyName.setText(job.getCompany());
        binding.location.setText(job.getLocation());
        binding.salary.setText(job.getSalary());
        setJobReviews(job.getReview());

        SpannableString spannableString = new SpannableString(job.getPostDate());
        spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, spannableString.length(), 0);
        spannableString.setSpan(Color.BLACK, 0, spannableString.length(), 0);
        binding.postDate.setText("Posted: "+spannableString);

        String openings= job.getOpenings();
        if(openings==null || job.getOpenings().equals("N/A")){
            binding.openings.setVisibility(View.GONE);
            binding.openingsLogo.setVisibility(View.GONE);
        }else {
            binding.openings.setVisibility(View.VISIBLE);
            binding.openingsLogo.setVisibility(View.VISIBLE);
            binding.openings.setText("| Openings: "+ job.getOpenings().trim());
        }
        String applicants= job.getApplicants();
        if(applicants==null || job.getApplicants().equals("N/A")){
            binding.applicants.setVisibility(View.GONE);
            binding.applicantsLogo.setVisibility(View.GONE);
        }else {
            binding.applicants.setVisibility(View.VISIBLE);
            binding.applicantsLogo.setVisibility(View.VISIBLE);
            binding.applicants.setText("| Applicants: "+ job.getApplicants().trim());
        }

        String description="No job details found";
        if(job.getFullDescription()!=null)
            description= job.getFullDescription();
        else
            Toast.makeText(this, "No job details found", Toast.LENGTH_SHORT).show();
        binding.descriptionContent.setText(Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY));
        // Start animations after data is loaded
        animateSequentially(false,
                binding.jobDetailsCardview,
                binding.descriptionCardview,
                binding.descriptionContent
        );
        // Retrieve the plain text shortDescription from the database
//        String description = job.getShortDescription();
//
//        // Split the shortDescription into lines to format with bullets
//        String[] contentItems = description.split("\n");
//
//        // Use SpannableStringBuilder to format each line with bullets
//        SpannableStringBuilder spannableContent = new SpannableStringBuilder();
//        for (String item : contentItems) {
//            int start = spannableContent.length();
//            boolean isHeading = false;
//            for (String heading : headingsArray) {
//                if (item.length() < 25 && item.contains(heading)) {
//                    isHeading = true;
//                    break;
//                }
//            }
//
//            if (isHeading) {
//                SpannableString boldHeading = new SpannableString(item);
//                boldHeading.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, boldHeading.length(), 0);
//                boldHeading.setSpan(new RelativeSizeSpan(1.2f), 0, boldHeading.length(), 0);
//                spannableContent.append(boldHeading).append("\n");
//                continue;
//            }
//            //to bold the headings like "Key Skills", "Qualifications","Experience"
//            if(item.contains(":") && !item.contains("http")){
//                String[] parts = item.split(":", 2);
//                String heading = parts[0]+":";
//                String content = parts[1];
//                SpannableString boldHeading = new SpannableString(heading);
//                //
//                boldHeading.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, boldHeading.length(), 0);
//                boldHeading.setSpan(new RelativeSizeSpan(1.2f), 0, boldHeading.length(), 0); // 1.2x size for headings
////                item=item.replace(heading,"");
//                spannableContent.append(boldHeading).append(content).append("\n");
//                continue;
//            }else if(item.contains("https")){
//                // to make the link clickable
//                SpannableString clickableLink = new SpannableString(item);
//                clickableLink.setSpan(new URLSpan(item), 0, clickableLink.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                //parsing job to url
//
//                Linkify.addLinks(binding.descriptionContent, Linkify.WEB_URLS);
//                spannableContent.append(clickableLink).append("\n");
//                continue;
//            }else{
//                SpannableString bulletItem = new SpannableString(item);
//                //to check whether items already starts with bullet points ,if yes then don't add bullets
//                if(!bulletItem.toString().startsWith("•"))
//                    bulletItem.setSpan(new BulletSpan(20), 0, bulletItem.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                spannableContent.append(bulletItem).append("\n");
//            }
        }

        // Set the formatted text to TextView
//        binding.descriptionContent.setText(spannableContent);
////        binding.descriptionContent.setMovementMethod(new ScrollingMovementMethod());
//        binding.descriptionContent.setMovementMethod(LinkMovementMethod.getInstance());
//    }
}
