package com.saif.jobnet.Models;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class SkillConverter {
    @TypeConverter
    public static String fromJobList(List<String> skills) {
        Gson gson = new Gson();
        return gson.toJson(skills);
    }

    @TypeConverter
    public static List<String> toJobList(String stringListSkills) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(stringListSkills, listType);
    }
}