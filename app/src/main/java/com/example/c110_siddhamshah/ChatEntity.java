package com.example.c110_siddhamshah;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_history")
public class ChatEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_message")
    public String userMessage;

    @ColumnInfo(name = "gemini_response")
    public String geminiResponse;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    public ChatEntity(String userMessage, String geminiResponse, long timestamp) {
        this.userMessage = userMessage;
        this.geminiResponse = geminiResponse;
        this.timestamp = timestamp;
    }
}
