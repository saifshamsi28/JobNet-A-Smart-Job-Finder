package com.saif.jobnet.Database;

import android.content.Context;

import androidx.room.Room;

public class DatabaseClient {
    private Context context;
    private static DatabaseClient databaseClient;
    private final AppDatabase appDatabase;

    public DatabaseClient(Context context) {
        this.context = context;
        appDatabase = Room.databaseBuilder(context, AppDatabase.class, "jobs.db")
                .fallbackToDestructiveMigration() // Use destructive migration to start fresh
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

