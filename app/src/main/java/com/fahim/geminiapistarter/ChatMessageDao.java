package com.fahim.geminiapistarter;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatMessageDao {
    @Insert
    void insert(ChatMessageEntity message);

    @Query("SELECT * FROM chat_messages")
    List<ChatMessageEntity> getAllMessages();

    @Query("DELETE FROM chat_messages")
    void deleteAll();
}
