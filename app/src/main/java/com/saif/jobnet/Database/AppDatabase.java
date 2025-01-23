package com.saif.jobnet.Database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.saif.jobnet.Database.JobDao;
import com.saif.jobnet.Models.Job;

@Database(entities = {Job.class}, version = 4) // Set version to 3 for the updated schema
public abstract class AppDatabase extends RoomDatabase {
    public abstract JobDao jobDao();
}
