package com.saif.jobnet.Database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.saif.jobnet.Models.Job;

import java.util.List;

@Dao
public interface JobDao {
    @Query("SELECT * FROM jobs")
    List<Job> getAllJobs();

    @Query("SELECT * FROM jobs WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%'")
    List<Job> getJobsByTitle(String query);

    @Query("SELECT * FROM jobs WHERE url = :url")
    Job getJobByUrl(String url);

    @Query("SELECT * FROM jobs WHERE title LIKE '%' || :query || '%' OR jobId LIKE '%' || :query || '%'")
    List<Job> searchJobs(String query);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertJob(Job job);

    //to update job shortDescription
//    @Query("UPDATE jobs SET description = :description WHERE url = :url")
//    void updateJobDescription(String url, String description);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllJobs(List<Job> jobs);

    @Query("DELETE FROM jobs")
    void deleteAllJobs();

}
