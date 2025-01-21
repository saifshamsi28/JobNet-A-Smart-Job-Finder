package com.saif.jobnet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.SaveJobsModel;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Network.ApiService;
import com.saif.jobnet.databinding.JobCardBinding;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.saif.jobnet.Activities.JobDetailActivity;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class JobsAdapter extends RecyclerView.Adapter<JobsAdapter.JobViewHolder> {

    private final List<Job> jobList;
    private final Context context;

    public JobsAdapter(Context context, List<Job> jobList) {
        this.context = context;
        this.jobList = jobList;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.job_card, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobList.get(position);
        holder.binding.jobTitle.setText(job.getTitle());
        holder.binding.companyName.setText(job.getCompany());
        holder.binding.location.setText(job.getLocation());
        String numericReview = job.getReview().replaceAll("[^\\d]", "");
        if(numericReview.isEmpty()){
            holder.binding.jobRating.setVisibility(View.GONE);
            holder.binding.ratingImg.setVisibility(View.GONE);
        }else {
            holder.binding.jobRating.setVisibility(View.VISIBLE);
            holder.binding.jobRating.setText(job.getRating());
            holder.binding.ratingImg.setVisibility(View.VISIBLE);
        }

        holder.binding.salary.setText(job.getSalary());
        holder.binding.description.setText(job.getDescription());
        holder.binding.postDate.setText(job.getPostDate());
        setJobReviews(holder.binding, job.getReview());

        holder.binding.saveJobs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveJobToBackend(job.getJobId(),holder.binding.saveJobs);
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //to go to job detail activity
                Intent intent = new Intent(context, JobDetailActivity.class);
                intent.putExtra("jobId", job.getJobId());
                intent.putExtra("url", job.getUrl());
                System.out.println("url to visit: "+job.getUrl());
                context.startActivity(intent);
            }});
    }

    private void saveJobToBackend(String jobId, ImageView saveJobs) {
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
        SharedPreferences sharedPreferences=context.getSharedPreferences("JobNetPrefs", Context.MODE_PRIVATE);
        String userId=sharedPreferences.getString("userId",null);
        String tag = (String) saveJobs.getTag();

        SaveJobsModel saveJobsModel;
        if (tag.equals("0")) {
            // Job is not saved; save the job
            saveJobs.setTag("1"); // Update the tag to reflect the new state
            saveJobs.setImageResource(R.drawable.job_saved_icon);
            saveJobsModel=new SaveJobsModel(userId,jobId,true);
            Toast.makeText(context, "Saving job", Toast.LENGTH_SHORT).show();

            // Call backend to save job here
        } else {
            // Job is saved; unsave the job
            saveJobs.setTag("0"); // Update the tag to reflect the new state
            saveJobs.setImageResource(R.drawable.job_not_saved_icon);
            saveJobsModel=new SaveJobsModel(userId,jobId,false);
            Toast.makeText(context, "Unsaving job", Toast.LENGTH_SHORT).show();

            // Call backend to unsave job her
        }
//        saveJobsModel=new SaveJobsModel(userId,jobId);
        System.out.println("before request: job id: "+saveJobsModel.getJobId()+" , user id: "+saveJobsModel.getUserId());
        Call<User> response=apiService.saveJobs(saveJobsModel);
        response.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                System.out.println("on response "+response);
                if(response.isSuccessful()){
                    User user=response.body();
                    if(user!=null){
                        System.out.println("Job saved successfully");
                        Toast.makeText(context,"Job saved successfully",Toast.LENGTH_SHORT).show();
                        saveJobs.setImageResource(R.drawable.job_saved_icon);
                        for(Job job:user.getSavedJobs()){
                            System.out.println("Job id: "+ job.getJobId()+" , title: "+job.getTitle());
                        }
                    }
                }else {
                    System.out.println("Error saving job");
                    System.out.println(response.message());
                    Toast.makeText(context,"Error saving job",Toast.LENGTH_SHORT).show();
                    saveJobs.setImageResource(R.drawable.job_not_saved_icon);
                    Log.d("error",response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable throwable) {
                System.out.println("Error saving job");
                Toast.makeText(context,"Error saving job",Toast.LENGTH_SHORT).show();
                saveJobs.setImageResource(R.drawable.job_not_saved_icon);
                System.out.println(throwable);
                throwable.printStackTrace();
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
        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = JobCardBinding.bind(itemView);
        }
    }
}
