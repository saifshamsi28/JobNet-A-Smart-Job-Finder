package com.saif.jobnet.Activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.saif.jobnet.Adapters.SkillsAdapter;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityAddSkillsBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddSkillsActivity extends AppCompatActivity {

    private ActivityAddSkillsBinding binding;
    private List<String> allSkills;
    private List<String> selectedSkills;
    private SkillsAdapter adapter;
    private AppDatabase appDatabase;
    private User user;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddSkillsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        allSkills = Arrays.asList("Java", "Kotlin", "Python", "C++", "SQL", "Android", "Spring Boot");
        selectedSkills = getIntent().getStringArrayListExtra("selectedSkills");

        appDatabase = DatabaseClient.getInstance(this).getAppDatabase();
        new Thread(new Runnable() {
            @Override
            public void run() {
                user=appDatabase.jobDao().getCurrentUser();
            }
        }).start();

        binding.skillsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SkillsAdapter(this,allSkills, selectedSkills);
        binding.skillsRecyclerView.setAdapter(adapter);

        binding.searchSkills.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });

        findViewById(R.id.save_skills).setOnClickListener(v -> {
            user.setSkills(selectedSkills);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    appDatabase.jobDao().insertOrUpdateUser(user);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });
                }
            }).start();
        });
    }
}