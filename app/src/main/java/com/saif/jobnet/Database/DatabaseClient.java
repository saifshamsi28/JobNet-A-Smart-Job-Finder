package com.saif.jobnet.Database;

import android.content.Context;

import androidx.room.Room;

    public class DatabaseClient {
        private Context context;
        private static DatabaseClient databaseClient;
        private AppDatabase appDatabase;

        public DatabaseClient(Context context) {
            this.context = context;
            appDatabase = Room.databaseBuilder(context, AppDatabase.class, "jobs.db")
                    .addMigrations(AppDatabase.MIGRATION_2_3) // Include the migration here
//                    .fallbackToDestructiveMigration() // Optional: use destructive migration in development
                    .build();
        }


    public static synchronized DatabaseClient getInstance(Context mCtx) {
        if (databaseClient == null) {
            databaseClient = new DatabaseClient(mCtx);
        }
        return databaseClient;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }
}
