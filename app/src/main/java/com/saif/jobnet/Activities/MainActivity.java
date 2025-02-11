package com.saif.jobnet.Activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.navigation.NavigationView;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Utils.Config;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Database.JobDao;
import com.saif.jobnet.Adapters.JobsAdapter;
import com.saif.jobnet.Network.ApiService;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final float END_SCALE = 0.7f;
    private ActivityMainBinding binding;
    private SearchView searchView;
    private final ArrayList<String> stringTitles = new ArrayList<>();
    ProgressDialog progressDialog;
    private long startTime;
    private long endTime;
    private AppDatabase appDatabase;
    private JobDao jobDao;
    private List<Job> savedJobs =new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setStatusBarColor(ContextCompat.getColor(this,R.color.white));
        //to set the color of items of status bar black
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        TextView jobTitlesTextView = findViewById(R.id.job_title);
        searchView = findViewById(R.id.search_view);

        binding.recyclerViewJobs.setVisibility(View.GONE);
        binding.shimmerViewContainer.setVisibility(View.GONE);

        //to set navigation drawer
        setNavigationDrawer();

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
                binding.drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        //setting up different job titles
        Collections.addAll(stringTitles, "Web Developer", "Android Developer", "Java Developer",
                "Python Developer", "Flutter Developer", "IOS Developer", "Data Scientist", "Data Analyst", "Data Engineer"
                , "Front-end Developer", "Back-end Developer");


        //to select random title from list
        Random random = new Random();
        int randomIndex = random.nextInt(stringTitles.size());
        Log.d("MainActivity", "Title selected: " + stringTitles.get(randomIndex));
        setShimmerEffect();
        System.out.println("fetching this job for home: "+ stringTitles.get(randomIndex));
        fetchJobs(stringTitles.get(randomIndex), "home");
        // Initialize the database
        appDatabase = DatabaseClient.getInstance(this).getAppDatabase();
        jobDao = appDatabase.jobDao();

        //to check database has any item or not
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                boolean isEmpty = jobDao.getAllJobs().isEmpty();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (isEmpty) {
//                            binding.viewHistory.setVisibility(View.GONE);
//                        } else {
//                            binding.viewHistory.setVisibility(View.VISIBLE);
//                        }
//                    }
//                });
//            }
//        }).start();

        // Start showing titles
        displayJobTitles();
        progressDialog = new ProgressDialog(this);

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Start shimmer animation
                setShimmerEffect();
                //first fetch from local database if not found then send api request
                new Thread(() -> {
                    List<Job> jobs = jobDao.getJobsByTitle(query.trim());
                    Log.d("MainActivity", "Jobs found in database: " + jobs.size());
                    if (jobs.isEmpty()) {
                        fetchJobs(query, "search bar");
                    } else {
                        runOnUiThread(() -> populateTableWithJobs(jobs, query));
                    }
                }).start();

                fetchJobs(query,"search bar");
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                return false;
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
        TextView usernameTextView = headerView.findViewById(R.id.user_name);
        TextView userEmailTextView = headerView.findViewById(R.id.user_email);

        if (isLoggedIn) {
            loginItem.setVisible(false); // Hide Login
            profileItem.setVisible(true); // Show Profile
            logoutItem.setVisible(true);
            String name = sharedPreferences.getString("name", "");
            String userEmail = sharedPreferences.getString("userEmail", "");
            usernameTextView.setText(name);
            userEmailTextView.setText(userEmail);
        } else {
            loginItem.setVisible(true);
            logoutItem.setVisible(false);
            profileItem.setVisible(false);
            usernameTextView.setText("JobNet");
            userEmailTextView.setText("A Smart Job Finder");

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
//                    Intent intent = new Intent(MainActivity.this, NewOpeningsActivity.class);
//                    startActivity(intent);
                    Toast.makeText(MainActivity.this, "New Openings Coming Soon", Toast.LENGTH_SHORT).show();
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
        });
    }

    private void setShimmerEffect() {
        //check if shimmer is running or not
        if (binding.shimmerViewContainer.isShimmerStarted()) {
            binding.shimmerViewContainer.stopShimmer();
            binding.shimmerViewContainer.setVisibility(View.GONE);
            binding.recyclerViewJobs.setVisibility(View.VISIBLE);
        } else {
            binding.shimmerViewContainer.setVisibility(View.VISIBLE);
            binding.recyclerViewJobs.setVisibility(View.GONE);
            binding.shimmerViewContainer.startShimmer();
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
//                        for(Job job:jobs){
//                            //printing all the details of jobs
//                            System.out.println("job id: "+job.getJobId()+" , \njob title: "+job.getTitle()+" , " +
//                                    "\njob company: "+job.getCompany()+"\nlocation: "+job.getLocation()+"\nsalary: "+job.getSalary()
//                            +"\n min salary: "+job.getMinSalary()+"\n max salary: "+job.getMaxSalary()+"\n");
//                        }
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
//        for(int i=0;i<10;i++){
//            savedJobs.add(jobs.get(i));
//        }
        JobsAdapter jobsAdapter = new JobsAdapter(this, jobs);
        binding.recyclerViewJobs.setAdapter(jobsAdapter);
//        binding.recyclerViewJobs.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerViewJobs.setLayoutManager(new LinearLayoutManager(this));
        new Thread(() -> jobDao.insertAllJobs(jobs)).start();
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