package com.saif.jobnet.Activities;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;
import com.saif.jobnet.Adapters.SkillsAdapter;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class AddSkillsActivity extends AppCompatActivity {

    private RecyclerView suggestionsRecyclerView;
    private FlexboxLayout selectedSkillsContainer;
    private SearchView searchView;
    private Button btnSave, btnCancel;
    private List<String> allSkills = Arrays.asList("Java", "Kotlin", "Python", "C++", "SQL", "Android", "Spring Boot");
    private List<String> selectedSkills = new ArrayList<>();
    private SkillsAdapter skillsAdapter;
    private AppDatabase appDatabase;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_skills);

        searchView = findViewById(R.id.search_view);
        suggestionsRecyclerView = findViewById(R.id.suggestions_recycler_view);
        selectedSkillsContainer = findViewById(R.id.flexbox_selected_skills);
        btnSave = findViewById(R.id.save_button);
        btnCancel = findViewById(R.id.cancel_button);
        appDatabase=DatabaseClient.getInstance(this).getAppDatabase();

        new Thread(new Runnable() {
            @Override
            public void run() {
                user=appDatabase.jobDao().getCurrentUser();
                selectedSkills.addAll(user.getSkills());
                for(String skill:selectedSkills){
                    createSkillRadioButton(skill);
                }
            }
        }).start();

        skillsAdapter = new SkillsAdapter(this, allSkills, this::addSelectedSkill);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        suggestionsRecyclerView.setAdapter(skillsAdapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                suggestionsRecyclerView.setVisibility(newText.isEmpty() ? View.GONE : View.VISIBLE);
                skillsAdapter.filter(newText);
                return false;
            }
        });

        btnSave.setOnClickListener(view -> {
            Toast.makeText(this, "Skills saved: " + selectedSkills, Toast.LENGTH_SHORT).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    user.setSkills(selectedSkills);
                    appDatabase.jobDao().insertOrUpdateUser(user);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AddSkillsActivity.this, "Skills saved successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
            }).start();
        });

        btnCancel.setOnClickListener(view -> finish());
    }

    private void addSelectedSkill(String skill) {
        if (!selectedSkills.contains(skill)) {
            selectedSkills.add(skill);
            createSkillRadioButton(skill);
            suggestionsRecyclerView.setVisibility(View.GONE);
        }
    }

    private void createSkillRadioButton(String skill) {
        View chipView = LayoutInflater.from(this).inflate(R.layout.skill_item, selectedSkillsContainer, false);
        RadioButton chip = chipView.findViewById(R.id.skillRadioButton);
        chip.setText(skill);
        chip.setVisibility(View.VISIBLE);
        chip.setChecked(true);
        chip.setBackgroundResource(R.drawable.gender_selected);

        chip.setOnClickListener(v -> {
            selectedSkills.remove(skill);
            selectedSkillsContainer.removeView(chipView);
        });

        selectedSkillsContainer.addView(chipView);
    }
}

