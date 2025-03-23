package com.saif.jobnet.Models.Education;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class Class10TypeConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromEducationDetails(Class10Details class10Details) {
        return gson.toJson(class10Details);
    }

    @TypeConverter
    public static Class10Details toEducationDetails(String json) {
        Type listType = new TypeToken<Class10Details>() {}.getType();
        return gson.fromJson(json, listType);
    }
}