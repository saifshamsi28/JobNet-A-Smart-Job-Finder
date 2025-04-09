package com.saif.jobnet.Models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.time.LocalDateTime;

@Entity(tableName = "recent_searches")
public class RecentSearch {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "query")
    public String query;

    @TypeConverters(DateTimeConverters.class)
    @ColumnInfo(name = "searchedAt")
    public LocalDateTime searchedAt;

    public RecentSearch() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public LocalDateTime getSearchedAt() {
        return searchedAt;
    }

    public void setSearchedAt(LocalDateTime searchedAt) {
        this.searchedAt = searchedAt;
    }
}

