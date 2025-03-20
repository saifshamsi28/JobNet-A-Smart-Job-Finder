package com.saif.jobnet.Models.Education;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class EducationTypeConverter {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromEducationList(List<GraduationDetails> graduationDetailsList) {
        return gson.toJson(graduationDetailsList);
    }

    @TypeConverter
    public static List<GraduationDetails> toEducationList(String educationString) {
        Type listType = new TypeToken<List<GraduationDetails>>() {}.getType();
        return gson.fromJson(educationString, listType);
    }
}
