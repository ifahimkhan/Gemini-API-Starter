package com.C162.AhaanMehta;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insert(MessageEntity message);

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    List<MessageEntity> getAll();

    @Query("DELETE FROM messages") // Ensure 'message_entity' is your actual table name
    void clearAllMessages();
}
