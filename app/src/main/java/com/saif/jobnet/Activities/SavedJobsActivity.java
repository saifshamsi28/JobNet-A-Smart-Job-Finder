package com.saif.jobnet.Activities;

import android.content.Intent;
import android.os.Bundle;
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
                currentUser=jobDao.getCurrentUser();
                if(currentUser==null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SavedJobsActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SavedJobsActivity.this, LoginActivity.class));
                            finish();
                        }
                    });
                }
                System.out.println("current user: "+currentUser.getSavedJobs().size());
                for (int i = 0; i < currentUser.getSavedJobs().size(); i++) {
                    System.out.println("saved job title: "+currentUser.getSavedJobs().get(i).getTitle());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SavedJobsAdapter adapter=new SavedJobsAdapter(SavedJobsActivity.this,currentUser.getSavedJobs());
                        binding.savedJobsRecyclerView.setAdapter(adapter);
                        binding.savedJobsRecyclerView.setLayoutManager(new LinearLayoutManager(SavedJobsActivity.this));
//                        binding.savedJobsRecyclerView.
                    }
                });
            }
        }).start();


    }
}