package com.saif.jobnet.Activities;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.saif.jobnet.Models.Job;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJobDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        appDatabase = DatabaseClient.getInstance(this).getAppDatabase();
        jobDao = appDatabase.jobDao();

        setUpShimmerEffect();

        Intent intent = getIntent();
        String jobId = intent.getStringExtra("jobId");
        System.out.println("JobDetailActivity: jobId got from intent: "+jobId);
        String url = intent.getStringExtra("url");

        Log.d("JobDetailActivity", "Received URL: calling fetchFullDetails");
        fetchFullDetails(jobId,url);

        binding.applyNow.setOnClickListener(v -> {
            //to open the url in browser
            Intent intent1 = new Intent(Intent.ACTION_VIEW);
            intent1.setData(android.net.Uri.parse(url));
            startActivity(intent1);
        });
    }


    private void fetchFullDetails(String jobId, String url) {
        new Thread(() -> {
            Job cachedJob = jobDao.getJobByUrl(url);
            runOnUiThread(() -> {
                if (cachedJob !=null && cachedJob.getShortDescription().length()>250) {
                    // Job found in database; display it directly
                    displayJobDetails(cachedJob);
                } else {
                    fetchFromApi(jobId,url);
                }
            });
        }).start();
    }

//    private void fetchFromApi(Job url) {
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl("http://10.162.1.53:5000")
//                .client(new OkHttpClient.Builder()
//                        .connectTimeout(60, TimeUnit.SECONDS)
//                        .readTimeout(60, TimeUnit.SECONDS)
//                        .writeTimeout(60, TimeUnit.SECONDS)
//                        .build())
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        System.out.println("JobDetailActivity: url: "+url);
//        ApiService apiService = retrofit.create(ApiService.class);
//        Call<Job> call = apiService.getJobDescription(url);
//        Log.d("API Call", "URL: " + url);
//
//        call.enqueue(new Callback<Job>() {
//            @Override
//            public void onResponse(Call<Job> call, Response<Job> response) {
//                if (response.isSuccessful()) {
//                    Job job = response.body();
//                    if (job != null){
//                            if(!job.getUrl().contains("indeed.com")) {
//                                System.out.println("JobDetailActivity: shortDescription: "+job.getShortDescription());
//                                Resources res = getResources();
//                                Job[] headingsArray = res.getStringArray(R.array.job_heading_terms);
//                                // Format shortDescription with bullet points
//                                Job shortDescription = job.getShortDescription().replaceAll("\n+", "\n");
//                                Job[] contentItems = shortDescription.split("\n");
//
//                                SpannableStringBuilder spannableContent = new SpannableStringBuilder();
//                                for (Job item : contentItems) {
//                                    item = item.trim(); // Trim each item for cleaner formatting
//                                    boolean isHeading = false;
//                                    for (Job heading : headingsArray) {
//                                        if (item.length() < 25 && item.contains(heading)) {
//                                            isHeading = true;
//                                            break;
//                                        }
//                                    }
//
//                                    if (isHeading) {
//                                        SpannableString boldHeading = new SpannableString(item);
//                                        boldHeading.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, boldHeading.length(), 0);
//                                        boldHeading.setSpan(new RelativeSizeSpan(1.2f), 0, boldHeading.length(), 0);
//                                        spannableContent.append(boldHeading).append("\n");
//                                        continue;
//                                    }
//
//                                    // Check if the line contains a heading (e.g., "Key Skills:", "Experience:", etc.)
//                                    if (item.contains(":") && !item.contains("http")) {
//                                        Job[] parts = item.split(":", 2);
//                                        Job heading = parts[0] + ":";
//                                        Job content = parts.length > 1 ? parts[1] : "";
//
//                                        // Apply bold and larger text to headings
//                                        SpannableString headingSpannable = new SpannableString(heading);
//                                        headingSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, headingSpannable.length(), 0);
//                                        headingSpannable.setSpan(new RelativeSizeSpan(1.2f), 0, headingSpannable.length(), 0); // 1.2x size for headings
//
//                                        // Append formatted heading and content
//                                        spannableContent.append(headingSpannable).append(content).append("\n");
//                                    } else if (item.contains("http")) {
//                                        // For clickable links
//                                        SpannableString linkSpannable = new SpannableString(item);
//                                        linkSpannable.setSpan(new URLSpan(item), 0, linkSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                                        spannableContent.append(linkSpannable).append("\n");
//                                    } else {
//                                        SpannableString bulletItem = new SpannableString(item);
//                                        bulletItem.setSpan(new BulletSpan(20), 0, bulletItem.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                                        spannableContent.append(bulletItem).append("\n");
//                                    }
//                                }
//                               setUpShimmerEffect();
//
//                                // Set formatted text to TextView and save it in database
//                                Job rating = job.getRating();
//                                if (rating == null || rating.equals("null")) {
//                                    binding.jobRating.setVisibility(View.GONE);
//                                    binding.ratingImg.setVisibility(View.GONE);
//                                } else {
//                                    binding.jobRating.setVisibility(View.VISIBLE);
//                                    binding.jobRating.setText(rating);
//                                }
//
//                                //to set reviews
//                                setJobReviews(job.getReview());
//
//                                Job openings = job.getOpenings();
//                                if (openings == null || job.getOpenings().equals("N/A")) {
//                                    binding.openings.setVisibility(View.GONE);
//                                    binding.openingsLogo.setVisibility(View.GONE);
//                                } else {
//                                    binding.openings.setVisibility(View.VISIBLE);
//                                    binding.openingsLogo.setVisibility(View.VISIBLE);
//                                    binding.openings.setText("Openings: " + job.getOpenings().trim());
//                                }
//                                Job applicants = job.getApplicants();
//                                if (applicants == null || job.getApplicants().equals("N/A")) {
//                                    binding.applicants.setVisibility(View.GONE);
//                                    binding.applicantsLogo.setVisibility(View.GONE);
//                                } else {
//                                    binding.applicants.setVisibility(View.VISIBLE);
//                                    binding.applicantsLogo.setVisibility(View.VISIBLE);
//                                    binding.applicants.setText("Applicants: " + job.getApplicants().trim());
//                                }
//                                binding.postDate.setText("Posted: " + job.getPostDate().trim());
//                                binding.jobTitle.setText(job.getTitle());
//                                binding.companyName.setText(job.getCompany());
//                                binding.location.setText(job.getLocation());
//                                binding.salary.setText(job.getSalary());
//                                binding.descriptionContent.setText(spannableContent);
////                        System.out.println("shortDescription: "+ spannableContent);
//                                binding.descriptionContent.setMovementMethod(LinkMovementMethod.getInstance()); // Enable clickable links
//
//                                // Save the job in the database with the formatted shortDescription
//                                job.setShortDescription(shortDescription);
////                        System.out.println("after update, shortDescription: "+job.getShortDescription());
//                                new Thread(() -> jobDao.updateJobDescription(url, shortDescription)).start();
//                            }else {
//                                displayFormattedDescription(job);
//                                new Thread(() -> jobDao.updateJobDescription(url, job.getShortDescription())).start();
//                            }
//                    } else {
//                        Log.d("API Response", "No job details found");
//                    }
//                } else {
//                    Log.d("API Response", "Response not successful: " + response.message());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<Job> call, Throwable throwable) {
//                Log.e("API Error", "Failed to connect to Flask server", throwable);
//                setUpShimmerEffect();
//                Toast.makeText(JobDetailActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

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
        Call<Job> call = apiService.getJobDescription(jobId,url);

        call.enqueue(new Callback<Job>() {
            @Override
            public void onResponse(@NonNull Call<Job> call, @NonNull Response<Job> response) {
                setUpShimmerEffect();
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
                    setUpShimmerEffect();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Job> call, @NonNull Throwable t) {
                Log.e("API Error", "Failed to connect to Spring Boot server", t);
                Toast.makeText(JobDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                setUpShimmerEffect();
                finish();
            }
        });
    }

    private void setDescriptionInViews(Job job) {
        if(!job.getUrl().contains("indeed.com")) {
        Resources res = getResources();
            String[] headingsArray = res.getStringArray(R.array.job_heading_terms);
            // Format shortDescription with bullet points
            String description = job.getShortDescription().replaceAll("\n+", "\n");
            String[] contentItems = description.split("\n");

            SpannableStringBuilder spannableContent = new SpannableStringBuilder();
            for (String item : contentItems) {
                item = item.trim(); // Trim each item for cleaner formatting
                boolean isHeading = false;
                for (String heading : headingsArray) {
                    if (item.length() < 25 && item.contains(heading)) {
                        isHeading = true;
                        break;
                    }
                }

                if (isHeading) {
                    SpannableString boldHeading = new SpannableString(item);
                    boldHeading.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, boldHeading.length(), 0);
                    boldHeading.setSpan(new RelativeSizeSpan(1.2f), 0, boldHeading.length(), 0);
                    spannableContent.append(boldHeading).append("\n");
                    continue;
                }

                // Check if the line contains a heading (e.g., "Key Skills:", "Experience:", etc.)
                if (item.contains(":") && !item.contains("http")) {
                    String[] parts = item.split(":", 2);
                    String heading = parts[0] + ":";
                    String content = parts.length > 1 ? parts[1] : "";

                    // Apply bold and larger text to headings
                    SpannableString headingSpannable = new SpannableString(heading);
                    headingSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, headingSpannable.length(), 0);
                    headingSpannable.setSpan(new RelativeSizeSpan(1.2f), 0, headingSpannable.length(), 0); // 1.2x size for headings

                    // Append formatted heading and content
                    spannableContent.append(headingSpannable).append(content).append("\n");
                } else if (item.contains("http")) {
                    // For clickable links
                    SpannableString linkSpannable = new SpannableString(item);
                    linkSpannable.setSpan(new URLSpan(item), 0, linkSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannableContent.append(linkSpannable).append("\n");
                } else {
                    SpannableString bulletItem = new SpannableString(item);
                    bulletItem.setSpan(new BulletSpan(20), 0, bulletItem.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannableContent.append(bulletItem).append("\n");
                }
            }
            // Set formatted text to TextView and save it in database
            String rating = job.getRating();
            if (rating == null || rating.equals("null")) {
                binding.jobRating.setVisibility(View.GONE);
                binding.ratingImg.setVisibility(View.GONE);
            } else {
                binding.jobRating.setVisibility(View.VISIBLE);
                binding.jobRating.setText(rating);
            }

            //to set reviews
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
            binding.postDate.setText("Posted: " + job.getPostDate().trim());
            binding.jobTitle.setText(job.getTitle());
            binding.companyName.setText(job.getCompany());
            binding.location.setText(job.getLocation());
            binding.salary.setText(job.getSalary());
            binding.descriptionContent.setText(spannableContent);
//                        System.out.println("shortDescription: "+ spannableContent);
            binding.descriptionContent.setMovementMethod(LinkMovementMethod.getInstance()); // Enable clickable links

            // Save the job in the database with the formatted shortDescription
            job.setShortDescription(description);
//            new Thread(() -> jobDao.updateJobDescription(job.getUrl(), description)).start();
        } else {
            displayFormattedDescription(job);
//            new Thread(() -> jobDao.updateJobDescription(job.getUrl(), job.getShortDescription())).start();
        }
    }

    //set up shimmer effect
    private void setUpShimmerEffect() {
        if(binding.shimmerViewContainer.isShimmerStarted()){
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
        setUpShimmerEffect();
    }

    // Helper method to display job details from the database
    private void displayJobDetails(Job job) {
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
        setUpShimmerEffect();
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

        // Retrieve the plain text shortDescription from the database
        String description = job.getShortDescription();

        // Split the shortDescription into lines to format with bullets
        String[] contentItems = description.split("\n");

        // Use SpannableStringBuilder to format each line with bullets
        SpannableStringBuilder spannableContent = new SpannableStringBuilder();
        for (String item : contentItems) {
            int start = spannableContent.length();
            boolean isHeading = false;
            for (String heading : headingsArray) {
                if (item.length() < 25 && item.contains(heading)) {
                    isHeading = true;
                    break;
                }
            }

            if (isHeading) {
                SpannableString boldHeading = new SpannableString(item);
                boldHeading.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, boldHeading.length(), 0);
                boldHeading.setSpan(new RelativeSizeSpan(1.2f), 0, boldHeading.length(), 0);
                spannableContent.append(boldHeading).append("\n");
                continue;
            }
            //to bold the headings like "Key Skills", "Qualifications","Experience"
            if(item.contains(":") && !item.contains("http")){
                String[] parts = item.split(":", 2);
                String heading = parts[0]+":";
                String content = parts[1];
                SpannableString boldHeading = new SpannableString(heading);
                //
                boldHeading.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, boldHeading.length(), 0);
                boldHeading.setSpan(new RelativeSizeSpan(1.2f), 0, boldHeading.length(), 0); // 1.2x size for headings
//                item=item.replace(heading,"");
                spannableContent.append(boldHeading).append(content).append("\n");
                continue;
            }else if(item.contains("https")){
                // to make the link clickable
                SpannableString clickableLink = new SpannableString(item);
                clickableLink.setSpan(new URLSpan(item), 0, clickableLink.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                //parsing job to url

                Linkify.addLinks(binding.descriptionContent, Linkify.WEB_URLS);
                spannableContent.append(clickableLink).append("\n");
                continue;
            }else{
                SpannableString bulletItem = new SpannableString(item);
                //to check whether items already starts with bullet points ,if yes then don't add bullets
                if(!bulletItem.toString().startsWith("•"))
                    bulletItem.setSpan(new BulletSpan(20), 0, bulletItem.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableContent.append(bulletItem).append("\n");
            }
        }

        // Set the formatted text to TextView
        binding.descriptionContent.setText(spannableContent);
//        binding.descriptionContent.setMovementMethod(new ScrollingMovementMethod());
        binding.descriptionContent.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
