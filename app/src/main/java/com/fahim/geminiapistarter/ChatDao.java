package com.fahim.geminiapistarter;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatDao {
    @Insert
    void insert(ChatEntity chat);

    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    List<ChatEntity> getAllChats();

    @Query("DELETE FROM chat_history")
    void clearAll();
}
