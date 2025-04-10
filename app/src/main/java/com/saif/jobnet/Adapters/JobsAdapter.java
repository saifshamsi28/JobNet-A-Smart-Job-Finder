package com.saif.jobnet.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.jobnet.Activities.ViewAllJobsActivity;
import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Database.JobDao;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.SaveJobsModel;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Api.ApiService;
import com.saif.jobnet.R;
import com.saif.jobnet.Utils.Config;
import com.saif.jobnet.databinding.JobCardBinding;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.saif.jobnet.Activities.JobDetailActivity;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class JobsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Job> jobList;
    private final Context context;
    private AppDatabase appDatabase;
    private JobDao jobDao;
    private User currentUser;
    private ProgressBar progressBar;
    private static final int VIEW_TYPE_JOB = 0;
    private static final int VIEW_TYPE_VIEW_ALL = 1;
    private String source;

    public JobsAdapter(Context context, List<Job> jobList,String source) {
        this.context = context;
        this.jobList = jobList;
        this.source=source;
        appDatabase= DatabaseClient.getInstance(context).getAppDatabase();
        jobDao = appDatabase.jobDao();
        Log.d("JobsAdapter", "Total items in adapter(from source: " + source + "): " + this.jobList.size());

        new Thread(new Runnable() {
            @Override
            public void run() {
                //get user id from shared preferences
                SharedPreferences sharedPreferences=context.getSharedPreferences("JobNetPrefs", Context.MODE_PRIVATE);
                String userId= sharedPreferences.getString("userId",null);
                currentUser = jobDao.getCurrentUser();
            }
        }).start();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == VIEW_TYPE_VIEW_ALL) {
            View view = inflater.inflate(R.layout.item_view_all, parent, false);
            return new ViewAllViewHolder(view);
        } else {
            JobCardBinding binding = JobCardBinding.inflate(inflater, parent, false);
            return new JobViewHolder(binding);
        }
    }



    @Override
    public int getItemViewType(int position) {
        Job job = jobList.get(position);
        return (job.getUrl() == null) ? VIEW_TYPE_VIEW_ALL : VIEW_TYPE_JOB;
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder1, int position) {
        Job job = jobList.get(position);

        if (getItemViewType(position) == VIEW_TYPE_VIEW_ALL) {
            holder1.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ViewAllJobsActivity.class);
                intent.putExtra("source", source);
                context.startActivity(intent);
            });
            return;
        }

        Log.d("JobsAdapter", "Binding position: " + position);

        JobViewHolder holder = (JobViewHolder) holder1;

        holder.binding.jobTitle.setText(job.getTitle());
        holder.binding.companyName.setText(job.getCompany());
        holder.binding.location.setText(job.getLocation());

        progressBar=holder.binding.savedJobsProgressbar;
        String numericReview = job.getReview().replaceAll("[^\\d]", "");
        if(numericReview.isEmpty()){
            holder.binding.jobRating.setVisibility(View.GONE);
            holder.binding.ratingImg.setVisibility(View.GONE);
            holder.binding.companyName.setMaxWidth(holder.binding.shortDescription.getWidth());
        }else {
            holder.binding.jobRating.setVisibility(View.VISIBLE);
            holder.binding.jobRating.setText(job.getRating());
            holder.binding.ratingImg.setVisibility(View.VISIBLE);
        }

        holder.binding.salary.setText(job.getSalary());
        holder.binding.shortDescription.setText(job.getShortDescription());
        holder.binding.postDate.setText(job.getPostDate());
        setJobReviews(holder.binding, job.getReview());

        holder.binding.saveJobs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
                holder.binding.saveJobs.setVisibility(View.GONE);
                //check user is logged in or not and save job to backend
                if(currentUser==null){
                    Toast.makeText(context, "Login first to save the job", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    progressBar.setIndeterminate(false);
                    holder.binding.saveJobs.setVisibility(View.VISIBLE);
                    return;
                }
                saveJobToBackend(job,holder.binding.saveJobs);
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //to go to job detail activity
                Intent intent = new Intent(context, JobDetailActivity.class);
                intent.putExtra("jobId", job.getJobId());
                intent.putExtra("url", job.getUrl());
                System.out.println("url to visit: "+ job.getUrl());
                context.startActivity(intent);
            }});

        // Set animation when a job item appears
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View view, int position) {
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.slide_top_to_bottom);
        view.startAnimation(animation);
    }
    private void saveJobToBackend(Job job, ImageView saveJobs) {
        String BASE_URL = Config.BASE_URL;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(new OkHttpClient()
                        .newBuilder().connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService=retrofit.create(ApiService.class);
        String userId= currentUser.getId();
        String tag = (String) saveJobs.getTag();
        SaveJobsModel saveJobsModel;

        if (tag.equals("0")) {
            // Job is not saved; save the job
            saveJobsModel=new SaveJobsModel(userId, job.getJobId(),true);
        } else {
            // Job is saved; unsave the job
            saveJobsModel=new SaveJobsModel(userId, job.getJobId(), false);
        }
//        saveJobsModel=new SaveJobsModel(userId,stringId);
        System.out.println("before request: job id: "+saveJobsModel.getJobId()+" , user id: "+saveJobsModel.getUserId());
        Call<ResponseBody> response=apiService.saveJobs(saveJobsModel);
        response.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                progressBar.setIndeterminate(false);
                saveJobs.setVisibility(View.VISIBLE);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseMessage = response.body().string(); // Extract plain text
                        System.out.println("responseMessage: "+responseMessage);
                        //split the response by title
                        String[] parts = responseMessage.split("title:");
                        System.out.println("responseTitle: "+parts[1]);
                        if(responseMessage.contains("saved")){
                            Toast.makeText(context, "Successfully saved the job: " + parts[1], Toast.LENGTH_SHORT).show();
                            saveJobs.setTag("1");
                            saveJobs.setImageResource(R.drawable.job_saved_icon);
                            //save job in user in room database
                            currentUser.getSavedJobs().add(job);
                        }else {
                            saveJobs.setTag("0");
                            saveJobs.setImageResource(R.drawable.job_not_saved_icon);
                            Toast.makeText(context, "Successfully removed the job: " + parts[1], Toast.LENGTH_SHORT).show();
                            //remove job from user in room database
                            currentUser.getSavedJobs().remove(job);
                        }
                        currentUser.setSavedJobs(currentUser.getSavedJobs());
                        new Thread(() -> jobDao.insertOrUpdateUser(currentUser)).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Error saving job", Toast.LENGTH_SHORT).show();
                    System.out.println("response: "+ response);
                }
            }
            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Toast.makeText(context, "Error saving job", Toast.LENGTH_SHORT).show();
                throwable.printStackTrace();
                progressBar.setVisibility(View.GONE);
                progressBar.setIndeterminate(false);
                saveJobs.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return jobList.size();
    }

    private void setJobReviews(JobCardBinding binding, String review) {
        if (review != null) {
            // Use regex to extract only numbers from the review string
            String numericReview = review.replaceAll("[^\\d]", ""); // Remove all non-digit characters

            if (!numericReview.isEmpty()) {
                numericReview=numericReview.trim()+" Reviews";
                // Set only the numerical part if it exists
                binding.reviews.setVisibility(View.VISIBLE);
                binding.reviews.setText(numericReview);
            } else {
                // Optionally, set an empty string or some placeholder if no numerical value is present
                binding.reviews.setVisibility(View.GONE);
            }
        }
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        JobCardBinding binding;
        public JobViewHolder(JobCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ViewAllViewHolder extends RecyclerView.ViewHolder {
        public ViewAllViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setForegroundGravity(Gravity.CENTER);
        }
    }

}
