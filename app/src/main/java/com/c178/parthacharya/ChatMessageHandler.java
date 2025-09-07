package com.c178.parthacharya;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatMessageHandler {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChatMessage chatMessage);

    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    List<ChatMessage> getAllMessages();

    @Query("DELETE FROM chat_messages")
    void deleteAll();
}