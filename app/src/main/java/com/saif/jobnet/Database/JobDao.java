package com.saif.jobnet.Database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.saif.jobnet.Models.Education;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.User;

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

    //to update job fullDescription
    @Query("UPDATE jobs SET fullDescription = :description WHERE url = :url")
    void updateJobDescription(String url, String description);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllJobs(List<Job> jobs);

    @Query("DELETE FROM jobs")
    void deleteAllJobs();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateUser(User user);

    @Query("SELECT * FROM jobs WHERE jobId = :id")
    Job getJobById(String id);

    @Query("SELECT * FROM user where id=:id")
    User getCurrentUser(String id);

    //insert education
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEducation(Education education);

    // Clear all user data (e.g., during logout)
    @Query("DELETE FROM user")
    void clearUser();
}
