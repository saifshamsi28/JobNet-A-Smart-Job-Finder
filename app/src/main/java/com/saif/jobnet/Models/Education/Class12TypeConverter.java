package com.saif.jobnet.Models.Education;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class Class12TypeConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromEducationDetails(Class12Details class12Details) {
        return gson.toJson(class12Details);
    }

    @TypeConverter
    public static Class12Details toEducationDetails(String json) {
        Type listType = new TypeToken<Class12Details>() {}.getType();
        return gson.fromJson(json, listType);
    }
}