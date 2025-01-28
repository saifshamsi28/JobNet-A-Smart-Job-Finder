package com.saif.jobnet.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Database.JobDao;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.databinding.SavedJobsLayoutBinding;

import java.util.List;

public class SavedJobsActivity extends AppCompatActivity {

    private SavedJobsLayoutBinding binding;
    private AppDatabase appDatabase;
    private JobDao jobDao;
    private User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SavedJobsLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        //fetch the job from intent and set 10 jobs in recyclerview.............
//        Intent intent = getIntent();
//        List<Job> job = intent.getParcelableArrayListExtra("savedStrings");
//        if (job != null) {
//            System.out.println("size of the job list: "+ job.size());
//            for (int i = 0; i < 10; i++) {
//                if (i < job.size()) {
//                    Job currentJob = job.get(i);
//                    binding.jobTitle.setText(currentJob.getTitle());
//                    binding.companyName.setText(currentJob.getCompany());
//                    binding.location.setText(currentJob.getLocation());
//                    binding.salary.setText(currentJob.getSalary());
//                }
//            }
//        }else {
//            Toast.makeText(this, "saved jobs is null", Toast.LENGTH_SHORT).show();
//        }

        appDatabase= DatabaseClient.getInstance(this).getAppDatabase();
        jobDao = appDatabase.jobDao();
        new Thread(new Runnable() {
            @Override
            public void run() {
                currentUser=jobDao.getCurrentUser();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        }).start();


    }
}