package com.saif.jobnet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
                saveJobToBackend(job.getJobId());
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //to go to job detail activity
                Intent intent = new Intent(context, JobDetailActivity.class);
                intent.putExtra("url", job.getUrl());
                context.startActivity(intent);
            }});
    }

    private void saveJobToBackend(String jobId) {
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl("http://10.162.1.53:8080/")
                .client(new OkHttpClient()
                        .newBuilder().connectTimeout(10, TimeUnit.MILLISECONDS)
                        .readTimeout(10, TimeUnit.MILLISECONDS)
                        .writeTimeout(10, TimeUnit.MILLISECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService=retrofit.create(ApiService.class);
        SharedPreferences sharedPreferences=context.getSharedPreferences("JobNetPrefs", Context.MODE_PRIVATE);
        String userId=sharedPreferences.getString("userId",null);
        SaveJobsModel saveJobsModel=new SaveJobsModel(userId,jobId);
        Call<User> response=apiService.saveJobs(saveJobsModel);
        response.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if(response.isSuccessful()){
                    User user=response.body();
                    if(user!=null){
                        System.out.println("Job saved successfully");
                        Toast.makeText(context,"Job saved successfully",Toast.LENGTH_SHORT).show();
                        for(String job:user.getSavedJobs()){
                            System.out.println("Job id: "+job);
                        }
                    }
                }else {
                    System.out.println("Error saving job");
                    System.out.println(response.message());
                    Log.d("error",response.message());
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable throwable) {
                System.out.println("Error saving job");
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
