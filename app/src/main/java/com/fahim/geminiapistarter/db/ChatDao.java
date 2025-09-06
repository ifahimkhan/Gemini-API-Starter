package com.fahim.geminiapistarter.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatDao {

    @Insert
    void insert(ChatEntity chat);

    @Query("SELECT * FROM chat ORDER BY timestamp ASC")
    List<ChatEntity> getAllChats();
}
