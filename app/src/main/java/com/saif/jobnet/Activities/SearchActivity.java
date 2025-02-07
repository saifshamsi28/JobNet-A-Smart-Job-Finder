package com.saif.jobnet.Activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.saif.jobnet.Adapters.JobsAdapter;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Network.ApiService;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivitySearchBinding;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        EdgeToEdge.enable();

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Perform search based on the title, location, and preferences

                finJobsByTitleAndPreferences(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });


        binding.searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showPreferences(true);
                }
            }
        });

        binding.autoCompleteLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        binding.autoCompleteCompany.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        binding.autoCompleteJobType.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        binding.btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = binding.searchView.getQuery().toString().trim();
                if (!title.isEmpty()) {
                    finJobsByTitleAndPreferences(title);
                } else {
                    Toast.makeText(SearchActivity.this, "Please enter a job title", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.autoCompleteSalary.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });


//        //implement onbackpress
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {
                //set the visibility of the auto complete views to visible
                if(binding.preferencesCardView.getVisibility()==View.VISIBLE){
                    //default behaviour of backpressed is to finish the activity
                    showPreferences(false);
                }else {
                    if(!getOnBackPressedDispatcher().hasEnabledCallbacks()) {
                        getOnBackPressedDispatcher().onBackPressed();
                    }else {
                        finish();
                    }

                }

            }
        });

    }

    private void performSearch() {
        String title = binding.searchView.getQuery().toString().trim();
        if (!title.isEmpty()) {
            finJobsByTitleAndPreferences(title);
            showPreferences(false); // Hide preferences on search
        } else {
            Toast.makeText(this, "Please enter a job title", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPreferences(boolean b) {
        if(b){
            binding.preferencesCardView.setVisibility(View.VISIBLE);
            binding.searchedQuery.setVisibility(View.GONE);
//            Toast.makeText(this, "Show preferences fields", Toast.LENGTH_SHORT).show();
        }else{
            binding.preferencesCardView.setVisibility(View.GONE);
            binding.searchedQuery.setVisibility(View.VISIBLE);
//            Toast.makeText(this, "Hide preferences fields", Toast.LENGTH_SHORT).show();
        }
    }

    private void finJobsByTitleAndPreferences(String title) {
        binding.searchView.clearFocus();
        String location = binding.autoCompleteLocation.getText().toString().trim();
        String company=binding.autoCompleteCompany.getText().toString().trim();
        String jobType=binding.autoCompleteJobType.getText().toString().trim();
        String salaryInString=binding.autoCompleteSalary.getText().toString().trim();
        Integer salary= salaryInString.isEmpty() ? 0 : Integer.parseInt(salaryInString);
        System.out.println("SearchActivity: finJobsByTitleAndPreferences: " +
                "title: " +title+
                " location: " +location+
                " company: " +company+
                " jobType: " +jobType+
                " salary: "+salary);

        formatSearchedQuery(title,location,company,jobType,salaryInString);

        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl("http://10.162.1.53:8080")
                .client(new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService=retrofit.create(ApiService.class);

        apiService.fetchJobsByTitle(title,location,company,salary,jobType)
                .enqueue(new Callback<List<Job>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Job>> call, @NonNull Response<List<Job>> response) {
                        if(response.isSuccessful()){
                            List<Job> jobs=response.body();
                            if (jobs != null) {
                                binding.recyclerViewJobs.setVisibility(View.VISIBLE);
                                binding.noJobsFound.setVisibility(View.GONE);
                                JobsAdapter jobsAdapter=new JobsAdapter(SearchActivity.this,jobs);
                                binding.recyclerViewJobs.setAdapter(jobsAdapter);
                                binding.recyclerViewJobs.setLayoutManager(new LinearLayoutManager(SearchActivity.this));
                                showPreferences(false);

                                for(Job job:jobs){
                                    System.out.println(job.getTitle());
                                    System.out.println(job.getLocation());
                                    System.out.println(job.getCompany());
                                    System.out.println(job.getSalary());
                                }
                            }else {
                                System.out.println("No jobs found");
                            }
                        }else {
                            //if response code is not found
                            if(response.code()==404){
                                binding.recyclerViewJobs.setVisibility(View.GONE);
                                binding.noJobsFound.setVisibility(View.VISIBLE);
                                showPreferences(false);
                                binding.noJobsFound.setText("No jobs found for given preferences\nTry removing/modifying the preferences");
                                //set all the fiel
                                System.out.println("No jobs found");

                            }
                            Log.e("SearchActivity","Error in fetching jobs by title response code: "+response.code()+"response message: "+response.message());
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<Job>> call, @NonNull Throwable throwable) {
                        System.out.println("Error in fetching jobs by title: "+throwable.getMessage());
                        throwable.printStackTrace();
                    }
                });
    }

    private void formatSearchedQuery(String title, String location, String company, String jobType, String salary) {
        // Default values if fields are empty
        title = title.isEmpty() ? "Any title" : title;
        company = company.isEmpty() ? "Any company" : company;
        salary = salary.isEmpty() ? "Any salary" : salary + " LPA";
        location = location.isEmpty() ? "Any location" : location;
        jobType = jobType.isEmpty() ? "Any job type" : jobType;

        SpannableStringBuilder builder = new SpannableStringBuilder();

        // Helper function to format each line
        appendStyledText(builder, "Showing results for:\n", Color.BLACK, true);
        appendStyledText(builder, "Title: ", Color.BLUE, true);
        appendStyledText(builder, title + "\n", Color.DKGRAY, false);
        appendStyledText(builder, "Company: ", Color.BLUE, true);
        appendStyledText(builder, company + "\n", Color.DKGRAY, false);
        appendStyledText(builder, "Minimum Salary: ", Color.BLUE, true);
        appendStyledText(builder, salary + "\n", Color.DKGRAY, false);
        appendStyledText(builder, "Location: ", Color.BLUE, true);
        appendStyledText(builder, location + "\n", Color.DKGRAY, false);
        appendStyledText(builder, "Job Type: ", Color.BLUE, true);
        appendStyledText(builder, jobType, Color.DKGRAY, false);

        binding.searchedQuery.setText(builder);
    }

    // Helper function to apply color and bold style
    private void appendStyledText(SpannableStringBuilder builder, String text, int color, boolean isBold) {
        int start = builder.length();
        builder.append(text);
        int end = builder.length();

        builder.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (isBold) {
            builder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}