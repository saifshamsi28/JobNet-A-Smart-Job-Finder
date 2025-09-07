package com.saif.jobnet.Activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.saif.jobnet.Utils.Config.BASE_URL;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.saif.jobnet.Models.AuthResponse;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.RecentSearch;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Utils.Config;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Database.JobDao;
import com.saif.jobnet.Adapters.JobsAdapter;
import com.saif.jobnet.Api.ApiService;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityMainBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final float END_SCALE = 0.7f;
    private ActivityMainBinding binding;
    private final ArrayList<String> stringTitles = new ArrayList<>();
    ProgressDialog progressDialog;
    private long startTime;
    private long endTime;
    private AppDatabase appDatabase;
    private JobDao jobDao;
    private User user;
    private TextView drawerUserName;
    private TextView drawerUserEmail;
    private ImageView drawerProfileImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setStatusBarColor(ContextCompat.getColor(this,R.color.colorActionBarBackground));
        //to set the color of items of status bar black
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        binding.recyclerViewSuggestedJobs.setVisibility(GONE);
        binding.recyclerViewRecentJobs.setVisibility(GONE);

        appDatabase=DatabaseClient.getInstance(this).getAppDatabase();
        jobDao=appDatabase.jobDao();

        binding.viewAllNewJobs.setText(Html.fromHtml("<u>View all</u>", Html.FROM_HTML_MODE_LEGACY));
        binding.viewAllNewJobs.setText(Html.fromHtml("<u>View all</u>", Html.FROM_HTML_MODE_LEGACY));
        binding.viewAllNewJobs.setText(Html.fromHtml("<u>View all</u>", Html.FROM_HTML_MODE_LEGACY));

        //to set navigation drawer
        setNavigationDrawer();

        //show shimmer effect until jobs load
        setShimmerEffect();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Check if the drawer is open
                if (binding.drawerLayout.isDrawerOpen(binding.navigationView)) {
                    // Close the drawer if it's open
                    binding.drawerLayout.closeDrawer(binding.navigationView);
                } else {
                    // If the drawer is closed, proceed with the default back action
                    setEnabled(false); // Disable this callback to avoid looping
                    getOnBackPressedDispatcher().onBackPressed(); // Trigger the default back behavior
                }
            }
        });

        binding.menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                getWindow().setStatusBarColor(ContextCompat.getColor(MainActivity.this,R.color.white));
                binding.drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        //setting up different job titles
        Collections.addAll(stringTitles, "Web Developer", "Android Developer", "Java Developer",
                "Python Developer", "Flutter Developer", "IOS Developer", "Data Scientist", "Data Analyst", "Data Engineer"
                , "Front-end Developer", "Back-end Developer");

        //show the jobs for different sections
        new Thread(new Runnable() {
            @Override
            public void run() {
                user = appDatabase.jobDao().getCurrentUser();
                //sync data from server
                showSuggestedJobs();
                showRecentJobs();
                showNewJobs();

                if(user!=null) {
                    syncUserDataWithServer(user.getId());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setShimmerEffect();
                            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                                Uri profileImageUri = Uri.parse(user.getProfileImage());
                                Glide.with(MainActivity.this)
                                        .load(profileImageUri)
                                        .circleCrop()
                                        .into(drawerProfileImage);
                                drawerUserName.setText(user.getName());
                                drawerUserEmail.setText(user.getEmail());
                            }
                        }
                    });
                }
            }
        }).start();

        // Initialize the database
        appDatabase = DatabaseClient.getInstance(this).getAppDatabase();
        jobDao = appDatabase.jobDao();

        // Start showing titles
        displayJobTitles();
        progressDialog = new ProgressDialog(this);

        // Disable focus and keyboard from triggering in this activity
        binding.searchView.setFocusable(false);
        binding.searchView.setClickable(true);

        binding.searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,SearchActivity.class);
                startActivity(intent);
            }
        });

        binding.searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                Intent intent=new Intent(MainActivity.this,SearchActivity.class);
                startActivity(intent);
            }
        });

        binding.viewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, JobListActivity.class);
                intent.putExtra("source", "history button");
                startActivity(intent);
            }
        });

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
                if (R.id.nav_home==item.getItemId()){
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    startActivity(intent);
                    finishAffinity();
                } else if (R.id.nav_search==item.getItemId()){
                    Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                    startActivity(intent);
                } else if (R.id.nav_saved_jobs==item.getItemId()) {
//                    Toast.makeText(MainActivity.this, "Saved Jobs", Toast.LENGTH_SHORT).show();
                    // Replace the fragment container with SavedJobsFragment
                    Intent intent = new Intent(MainActivity.this, SavedJobsActivity.class);
                    intent.putExtra("source", "bottom navigation");
                    startActivity(intent);
                } else if (R.id.nav_profile==item.getItemId()){
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                }
            return false;
        });

        binding.viewAllSuggestedJobs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ViewAllJobsActivity.class);
                intent.putExtra("source", "suggested");
                startActivity(intent);
            }
        });

        binding.viewAllRecentJobs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ViewAllJobsActivity.class);
                intent.putExtra("source", "recent");
                startActivity(intent);
            }
        });

        binding.viewAllNewJobs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ViewAllJobsActivity.class);
                intent.putExtra("source", "new openings");
                startActivity(intent);
            }
        });
    }

    private void syncUserDataWithServer(String id) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(new OkHttpClient
                        .Builder().connectTimeout(10, TimeUnit.SECONDS)
                        .callTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService=retrofit.create(ApiService.class);
        //fetch jwt token from shared prefs....
        SharedPreferences sharedPreferences=getSharedPreferences("JobNetPrefs",MODE_PRIVATE);
        String jwtToken=sharedPreferences.getString("jwtToken",null);
        Call<User> response=apiService.getLoggedInUser("Bearer "+jwtToken);
        response.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if(response.isSuccessful()){
                    User user1=response.body();
                    if(user1!=null && user!=null){
//                        System.out.println("previous user basic details: "+user);
                        user=user1;
                        //save to local database
                        new Thread(() -> jobDao.insertOrUpdateUser(user)).start();
//                        System.out.println("updated user basic details: "+user.getBasicDetails());
//                        Toast.makeText(ProfileActivity.this, "Profile synchronised Successfully", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Log.d("MainActivity", "Error synchronizing user details");
                    try{
                        if(response.errorBody()!=null){
                            AuthResponse errorResponse=new Gson().fromJson(response.errorBody().string(),AuthResponse.class);
                            Log.d("MainActivity", "Error synchronizing user details: "+errorResponse);
                        }
                    }catch (IOException e){
                        e.printStackTrace();

                    }
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable throwable) {
                Log.e("MainActivity", "Error synchronizing user details: "+throwable);
                Toast.makeText(MainActivity.this, "Error synchronizing user details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setNavigationDrawer() {
        binding.navigationView.setCheckedItem(R.id.home);
        binding.navigationView.bringToFront();

        // Get navigation menu and shared preferences
        Menu menu = binding.navigationView.getMenu();
        SharedPreferences sharedPreferences = getSharedPreferences("JobNetPrefs", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);


        // Show or hide Login and Profile items based on login status
        MenuItem loginItem = menu.findItem(R.id.login);
        MenuItem logoutItem = menu.findItem(R.id.logout);
        MenuItem profileItem = menu.findItem(R.id.profile);
        View headerView = binding.navigationView.getHeaderView(0);
        drawerUserName = headerView.findViewById(R.id.user_name);
        drawerUserEmail = headerView.findViewById(R.id.user_email);
        drawerProfileImage = headerView.findViewById(R.id.app_logo);

        drawerUserName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        if (isLoggedIn) {
            loginItem.setVisible(false); // Hide Login
            profileItem.setVisible(true); // Show Profile
            logoutItem.setVisible(true);
            String name = sharedPreferences.getString("name", "");
            String userEmail = sharedPreferences.getString("userEmail", "");
            drawerUserName.setText(name);
            drawerUserEmail.setText(userEmail);
        } else {
            loginItem.setVisible(true);
            logoutItem.setVisible(false);
            profileItem.setVisible(false);
            drawerUserName.setText("JobNet");
            drawerUserEmail.setText("A Smart Job Finder");
            drawerProfileImage.setImageResource(R.drawable.account_img);
        }
        binding.navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.home){
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    startActivity(intent);
                }else if(item.getItemId() == R.id.job_list){
                    Intent intent = new Intent(MainActivity.this, JobListActivity.class);
                    intent.putExtra("source", "navigation drawer");
                    startActivity(intent);
                }else if(item.getItemId() == R.id.new_openings){
                    Intent intent = new Intent(MainActivity.this, ViewAllJobsActivity.class);
                    intent.putExtra("source", "new openings");
                    startActivity(intent);
//                    Toast.makeText(MainActivity.this, "New Openings Coming Soon", Toast.LENGTH_SHORT).show();
                }else if(item.getItemId() == R.id.login){
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                }else if(item.getItemId() == R.id.profile){
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                }else if(item.getItemId() == R.id.logout){
                    //method to pop up a dialogue to confirm the logout
                    logoutConfirmationDialogue();
                }
                return true;
            }
        });

        binding.drawerLayout.setScrimColor(ContextCompat.getColor(this, R.color.colorActionBarBackground));
        binding.drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // Scale the View based on current slide offset
                final float diffScaledOffset = slideOffset * (1 - END_SCALE);
                final float offsetScale = 1 - diffScaledOffset;
                binding.homeContent.setScaleX(offsetScale);
                binding.homeContent.setScaleY(offsetScale);
                // Translate the View, accounting for the scaled width
                final float xOffset = drawerView.getWidth() * slideOffset;
                final float xOffsetDiff = binding.homeContent.getWidth() * diffScaledOffset / 2;
                final float xTranslation = xOffset - xOffsetDiff;
                binding.homeContent.setTranslationX(xTranslation);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Handle drawer opened event
                getWindow().setStatusBarColor(ContextCompat.getColor(MainActivity.this,R.color.white));
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                // Handle drawer closed event
                getWindow().setStatusBarColor(ContextCompat.getColor(MainActivity.this,R.color.colorActionBarBackground));
                binding.navigationView.setCheckedItem(R.id.home);
            }
        });
    }

    private void setShimmerEffect() {
        //check if shimmer is running or not
        if (binding.shimmerViewContainerSuggested.isShimmerStarted()) {
            binding.shimmerViewContainerSuggested.stopShimmer();
            binding.shimmerViewContainerRecent.stopShimmer();
            binding.shimmerViewContainerSuggested.setVisibility(GONE);
            binding.shimmerViewContainerRecent.setVisibility(GONE);
            binding.recyclerViewSuggestedJobs.setVisibility(VISIBLE);
            binding.recyclerViewRecentJobs.setVisibility(VISIBLE);
            binding.shimmerViewContainerNew.setVisibility(GONE);
            binding.recyclerViewNewJobs.setVisibility(VISIBLE);
        } else {
            binding.shimmerViewContainerSuggested.setVisibility(VISIBLE);
            binding.shimmerViewContainerRecent.setVisibility(VISIBLE);
            binding.recyclerViewRecentJobs.setVisibility(GONE);
            binding.recyclerViewSuggestedJobs.setVisibility(GONE);
            binding.recyclerViewNewJobs.setVisibility(GONE);
            binding.shimmerViewContainerSuggested.startShimmer();
            binding.shimmerViewContainerRecent.startShimmer();
            binding.shimmerViewContainerNew.startShimmer();
        }
    }

    private void displayJobTitles() {
        new Thread(() -> {
            while (true) {
                for (String title : stringTitles) {
                    // Build the title character by character
                    StringBuilder displayedTitle = new StringBuilder("Search ");
                    // Create a SpannableString for styling
                    SpannableString spannableString = new SpannableString(displayedTitle.toString());

                    // Setting different color for "search"
                    spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSearch)), 0, 6, 0);

                    for (char c : title.toCharArray()) {
                        displayedTitle.append(c); // Adding one character at a time for typing effect
                        if (title.length() == displayedTitle.length() - 7) {
                            displayedTitle.append(" !!");
                        }
                        // Updating the spannable string with the job title
                        spannableString = new SpannableString(displayedTitle.toString());
                        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSearch)), 0, 6, 0);
                        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorJobTitle)), 7, displayedTitle.length(), 0);

                        SpannableString finalSpannableString = spannableString;
                        runOnUiThread(() -> binding.jobTitles.setText(finalSpannableString)); // Update UI

                        try {
                            Thread.sleep(30); // Time delay for typing effect
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    // Wait before clearing the title
                    try {
                        Thread.sleep(1000); // To show the full title for 1 second
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // Clear the title after showing
                    while (displayedTitle.length() > 6) {
                        displayedTitle.deleteCharAt(displayedTitle.length() - 1); // Remove one character at a time
                        spannableString = new SpannableString(displayedTitle.toString());
                        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSearch)), 0, 6, 0);
                        SpannableString finalSpannableString1 = spannableString;
                        runOnUiThread(() -> {
                            binding.jobTitles.setText(finalSpannableString1);
                            //adding cursor to the end of the text
                            binding.jobTitles.setCursorVisible(true);
                        }); // Update UI
                        try {
                            Thread.sleep(30); // Time delay for typing effect
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    // Wait a bit before starting the next title
                    try {
                        Thread.sleep(500); // Pause before starting the next title
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


            }
        }).start();
    }

    // Function to fetch job data from API
    private void fetchJobs(String query, String home) {
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
        Call<List<Job>> call;
        if (home.equals("home")) {
            call = apiService.showJobs();
        } else {
            call = apiService.searchJobs(query);
        }
        call.enqueue(new Callback<List<Job>>() {
            @Override
            public void onResponse(@NonNull Call<List<Job>> call, @NonNull Response<List<Job>> response) {
//                setShimmerEffect();
                if (response.isSuccessful()) {
                    List<Job> jobs = response.body();
                    if (jobs != null) {
                        Log.d("API Response", "Received " + jobs.size() + " jobs");
                        ;
                        endTime = System.currentTimeMillis();
                        Log.d("API Response", "Time taken: " + TimeUnit.MILLISECONDS.toSeconds(endTime - startTime) + " seconds");
                        populateTableWithJobs(jobs, query);
                    } else
                        Log.d("API Response", "No jobs found");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Job>> call, @NonNull Throwable t) {
                Log.e("API Error", "Failed to connect to spring boot server \n"+t);
                setShimmerEffect();

                Toast.makeText(MainActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //    // Function to dynamically add rows to the TableLayout
    private void populateTableWithJobs(List<Job> jobs, String query) {
        setShimmerEffect();
        JobsAdapter jobsAdapter = new JobsAdapter(this, jobs,"home");
        binding.recyclerViewRecentJobs.setAdapter(jobsAdapter);
//        binding.recyclerViewSuggestedJobs.setAdapter(jobsAdapter);
        binding.recyclerViewRecentJobs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerViewSuggestedJobs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

//        new Thread(() -> {
//            jobDao.insertAllJobs(jobs);
//            // Load Recent Jobs
//            List<RecentSearch> recentSearches = appDatabase.jobDao().getRecentSearches();
//            showSuggestedJobs();
//
//            if (!recentSearches.isEmpty()) {
//                List<Job> allRecentJobs = new ArrayList<>();
//                for (RecentSearch search : recentSearches) {
//                    List<Job> jobsMatched = jobDao.getJobsByTitle(search.query); // matches title
//                    allRecentJobs.addAll(jobsMatched);
//                }
//
//                runOnUiThread(() -> {
//                    LayoutAnimationController controller =
//                            AnimationUtils.loadLayoutAnimation(this, R.anim.fall_down_anim);
//                    binding.recyclerViewRecentJobs.setLayoutAnimation(controller);
//                    binding.recyclerViewRecentJobs.scheduleLayoutAnimation();
//                    JobsAdapter jobsAdapter = new JobsAdapter(this, allRecentJobs);
//                    binding.recyclerViewRecentJobs.setAdapter(jobsAdapter);
//                });
//            } else {
//                // Fallback to Suggested Jobs
//                runOnUiThread(this::showSuggestedJobs);
//            }
//        }).start();

        LayoutAnimationController controller =
                AnimationUtils.loadLayoutAnimation(this, R.anim.fall_down_anim);
        binding.recyclerViewRecentJobs.setLayoutAnimation(controller);
        binding.recyclerViewSuggestedJobs.setLayoutAnimation(controller);
        binding.recyclerViewRecentJobs.scheduleLayoutAnimation();
        binding.recyclerViewSuggestedJobs.scheduleLayoutAnimation();
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
            public void onResponse(@NonNull Call<List<Job>> call, @NonNull Response<List<Job>> response) {
                if (response.isSuccessful()) {
                    List<Job> jobs = response.body();
                    if (jobs != null) {
                        Log.d("API Response", "New jobs Received " + jobs.size() + " jobs");

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
//                                int sizeBefore=allJobs.size();
                                jobDao.insertAllJobs(jobs);
//                                allJobs=jobDao.getAllJobs();
//                                int sizeAfter=allJobs.size();
//                                System.out.println("new jobs added: "+(sizeAfter-sizeBefore));
                            }
                        }).start();
                        //set up new jobs section

                        List<Job> displayedJobs = new ArrayList<>();
                        for (int i = 0; i < Math.min(jobs.size(), 10); i++) {
                            displayedJobs.add(jobs.get(i));
                        }

                        // Add dummy "View All" item
                        Job viewAllJob = new Job();
                        viewAllJob.setJobId("123456789");
                        viewAllJob.setUrl(null); // This triggers the view type
                        displayedJobs.add(viewAllJob);

                        JobsAdapter jobsAdapter = new JobsAdapter(MainActivity.this, displayedJobs,"new openings");
                        binding.recyclerViewNewJobs.setAdapter(jobsAdapter);
                        binding.recyclerViewNewJobs.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                        binding.recyclerViewNewJobs.setVisibility(VISIBLE);
                    }else{
                        Log.d("API Response", "No jobs found");

                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Job>> call, @NonNull Throwable throwable) {
                Log.e("API Error", "Failed to connect to spring boot server "+throwable);
                Toast.makeText(MainActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRecentJobs() {
        new Thread(() -> {
//            jobDao.insertAllJobs(jobs);
            // Load Recent Jobs
            List<RecentSearch> recentSearches = appDatabase.jobDao().getRecentSearches();

            if (!recentSearches.isEmpty()) {
                List<Job> allRecentJobs = new ArrayList<>();
                int recentSearchesSize = recentSearches.size();

                for (RecentSearch search : recentSearches) {
                    if(recentSearchesSize>10) {
                        allRecentJobs.addAll(jobDao.getJobsByTitle(search.query,3));
                    } else if(recentSearchesSize>5){
                        allRecentJobs.addAll(jobDao.getJobsByTitle(search.query,5));
                    }else{
                        allRecentJobs.addAll(jobDao.getJobsByTitle(search.query,10));
                    }
//                    System.out.println("recent search: "+search+" ,new size after adding related jobs "+allRecentJobs.size());
                    if(allRecentJobs.size()==10){
                        // Add dummy "View All" item
                        break;
                    }
                }

                Job viewAllJob = new Job();
                viewAllJob.setJobId("123456789");
                viewAllJob.setUrl(null);
                allRecentJobs.add(viewAllJob);

                runOnUiThread(() -> {
                    LayoutAnimationController controller =
                            AnimationUtils.loadLayoutAnimation(this, R.anim.fall_down_anim);
                    binding.recyclerViewRecentJobs.setLayoutAnimation(controller);
                    binding.recyclerViewRecentJobs.scheduleLayoutAnimation();
//                    System.err.println("final size of recent jobs: "+allRecentJobs.size());
                    JobsAdapter jobsAdapter = new JobsAdapter(this, allRecentJobs,"recent");
                    binding.recyclerViewRecentJobs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                    binding.recyclerViewRecentJobs.setAdapter(jobsAdapter);
                    binding.recyclerViewRecentJobs.setForegroundGravity(Gravity.CENTER);
                });
            } else {
                // Fallback to Suggested Jobs
                runOnUiThread(this::showNewJobs);
            }
        }).start();
    }

    private void showSuggestedJobs() {
        new Thread(() -> {
            User user = jobDao.getCurrentUser();
            List<Job> allJobs = jobDao.getAllJobs();

            if (user != null && user.getSkills() != null && !user.getSkills().isEmpty()) {
                Set<Job> suggestedJobsSet = new LinkedHashSet<>(); // To avoid duplicates
                int jobsPerSkill = 3;

                for (String skill : user.getSkills()) {
                    int count = 0;
                    for (Job job : allJobs) {
                        String content = (job.getFullDescription() + " " + job.getShortDescription());
                        if (matchesSkill(content, skill)) {
                            if (suggestedJobsSet.add(job)) {
                                count++;
                            }
                        }
                        if (count >= jobsPerSkill) break;
                    }
                }

                // Add "View All" dummy job if needed
                List<Job> suggestedJobs = new ArrayList<>(suggestedJobsSet);
                if (!suggestedJobs.isEmpty()) {
                    Job viewAllJob = new Job();
                    viewAllJob.setJobId("123456789");
                    viewAllJob.setUrl(null);
                    suggestedJobs.add(viewAllJob);
                }

                runOnUiThread(() -> {
                    JobsAdapter adapter = new JobsAdapter(this, suggestedJobs, "suggested");
                    binding.recyclerViewSuggestedJobs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                    binding.recyclerViewSuggestedJobs.setAdapter(adapter);
                    binding.recyclerViewSuggestedJobs.scheduleLayoutAnimation();
                    binding.recyclerViewSuggestedJobs.setVisibility(VISIBLE);
                });
            } else {
                runOnUiThread(() -> {
                    List<Job> displayedJobs = allJobs.subList(0, Math.min(10, allJobs.size()));
                    Job viewAllJob = new Job();
                    viewAllJob.setJobId("123456789");
                    viewAllJob.setUrl(null);
                    displayedJobs.add(viewAllJob);

                    JobsAdapter adapter = new JobsAdapter(this, displayedJobs, "suggested");
                    binding.recyclerViewSuggestedJobs.setAdapter(adapter);
                    binding.recyclerViewSuggestedJobs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                    binding.recyclerViewSuggestedJobs.scheduleLayoutAnimation();
                    binding.recyclerViewSuggestedJobs.setVisibility(VISIBLE);
                });
            }
        }).start();
    }

    private boolean matchesSkill(String content, String skill) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(skill) + "\\b", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(content).find();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_screen_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        setNavigationDrawer();
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.profile) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutConfirmationDialogue(){
        Dialog dialog=new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.confirmation_dialogue_layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }
        dialog.show();
        TextView confirmButton = dialog.findViewById(R.id.confirm_button);
        TextView dismissButton = dialog.findViewById(R.id.dismiss_button);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                SharedPreferences sharedPreferences = getSharedPreferences("JobNetPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isLoggedIn", false);
                editor.apply();
                setNavigationDrawer();
                dialog.dismiss();
            }
        });
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
}