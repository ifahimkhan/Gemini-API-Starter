package com.c016.aanchal.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ChatDao {
    @Insert
    void insert(ChatMessage message);

    @Query("SELECT * FROM messages")
    List<ChatMessage> getAllMessages();
}

