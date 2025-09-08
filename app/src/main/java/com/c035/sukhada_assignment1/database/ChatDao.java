package com.c035.sukhada_assignment1.database;

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

