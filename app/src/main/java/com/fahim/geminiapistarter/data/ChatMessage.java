package com.fahim.geminiapistarter.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String text;
    public boolean isUser;
    public long timestamp;

    public ChatMessage(String text, boolean isUser, long timestamp) {
        this.text = text;
        this.isUser = isUser;
        this.timestamp = timestamp;
    }
}
