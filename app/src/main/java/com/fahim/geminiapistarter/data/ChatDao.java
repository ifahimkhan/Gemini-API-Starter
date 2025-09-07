package com.fahim.geminiapistarter.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatDao {
    @Insert
    void insert(ChatMessage message);

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    List<ChatMessage> getAll();

    @Query("DELETE FROM messages")
    void clear();
}
