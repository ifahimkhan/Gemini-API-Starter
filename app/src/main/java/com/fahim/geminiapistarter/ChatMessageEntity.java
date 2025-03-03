package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String message;
    public boolean isUser;

    public ChatMessageEntity(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }
}
