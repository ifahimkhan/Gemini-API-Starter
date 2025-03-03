package com.fahim.geminiapistarter;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    UserPreferences getPreferences();

    @Insert
    void insert(UserPreferences preferences);

    @Update
    void update(UserPreferences preferences);
}
