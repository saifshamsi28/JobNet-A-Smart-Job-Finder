package com.saif.jobnet.Models.Education;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class GraduationTypeConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromEducationDetails(GraduationDetails graduationDetails) {
        return gson.toJson(graduationDetails);
    }

    @TypeConverter
    public static GraduationDetails toEducationDetails(String json) {
        Type listType = new TypeToken<GraduationDetails>() {}.getType();
        return gson.fromJson(json, listType);
    }
}

