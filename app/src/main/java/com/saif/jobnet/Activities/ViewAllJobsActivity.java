package com.saif.jobnet.Activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.saif.jobnet.Adapters.JobsAdapter;
import com.saif.jobnet.Api.ApiService;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Database.JobDao;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.RecentSearch;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Utils.Config;
import com.saif.jobnet.databinding.ActivityNewOpeningsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ViewAllJobsActivity extends AppCompatActivity {

    ActivityNewOpeningsBinding binding;
    private AppDatabase database;
    private JobDao jobDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewOpeningsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = DatabaseClient.getInstance(this).getAppDatabase();
        jobDao = database.jobDao();

        binding.notFoundJobs.setVisibility(GONE);
        binding.recyclerviewNewOpenings.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        String source=getIntent().getStringExtra("source");
        if(source!=null){
            switch (source) {
                case "suggested":
                    setTitle("Suggested Jobs");
                    showSuggestedJobs();
                    break;
                case "history button":
                    setTitle("History");
                    break;
                case "recent":
                    setTitle("Recent Searches");
                    showRecentJobs();
                    break;
                case "navigation drawer":
                    setTitle("Saved Jobs");
                    break;
                case "new openings":
                        setTitle("New Openings");
                        showNewJobs();
            }
        }
    }

    private void showNewJobs(){
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
        ApiService apiService = retrofit.create(ApiService.class);
        Call<List<Job>> call = apiService.getNewJobs();

        call.enqueue(new Callback<List<Job>>() {
            @Override
            public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                if (response.isSuccessful()) {
                    List<Job> jobs = response.body();
                    if (jobs != null) {
                        Log.d("API Response", "Received " + jobs.size() + " jobs");

                        binding.newJobsSize.setVisibility(VISIBLE);
                        binding.newJobsSize.setText(jobs.size() + " new openings");

                        //set up new jobs section
                        JobsAdapter jobsAdapter = new JobsAdapter(ViewAllJobsActivity.this, jobs,"new openings");
                        binding.recyclerviewNewOpenings.setAdapter(jobsAdapter);
                        binding.recyclerviewNewOpenings.setLayoutManager(new LinearLayoutManager(ViewAllJobsActivity.this, LinearLayoutManager.VERTICAL, false));
                        binding.recyclerviewNewOpenings.setVisibility(VISIBLE);
                    }else{
                        Log.d("API Response", "No jobs found");

                    }
                }
            }

            @Override
            public void onFailure(Call<List<Job>> call, Throwable throwable) {
                Log.e("API Error", "Failed to connect to spring boot server "+throwable);
                Toast.makeText(ViewAllJobsActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuggestedJobs() {
        new Thread(() -> {
            User user = jobDao.getCurrentUser();
            List<Job> allJobs = jobDao.getAllJobs();
            if(user!=null && user.getSkills()!=null && !user.getSkills().isEmpty()) {
                List<Job> suggestedJobs = new ArrayList<>();
                for (Job job : allJobs) {
                    String content = (job.getFullDescription() + " " + job.getShortDescription());
                    if (jobMatchesSkillRegex(content, user.getSkills())) {
                        suggestedJobs.add(job);
                    }
                }
//                System.out.println("suggested jobs found: "+suggestedJobs.size());
//                System.out.println("suggested jobs: "+suggestedJobs);
                runOnUiThread(() -> {
                    JobsAdapter adapter = new JobsAdapter(this, suggestedJobs,"suggested");
                    binding.recyclerviewNewOpenings.setAdapter(adapter);
                    binding.recyclerviewNewOpenings.setVisibility(VISIBLE);
                    binding.recyclerviewNewOpenings.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                    binding.notFoundJobs.setVisibility(GONE);
                    binding.newJobsSize.setVisibility(VISIBLE);
                    binding.newJobsSize.setText(suggestedJobs.size()+" suggestions");
                });
            }else {
                binding.notFoundJobs.setText("Not enough skills to suggest jobs");
                binding.notFoundJobs.setVisibility(VISIBLE);
                binding.recyclerviewNewOpenings.setVisibility(VISIBLE);
                binding.newJobsSize.setVisibility(GONE);
            }
        }).start();
    }

    private void showRecentJobs() {
        new Thread(() -> {
            List<RecentSearch> recentSearches = jobDao.getRecentSearches();

            if (!recentSearches.isEmpty()) {
                List<Job> allRecentJobs = new ArrayList<>();
                int recentSearchesSize = recentSearches.size();
                List<Job> jobsMatched =new ArrayList<>();
                for (RecentSearch search : recentSearches) {
                    if(recentSearchesSize>10) {
                        jobsMatched = jobDao.getJobsByTitle(search.query,3);
                    } else if(recentSearchesSize>5){
                        jobsMatched = jobDao.getJobsByTitle(search.query,5);
                    }else{
                        jobsMatched = jobDao.getJobsByTitle(search.query,10);
                    }
                    allRecentJobs.addAll(jobsMatched);
                }

                runOnUiThread(() -> {
                    JobsAdapter adapter = new JobsAdapter(this, allRecentJobs,"recent");
                    binding.recyclerviewNewOpenings.setAdapter(adapter);
                    binding.recyclerviewNewOpenings.scheduleLayoutAnimation();
                    binding.recyclerviewNewOpenings.setVisibility(VISIBLE);
                    binding.notFoundJobs.setVisibility(GONE);
                    binding.newJobsSize.setVisibility(VISIBLE);
                    binding.newJobsSize.setText(allRecentJobs.size()+" recent searches");
                });
            } else{
                binding.notFoundJobs.setText("No recent searches found");
                binding.notFoundJobs.setVisibility(VISIBLE);
                binding.recyclerviewNewOpenings.setVisibility(VISIBLE);
                binding.newJobsSize.setVisibility(GONE);
            }
        }).start();
    }

    private boolean jobMatchesSkillRegex(String jobText, List<String> skills) {
        jobText = jobText.toLowerCase();
        for (String skill : skills) {
            String regex = "\\b" + Pattern.quote(skill.toLowerCase()) + "\\b";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(jobText);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

}