package com.example.c110_siddhamshah;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatDao {
    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    List<ChatEntity> getAllChats();

    @Insert
    void insertChat(ChatEntity chat);

    @Query("DELETE FROM chat_history")
    void clearAllChats();
}
