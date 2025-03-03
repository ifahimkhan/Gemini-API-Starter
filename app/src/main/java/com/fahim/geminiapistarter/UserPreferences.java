package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_preferences")
public class UserPreferences {
    @PrimaryKey
    public int id = 1;
    public boolean darkMode;

    public UserPreferences(boolean darkMode) {
        this.darkMode = darkMode;
    }
}
