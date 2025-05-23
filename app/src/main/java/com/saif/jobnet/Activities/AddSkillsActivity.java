package com.saif.jobnet.Activities;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.saif.jobnet.Adapters.SkillsAdapter;
import com.saif.jobnet.Api.ApiService;
import com.saif.jobnet.Api.SkillFetcher;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Models.JobNetResponse;
import com.saif.jobnet.Models.Skill;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.R;
import com.saif.jobnet.Utils.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddSkillsActivity extends AppCompatActivity {

    private RecyclerView suggestionsRecyclerView;
    private FlexboxLayout selectedSkillsContainer;
    private TextInputEditText searchView;
    private Button btnSave, btnCancel;
    private List<String> allSkills = new ArrayList<>();
    private List<String> selectedSkills = new ArrayList<>();
    private SkillsAdapter skillsAdapter;
    private CardView skillsCardView;
    private AppDatabase appDatabase;
    private User user;
    private Drawable closeIcon;
    private ImageView backButton;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_skills);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        searchView = findViewById(R.id.search_view);
        suggestionsRecyclerView = findViewById(R.id.suggestions_recycler_view);
        skillsCardView = findViewById(R.id.skills_cardview);
        selectedSkillsContainer = findViewById(R.id.flexbox_selected_skills);
        btnSave = findViewById(R.id.save_button);
        btnCancel = findViewById(R.id.cancel_button);
        backButton = findViewById(R.id.back_button);
        progressBar = findViewById(R.id.progressbar);
        progressDialog=new ProgressDialog(this);

        closeIcon = ContextCompat.getDrawable(this, R.drawable.cancel_icon);
        appDatabase=DatabaseClient.getInstance(this).getAppDatabase();

        new Thread(() -> {
            // Step 1: Get current user and selected skills
            user = appDatabase.jobDao().getCurrentUser();
            if(user!=null && user.getSkills() != null){
                selectedSkills.addAll(user.getSkills());
            }

            // Step 2: Check if all skills already exist in Room
            List<Skill> storedSkills = appDatabase.jobDao().getAllSkills();

            if (storedSkills != null && !storedSkills.isEmpty() && storedSkills.size()>=500) {
                System.out.println("fetched from room database");
                System.out.println("stored skill size: "+storedSkills.size());

                // Skills exist in Room → load them directly
                for (Skill skill : storedSkills) {
                    allSkills.add(skill.getName());
                }

                // Update UI with selected skills and all skills
                runOnUiThread(() -> {
                    skillsAdapter = new SkillsAdapter(this, allSkills, this::addSelectedSkill);
                    suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                    suggestionsRecyclerView.setAdapter(skillsAdapter);

                    for (String skill : selectedSkills) {
                        createSkillRadioButton(skill);
                    }

                    skillsAdapter.notifyDataSetChanged();
                });


            } else {
                System.out.println("fetching from api");
                // No skills in Room then Fetch from API
                runOnUiThread(this::fetchSkillsForAllTypes);
            }
        }).start();

        skillsAdapter = new SkillsAdapter(this, allSkills, this::addSelectedSkill);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        suggestionsRecyclerView.setAdapter(skillsAdapter);

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.toString().isEmpty()) {
                    skillsCardView.setVisibility(View.GONE);
                }else {
                    skillsCardView.setVisibility(View.VISIBLE);
                    suggestionsRecyclerView.setVisibility(View.VISIBLE); // Ensure RecyclerView is visible
                    skillsAdapter.filter(charSequence.toString());

                    //If no match found, fetch from API
                    if (skillsAdapter.getItemCount() == 0) {
                        fetchRelatedSkillsFromApi(charSequence.toString());
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        btnSave.setOnClickListener(view -> {
            user.setSkills(selectedSkills);
            progressDialog.setMessage("Saving skills...");
            progressDialog.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    appDatabase.jobDao().insertOrUpdateUser(user);
                    //save skills to backend
                    saveSkillsOnBackend(user.getId(), selectedSkills);
                }
            }).start();
        });

        btnCancel.setOnClickListener(view -> finish());
        backButton.setOnClickListener(view -> finish());
    }

    private void saveSkillsOnBackend(String id, List<String> selectedSkills) {
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl(Config.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService=retrofit.create(ApiService.class);

        apiService.saveSkills(id,selectedSkills)
                .enqueue(new Callback<JobNetResponse>() {
                    @Override
                    public void onResponse(Call<JobNetResponse> call, Response<JobNetResponse> response) {
                        progressDialog.dismiss();
                        if(response.isSuccessful()){
                            JobNetResponse jobNetResponse=response.body();
                            Log.d("AddSkillsActivity","response: "+jobNetResponse);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(AddSkillsActivity.this, "Skills saved successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<JobNetResponse> call, Throwable throwable) {
                        progressDialog.dismiss();
                        Toast.makeText(AddSkillsActivity.this, "Error while saving skills", Toast.LENGTH_SHORT).show();
                        Log.d("AddSkillsActivity","error: "+throwable.getMessage());
                    }
                });


    }

    private void fetchSkillsForAllTypes() {
        String[] types = {"ST1", "ST2", "ST3"}; // Specialized, Common, Certification

        for (String type : types) {
            SkillFetcher.fetchSkillsByType(type, new SkillFetcher.SkillCallback() {
                @Override
                public void onSkillsFetched(List<String> skills) {
                    new Thread(() -> {
                        List<Skill> skillEntities = new ArrayList<>();
                        for (String skill : skills) {
                            skillEntities.add(new Skill(skill));
                        }
                        System.out.println("skills fetched from api: "+skillEntities.size());
                        appDatabase.jobDao().insertSkills(skillEntities);
                    }).start();
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(AddSkillsActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    private void fetchRelatedSkillsFromApi(String query) {

        //show spinner and hide list
        progressBar.setVisibility(View.VISIBLE);
//        progressBar.setIndeterminate(true);
        suggestionsRecyclerView.setVisibility(View.GONE);

        SkillFetcher.searchSkills(query, new SkillFetcher.SkillCallback() {
            @Override
            public void onSkillsFetched(List<String> skills) {
                runOnUiThread(() -> {
//                    System.out.println("newly related skills fetched from api: "+skills);
                    allSkills.addAll(skills);  // Add to local list
                    skillsAdapter = new SkillsAdapter(AddSkillsActivity.this, allSkills, AddSkillsActivity.this::addSelectedSkill);
                    suggestionsRecyclerView.setAdapter(skillsAdapter);
                    skillsAdapter.filter(query); // Re-filter after adding

                    //show list and hide spinner
                    progressBar.setVisibility(View.GONE);
                    suggestionsRecyclerView.setVisibility(View.VISIBLE);

                    // Save newly fetched skills to Room
                    new Thread(() -> {
                        List<Skill> newSkills = new ArrayList<>();
                        for (String skill : skills) {
                            newSkills.add(new Skill(skill));
                        }
                        appDatabase.jobDao().insertSkills(newSkills);
                    }).start();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(AddSkillsActivity.this, "API Error: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }


    private void addSelectedSkill(String skill) {
        if (!selectedSkills.contains(skill)) {
            selectedSkills.add(skill);
            createSkillRadioButton(skill);
            suggestionsRecyclerView.setVisibility(View.GONE);
            searchView.setText("");
        }
    }

    private void createSkillRadioButton(String skill) {
        View chipView = LayoutInflater.from(this).inflate(R.layout.skill_item, selectedSkillsContainer, false);
        RadioButton skillButton = new RadioButton(this);
        skillButton.setText(skill);
        skillButton.setChecked(true);
        skillButton.setBackgroundResource(R.drawable.gender_selected);
        skillButton.setCompoundDrawablesWithIntrinsicBounds(null, null, closeIcon, null);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);
        skillButton.setLayoutParams(params);

// Optional styling
        skillButton.setTextSize(12);
        skillButton.setTextColor(ContextCompat.getColor(this, R.color.black));
        skillButton.setButtonDrawable(null); // Equivalent to android:button="@null"

// Handle click to remove
        skillButton.setOnClickListener(v -> {
            selectedSkills.remove(skill);
            selectedSkillsContainer.removeView(skillButton);
        });

// Add it directly to the selectedSkillsContainer
        selectedSkillsContainer.addView(skillButton);

    }
}

