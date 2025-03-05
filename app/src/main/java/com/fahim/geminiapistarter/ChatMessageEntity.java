package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String message;
    public boolean isUserMessage;

    public ChatMessageEntity(String message, boolean isUserMessage) {
        this.message = message;
        this.isUserMessage = isUserMessage;
    }
}
