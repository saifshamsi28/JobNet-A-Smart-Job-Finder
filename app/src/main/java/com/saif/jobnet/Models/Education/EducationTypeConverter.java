package com.saif.jobnet.Models.Education;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class EducationTypeConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromEducationDetails(List<EducationDetails> list) {
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<EducationDetails> toEducationDetails(String json) {
        Type listType = new TypeToken<List<EducationDetails>>() {}.getType();
        return gson.fromJson(json, listType);
    }
}

