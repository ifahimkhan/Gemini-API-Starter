package com.c168.aaryahi;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update; // Import Update
import java.util.List;

@Dao
public interface ConversationDao {
    @Insert
    long insertConversation(Conversation conversation);

    @Query("SELECT * FROM conversations ORDER BY lastUpdated DESC")
    List<Conversation> getAllConversations();

    @Query("DELETE FROM conversations WHERE id = :id")
    void deleteConversationById(int id);

    @Query("SELECT * FROM conversations WHERE id = :id")
    Conversation getConversationById(int id);

    @Update
    void updateConversation(Conversation conversation);

    @Query("UPDATE conversations SET lastUpdated = :timestamp WHERE id = :id")
    void updateTimestamp(int id, long timestamp);
}
