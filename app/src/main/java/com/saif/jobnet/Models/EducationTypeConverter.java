package com.saif.jobnet.Models;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class EducationTypeConverter {
    @TypeConverter
    public String fromEducationList(List<Education> educationList) {
        Gson gson = new Gson();
        return gson.toJson(educationList);
    }

    @TypeConverter
    public List<Education> toEducationList(String educationJson) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<Education>>() {}.getType();
        return gson.fromJson(educationJson, type);
    }
}
