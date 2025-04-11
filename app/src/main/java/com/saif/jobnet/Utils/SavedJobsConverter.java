package com.saif.jobnet.Utils;
import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.saif.jobnet.Models.Job;

import java.lang.reflect.Type;
import java.util.List;

public class SavedJobsConverter {
    @TypeConverter
    public static String fromJobList(List<Job> jobs) {
        Gson gson = new Gson();
        return gson.toJson(jobs);
    }

    @TypeConverter
    public static List<Job> toJobList(String stringListString) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Job>>() {}.getType();
        return gson.fromJson(stringListString, listType);
    }
}
