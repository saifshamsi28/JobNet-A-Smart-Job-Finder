package com.saif.jobnet.Models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "skills")
public class Skill {
    @PrimaryKey
    @NonNull
    private String name; // Primary key to ensure uniqueness

    public Skill() {
    }

    public Skill(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }
}
