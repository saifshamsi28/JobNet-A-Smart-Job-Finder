package com.saif.jobnet.Activities;

import static android.view.View.GONE;

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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.flexbox.FlexboxLayout;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.JobUpdateDTO;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Utils.Config;
import com.saif.jobnet.Api.ApiService;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Database.JobDao;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityJobDetailBinding;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class JobDetailActivity extends AppCompatActivity {

    ActivityJobDetailBinding binding;
    private JobDao jobDao;
    private User currentUser;
    private Job currentJob;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJobDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        AppDatabase appDatabase = DatabaseClient.getInstance(this).getAppDatabase();
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
                currentUser=jobDao.getCurrentUser();
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
                Intent shareUrlIntent=new Intent(Intent.ACTION_SEND);
                shareUrlIntent.setType("text/plain");
                shareUrlIntent.putExtra(Intent.EXTRA_TEXT,url);
                startActivity(shareUrlIntent);
            }
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                animateSequentially(
                        true,
                        binding.descriptionContent
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
//                .baseUrl("http://10.162.1.53:5000/") //for flask
                .baseUrl(BASE_URL) //for spring boot
                .client(new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Job> call = apiService.getJobDescription(currentJob.getJobId(),url);// for spring boot
//        Call<Job> call = apiService.getJobDescriptionFromFlask(url);// for flask
        System.out.println("JobDetailActivity: url hitting: "+retrofit.baseUrl());
        call.enqueue(new Callback<Job>() {
            @Override
            public void onResponse(@NonNull Call<Job> call, @NonNull Response<Job> response) {
                setUpShimmerEffect(false);
                if (response.isSuccessful()) {
                    Job job = response.body();
                    if (job != null) {
                        System.out.println("job received: "+job);
//                        System.out.println("received shortDescription: \n" + job.getFullDescription());
                        Log.d("JobDetailActivity", "Job description fetched successfully");
                        binding.descriptionContent.setText(Html.fromHtml(job.getFullDescription(), Html.FROM_HTML_MODE_LEGACY));
                        System.out.println("job received: "+job);
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
//                            //save job in database
                            job.setFullDescription(job.getFullDescription());
                            currentJob.setFullDescription(job.getFullDescription());
                            new Thread(() -> jobDao.updateJobDescription(currentJob.getUrl(), job.getFullDescription())).start();
//                            updateJobDescriptionOnServer(currentJob);
//                            setUpShimmerEffect(false); // Stop shimmer effect
////                            Toast.makeText(JobDetailActivity.this, "Job description updated", Toast.LENGTH_SHORT).show();
//                            Log.d("JobDetailActivity", "Job description updated");
                            displayJobDetails(currentJob);
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
        String BASE_URL=Config.BASE_URL;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
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

    //set up shimmer effect
    private void setUpShimmerEffect(boolean toStart) {
        if(!toStart){
            binding.shimmerViewContainer.setVisibility(GONE);
            binding.shimmerViewContainer.stopShimmer();
            binding.jobDetailsCardview.setVisibility(View.VISIBLE);
            binding.descriptionCardview.setVisibility(View.VISIBLE);
            binding.shareButton.setVisibility(View.VISIBLE);
            binding.applyNow.setVisibility(View.VISIBLE);
        }else{
            binding.shimmerViewContainer.setVisibility(View.VISIBLE);
            binding.shimmerViewContainer.startShimmer();
            binding.jobDetailsCardview.setVisibility(GONE);
            binding.descriptionCardview.setVisibility(GONE);
            binding.shareButton.setVisibility(GONE);
            binding.applyNow.setVisibility(GONE);
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
                binding.reviews.setVisibility(GONE);
            }
        }
    }

    public void displayFormattedDescription(Job job) {
//        SpannableStringBuilder spannableContent = new SpannableStringBuilder();
//        String description = job.getShortDescription();
//        System.out.println("JobDetailActivity: shortDescription: "+description);
//        String[] lines = description.split("\n");

//        for (String line : lines) {
//            SpannableString spannableLine;
//
//            if (line.startsWith("[HEADING]")) {
//                String heading = line.replace("[HEADING]", "").trim();
//                spannableLine = new SpannableString(heading + "\n");
//                spannableLine.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, spannableLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                spannableLine.setSpan(new RelativeSizeSpan(1.2f), 0, spannableLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//            } else if (line.startsWith("[BULLET]")) {
//                String bulletText = line.replace("[BULLET]", "").trim();
//                spannableLine = new SpannableString(bulletText + "\n");
//
//                if(!spannableLine.toString().startsWith("•"))
//                        spannableLine.setSpan(new BulletSpan(15), 0, spannableLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//            } else {
//                spannableLine = new SpannableString(line + "\n");
//            }
//            spannableContent.append(spannableLine);
//        }

//        binding.descriptionContent.setText(spannableContent);
        binding.postDate.setText("Posted: "+ job.getPostDate().trim()+" days ago");
        binding.jobTitle.setText(job.getTitle());
        binding.companyName.setText(job.getCompany());
        binding.location.setText(job.getLocation());
        binding.salary.setText(job.getSalary());
//        binding.descriptionContent.setText(spannableContent);
        binding.descriptionContent.setText(job.getFullDescription());
        String rating = job.getRating();
        if (rating == null || rating.equals("N/A")) {
            binding.jobRating.setVisibility(GONE);
            binding.ratingImg.setVisibility(GONE);
        } else {
            binding.jobRating.setVisibility(View.VISIBLE);
            binding.jobRating.setText(rating);
        }
        setJobReviews(job.getReview());
        String openings = job.getOpenings();
        if (openings == null || job.getOpenings().equals("N/A")) {
            binding.openings.setVisibility(GONE);
            binding.openingsLogo.setVisibility(GONE);
        } else {
            binding.openings.setVisibility(View.VISIBLE);
            binding.openingsLogo.setVisibility(View.VISIBLE);
            binding.openings.setText("Openings: " + job.getOpenings().trim());
        }
        String applicants = job.getApplicants();
        if (applicants == null || job.getApplicants().equals("N/A")) {
            binding.applicants.setVisibility(GONE);
            binding.applicantsLogo.setVisibility(GONE);
        } else {
            binding.applicants.setVisibility(View.VISIBLE);
            binding.applicantsLogo.setVisibility(View.VISIBLE);
            binding.applicants.setText("Applicants: " + job.getApplicants().trim());
        }


        setUpShimmerEffect(false);
    }

    // Helper method to display job details from the database
    private void displayJobDetails(Job job) {
        System.out.println("job details in JobDetailActivity: " + job);
        if (job == null) {
            Toast.makeText(this, "No job details found", Toast.LENGTH_SHORT).show();
            return;
        }
        if (job.getUrl().contains("indeed.com")) {
            displayFormattedDescription(job);
            return;
        }
        Resources res = getResources();
        String[] headingsArray = res.getStringArray(R.array.job_heading_terms);
        setUpShimmerEffect(false);
        String rating = job.getRating();
        System.out.println("rating : " + rating);
        if (rating == null || rating.equals("N/A")) {
            binding.jobRating.setVisibility(GONE);
            binding.ratingImg.setVisibility(GONE);
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
        binding.postDate.setText("Posted: " + spannableString);

        String openings = job.getOpenings();
        if (openings == null || job.getOpenings().equals("N/A")) {
            binding.openings.setVisibility(GONE);
            binding.openingsLogo.setVisibility(GONE);
        } else {
            binding.openings.setVisibility(View.VISIBLE);
            binding.openingsLogo.setVisibility(View.VISIBLE);
            binding.openings.setText("| Openings: " + job.getOpenings().trim());
        }
        String applicants = job.getApplicants();
        if (applicants == null || job.getApplicants().equals("N/A")) {
            binding.applicants.setVisibility(GONE);
            binding.applicantsLogo.setVisibility(GONE);
        } else {
            binding.applicants.setVisibility(View.VISIBLE);
            binding.applicantsLogo.setVisibility(View.VISIBLE);
            binding.applicants.setText("| Applicants: " + job.getApplicants().trim());
        }

        String description = "No job details found";
        if (job.getFullDescription() != null)
            description = job.getFullDescription();
        else
            Toast.makeText(this, "No job details found", Toast.LENGTH_SHORT).show();
        String descriptionWithoutKeySkills = removeKeySkillsSection(description);
        binding.descriptionContent.setText(Html.fromHtml(descriptionWithoutKeySkills, Html.FROM_HTML_MODE_LEGACY));
        binding.descriptionContent.setMovementMethod(LinkMovementMethod.getInstance());
        extractKeySkills(description);

        // Start animations after data is loaded
        animateSequentially(false,
                binding.jobDetailsCardview,
                binding.descriptionCardview,
                binding.descriptionContent
        );
    }

    public void extractKeySkills(String htmlDescription) {
        // Parse the HTML using Jsoup
        Document document = Jsoup.parse(htmlDescription);

        // Find the "Key Skills" section
        Element keySkillsContainer = document.selectFirst(".styles_key-skill__GIPn_");

        if (keySkillsContainer != null) {
            Elements skillElements = keySkillsContainer.select("a");
            ArrayList<String> keySkills = new ArrayList<>();

            for (Element skillElement : skillElements) {
                String skill = skillElement.text();
                keySkills.add(skill);
            }

            // Get reference to the FlexboxLayout
            FlexboxLayout skillsContainer = findViewById(R.id.skillsContainer);

            // Clear any previously added views to avoid duplicates
            skillsContainer.removeAllViews();

            // Dynamically add TextViews for each skill
            for (String skill : keySkills) {
                RadioButton skillView = new RadioButton(this);
                skillView.setText(skill);
                skillView.setTextSize(12);
//                skillView.setSingleLine(true);
                skillView.setPadding(12, 5, 5, 5);
//                skillView.setBackgroundResource(R.drawable.skill_chip_background); // Custom background
                skillView.setButtonDrawable(null);
                skillView.setBackground(ContextCompat.getDrawable(this, R.drawable.gender_selected));
                skillView.setTextColor(ContextCompat.getColor(this, R.color.black));

                // Set layout parameters with margin
                FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                layoutParams.setMargins(8, 12, 8, 8); // Add margin between skill chips
                skillView.setLayoutParams(layoutParams);

                // Add the TextView to the FlexboxLayout
                skillsContainer.addView(skillView);
                // Animate the skill view when added
                animateSkillView(skillView);
            }
        } else {
            System.out.println("Key Skills section not found in the HTML.");
            binding.keySkillsHeading.setVisibility(GONE);
        }
    }

    private void animateSkillView(View skillView) {
        // Apply an animation to make the skill element fade in and scale up
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(skillView, "alpha", 0f, 1f);
        fadeIn.setDuration(500); // Fade-in duration (500ms)

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(skillView, "scaleX", 0.5f, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(skillView, "scaleY", 0.5f, 1f);
        scaleUpX.setDuration(500); // Scale-up duration (500ms)
        scaleUpY.setDuration(500); // Scale-up duration (500ms)

        // Combine animations
        fadeIn.start();
        scaleUpX.start();
        scaleUpY.start();
    }

    public String removeKeySkillsSection(String htmlDescription) {
        // Parse the HTML using Jsoup
        Document document = Jsoup.parse(htmlDescription);

        // Find the div that contains "Key Skills"
        Element keySkillsContainer = document.selectFirst(".styles_key-skill__GIPn_");

        // Remove the key skills section if it exists
        if (keySkillsContainer != null) {
            keySkillsContainer.remove();
        }

        // Return the modified HTML as a string
        return document.body().html();
    }
}
