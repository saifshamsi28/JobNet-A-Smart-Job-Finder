package com.saif.jobnet.Models.Education;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.saif.jobnet.EducationDetails;

import java.lang.reflect.Type;
import java.util.List;

public class EducationTypeConverter {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromEducationList(List<EducationDetails> educationDetailsList) {
        return gson.toJson(educationDetailsList);
    }

    @TypeConverter
    public static List<EducationDetails> toEducationList(String educationString) {
        Type listType = new TypeToken<List<EducationDetails>>() {}.getType();
        return gson.fromJson(educationString, listType);
    }
}
