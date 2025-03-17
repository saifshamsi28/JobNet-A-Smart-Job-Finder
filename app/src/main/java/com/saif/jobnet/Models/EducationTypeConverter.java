package com.saif.jobnet.Models;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EducationTypeConverter {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromEducationList(List<Education> educationList) {
        return gson.toJson(educationList);
    }

    @TypeConverter
    public static List<Education> toEducationList(String educationString) {
        Type listType = new TypeToken<List<Education>>() {}.getType();
        return gson.fromJson(educationString, listType);
    }
}
