package com.saif.jobnet.Models.Resume;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class ResumeConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromEducationDetails(Resume resume) {
        return gson.toJson(resume);
    }

    @TypeConverter
    public static Resume toEducationDetails(String json) {
        Type itemType = new TypeToken<Resume>() {}.getType();
        return gson.fromJson(json, itemType);
    }
}