package com.saif.jobnet.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.SaveJobsModel;
import com.saif.jobnet.Network.ApiService;
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
        holder.binding.shortDescription.setText(job.getShortDescription());
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
                intent.putExtra("stringId", job.getJobId());
                intent.putExtra("url", job.getUrl());
                System.out.println("url to visit: "+ job.getUrl());
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
            saveJobsModel=new SaveJobsModel(userId,jobId ,true);
        } else {
            // Job is saved; unsave the job
            saveJobsModel=new SaveJobsModel(userId, jobId,false);
        }
//        saveJobsModel=new SaveJobsModel(userId,stringId);
        System.out.println("before request: job id: "+saveJobsModel.getJobId()+" , user id: "+saveJobsModel.getUserId());
        Call<ResponseBody> response=apiService.saveJobs(saveJobsModel);
        response.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseMessage = response.body().string(); // Extract plain text
                        System.out.println("responseMessage: "+responseMessage);
                        if (tag.equals("0")) {
                            saveJobs.setTag("1");
                            saveJobs.setImageResource(R.drawable.job_saved_icon);

                            //split the response by title
                            String[] parts = responseMessage.split("title:");
                            System.out.println("responseTitle: "+parts[1]);

                            Toast.makeText(context, "Job saved Successfully: " + parts[1], Toast.LENGTH_SHORT).show();
                        } else {
                            saveJobs.setTag("0");
                            saveJobs.setImageResource(R.drawable.job_not_saved_icon);
                        }
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
