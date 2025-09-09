package com.fahim.geminiapistarter;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ChatDao {
    @Insert
    long insertMessage(ChatMessage message);

    @Update
    void updateMessage(ChatMessage message);

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    LiveData<List<ChatMessage>> getAllMessages();

    @Query("DELETE FROM messages")
    void clearAll();
}

