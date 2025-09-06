package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_history")
public class ChatEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String message;
    public boolean isUser;
    public long timestamp;

    public ChatEntity(String message, boolean isUser, long timestamp) {
        this.message = message;
        this.isUser = isUser;
        this.timestamp = timestamp;
    }
}
