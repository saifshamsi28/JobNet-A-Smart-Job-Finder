package com.saif.jobnet.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.saif.jobnet.Adapters.SavedJobsAdapter;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Database.JobDao;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.databinding.ActivitySavedJobsBinding;
import com.saif.jobnet.databinding.SavedJobsLayoutBinding;

import java.util.ArrayList;
import java.util.List;

public class SavedJobsActivity extends AppCompatActivity {

    private ActivitySavedJobsBinding binding;
    private AppDatabase appDatabase;
    private JobDao jobDao;
    private User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedJobsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("Saved Jobs");

        appDatabase= DatabaseClient.getInstance(this).getAppDatabase();
        jobDao = appDatabase.jobDao();
        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPreferences = getSharedPreferences("JobNetPrefs", MODE_PRIVATE);
                String userId = sharedPreferences.getString("userId", null);

                if (userId == null) {
                    runOnUiThread(() -> {
                        sharedPreferences.edit().clear().apply();
                        Toast.makeText(SavedJobsActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SavedJobsActivity.this, LoginActivity.class));
                        finish();
                    });
                } else {
                    System.out.println("saved jobs activity: userId: " + userId);
                    currentUser = jobDao.getCurrentUser();

                    runOnUiThread(() -> {
                        if (currentUser != null) {
                            List<Job> savedJobs = currentUser.getSavedJobs();

                            // ✅ Filter out null jobs
                            List<Job> nonNullSavedJobs = new ArrayList<>();
                            if (savedJobs != null) {
                                for (Job job : savedJobs) {
                                    if (job != null) {
                                        nonNullSavedJobs.add(job);
                                    }
                                }
                            }

                            if (nonNullSavedJobs.isEmpty()) {
                                binding.noSavedJobs.setVisibility(View.VISIBLE);
                                binding.savedJobsRecyclerView.setVisibility(View.GONE);
                            } else {
                                binding.noSavedJobs.setVisibility(View.GONE);
                                binding.savedJobsRecyclerView.setVisibility(View.VISIBLE);

                                SavedJobsAdapter adapter = new SavedJobsAdapter(SavedJobsActivity.this, nonNullSavedJobs);
                                binding.savedJobsRecyclerView.setAdapter(adapter);
                                binding.savedJobsRecyclerView.setLayoutManager(new LinearLayoutManager(SavedJobsActivity.this));
                            }

                        } else {
                            sharedPreferences.edit().clear().apply();
                            Toast.makeText(SavedJobsActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SavedJobsActivity.this, LoginActivity.class));
                            finish();
                        }
                    });
                }
            }
        }).start();
    }
}