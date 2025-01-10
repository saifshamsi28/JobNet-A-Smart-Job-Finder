package com.saif.jobnet.Database;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.saif.jobnet.Models.Job;

@Database(entities = Job.class, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract JobDao jobDao();

    //Migration to add new columns "openings" and "applicants"
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // MIGRATION_1_2....Add new columns "openings" and "applicants" with TEXT type
//            database.execSQL("ALTER TABLE jobs ADD COLUMN openings TEXT");
//            database.execSQL("ALTER TABLE jobs ADD COLUMN applicants TEXT");

            // Create indices for optimization(MIGRATION_2_3)
            try {
                database.execSQL("CREATE INDEX IF NOT EXISTS index_jobs_title ON jobs(title)");
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_jobs_jobId ON jobs(jobId)");
                database.execSQL("CREATE INDEX IF NOT EXISTS index_jobs_url ON jobs(url)");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}
