package com.fahim.geminiapistarter;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChatMessageEntity chatMessage);

    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    List<ChatMessageEntity> getAllMessages();

    @Query("DELETE FROM chat_messages")
    void deleteAll();
}


