package com.fahim.geminiapistarter;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insert(Message message);

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    List<Message> getAllMessages();

    @Query("DELETE FROM messages")
    void deleteAllMessages();
}