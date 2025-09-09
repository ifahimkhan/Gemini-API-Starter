package com.c168.aaryahi;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    void insertMessage(MessageEntity message);

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY id ASC")
    List<MessageEntity> getMessagesForConversation(int conversationId);

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    void deleteMessagesForConversation(int conversationId);
}
