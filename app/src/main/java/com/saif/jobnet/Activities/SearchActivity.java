package com.saif.jobnet.Activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.saif.jobnet.Adapters.JobsAdapter;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Api.ApiService;
import com.saif.jobnet.Models.RecentSearch;
import com.saif.jobnet.R;
import com.saif.jobnet.Utils.Config;
import com.saif.jobnet.databinding.ActivitySearchBinding;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
    private List<String> stringTitles;
    private int currentIndex = 0;
    private TextView searchHintText;
    private Handler handler = new Handler();
    private Runnable titleUpdater;
    private AppDatabase appDatabase;
//    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        progressDialog=new ProgressDialog(this);
        setTitle("Search Jobs");

        binding.searchView.requestFocus();
        appDatabase = DatabaseClient.getInstance(this).getAppDatabase();

        //to animate the hint of search view
        searchHintText = binding.searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchHintText != null) {
            searchHintText.setHint("Search \"Android Developer\"");
        }

        // Job titles list
        stringTitles = new ArrayList<>(Arrays.asList(
                "Web Developer", "Android Developer", "Java Developer",
                "Python Developer", "Flutter Developer", "iOS Developer",
                "Data Scientist", "Data Analyst", "Data Engineer",
                "Front-end Developer", "Back-end Developer"
        ));
            // Start the animation loop
            startTitleAnimation();

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Perform search based on the title, location, and preferences

                RecentSearch search = new RecentSearch();
                search.setQuery(query);
                search.setSearchedAt(LocalDateTime.now());

                new Thread(() -> {
                    appDatabase.jobDao().insertSearch(search);
                }).start();

                showPreferences(false);
                finJobsByTitleAndPreferences(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        //if searchview has some text then remove call back

//        binding.searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if (hasFocus) {
//                    showPreferences(true);
//                }
//            }
//        });

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

        binding.btnApplyFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = binding.searchView.getQuery().toString().trim();
//                if (!title.isEmpty()) {
                    showPreferences(false);
                    finJobsByTitleAndPreferences(title);
//                } else {
//                    Toast.makeText(SearchActivity.this, "Please enter a job title", Toast.LENGTH_SHORT).show();
//                }
            }
        });

        binding.autoCompleteSalary.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        binding.filters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show or hide the preference card view
                showPreferences(binding.preferencesCardView.getVisibility() != VISIBLE);
            }
        });

//        //implement on back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {
                //set the visibility of the auto complete views to visible
                if(binding.preferencesCardView.getVisibility()== VISIBLE){
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

    private void startTitleAnimation() {
        titleUpdater = new Runnable() {
            @Override
            public void run() {
                if (searchHintText == null) return;

                // Extract current hint (e.g., "Search "Android Developer"")
                String baseText = "Search ";

                // Create a translate animation (bottom to top)
                TranslateAnimation slideUp = new TranslateAnimation(0, 0, 100, -20);
                slideUp.setDuration(300);

                // Create fade-in animation
                AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
                fadeIn.setDuration(300);

                // Combine animations
                AnimationSet animationSet = new AnimationSet(true);
                animationSet.addAnimation(slideUp);
                animationSet.addAnimation(fadeIn);

                // Change the job title
                searchHintText.postDelayed(() -> {
                    searchHintText.setHint(baseText + "\"" + stringTitles.get(currentIndex) + "\"");

                    // Apply animation to the hint text
                    searchHintText.startAnimation(animationSet);
                    searchHintText.setTextSize(16);
                    // Move to the next title in a loop
                    currentIndex = (currentIndex + 1) % stringTitles.size();

                    // Repeat after 3 seconds
//                    handler.postDelayed(titleUpdater, 2500);
                    if(binding.searchView.getQuery().toString().isEmpty()) {
                        handler.postDelayed(titleUpdater, 2500);
                    }else {
                        handler.removeCallbacks(titleUpdater);
                    }
                }, 300); // Update text midway for smooth effect
            }
        };

        // Start the animation loop
        handler.post(titleUpdater);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(titleUpdater); // Stop handler when activity is destroyed
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
            //set top to down slide animation
            binding.preferencesCardView.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_top_to_bottom));
            binding.preferencesCardView.setVisibility(VISIBLE);
//            binding.searchedQuery.setVisibility(View.GONE);
//            Toast.makeText(this, "Show preferences fields", Toast.LENGTH_SHORT).show();
        }else{
            binding.preferencesCardView.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_bottom_to_top));
            binding.preferencesCardView.setVisibility(GONE);
//            binding.searchedQuery.setVisibility(View.VISIBLE);
//            Toast.makeText(this, "Hide preferences fields", Toast.LENGTH_SHORT).show();
        }
    }

    private void finJobsByTitleAndPreferences(String title) {
        setUpShimmer(true);
        binding.noJobsFound.setVisibility(GONE);
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
        String BASE_URL= Config.BASE_URL;
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl(BASE_URL)
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
                        setUpShimmer(false);
                        if(response.isSuccessful()){
                            List<Job> jobs=response.body();
                            if (jobs != null) {
                                binding.recyclerViewJobs.setVisibility(VISIBLE);
//                                binding.noJobsFound.setVisibility(View.GONE);
                                JobsAdapter jobsAdapter=new JobsAdapter(SearchActivity.this,jobs,"search");
                                binding.recyclerViewJobs.setAdapter(jobsAdapter);
                                binding.recyclerViewJobs.setLayoutManager(new LinearLayoutManager(SearchActivity.this));

                                for(Job job:jobs){
                                    System.out.println(job.getTitle());
                                    System.out.println(job.getLocation());
                                    System.out.println(job.getCompany());
                                    System.out.println(job.getSalary());
//                                    System.out.println(job.getJobType());
//                                    System.out.println(job.getUrl());
                                    System.out.println(job.getPostDate());
                                    System.out.println(job.getRating());
                                    System.out.println(job.getReview());
                                    System.out.println(job.getShortDescription());
//                                    System.out.println(job.getReviewsRating());
//                                    System.out.println(job.getReviewsCount());

                                }
                            }else {
                                System.out.println("No jobs found");
                            }
                        }else {
                            //if response code is not found
                            if(response.code()==404){
                                binding.recyclerViewJobs.setVisibility(GONE);
                                binding.noJobsFound.setVisibility(VISIBLE);
                                binding.noJobsFound.setText("No Jobs found for given preferences\ntry removing the preference");
//                                binding.noJobsFound.setText("No jobs found for given preferences\nTry removing/modifying the preferences");
                                System.out.println("No jobs found");
                            }
                            Log.e("SearchActivity","Error in fetching jobs by title response code: "+response.code()+"response message: "+response.message());
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<Job>> call, @NonNull Throwable throwable) {
                        System.out.println("Error in fetching jobs by title: "+throwable.getMessage());
                        setUpShimmer(false);
                        throwable.printStackTrace();
                    }
                });
    }

    private void setUpShimmer(boolean b) {
        if(b){
            binding.shimmerLayout.setVisibility(VISIBLE);
            binding.recyclerViewJobs.setVisibility(GONE);
            binding.shimmerLayout.startShimmer();
        }else {
            binding.shimmerLayout.setVisibility(GONE);
            binding.recyclerViewJobs.setVisibility(VISIBLE);
            binding.shimmerLayout.stopShimmer();
        }
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

//        binding.searchedQuery.setText(builder);
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