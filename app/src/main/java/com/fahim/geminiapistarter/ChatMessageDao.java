package com.fahim.geminiapistarter;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    List<ChatMessage> getAll();

    @Insert
    void insert(ChatMessage message);
}