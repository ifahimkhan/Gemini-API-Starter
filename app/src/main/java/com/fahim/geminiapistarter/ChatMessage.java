package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String text;
    public boolean isFromUser;

    public ChatMessage(String text, boolean isFromUser) {
        this.text = text;
        this.isFromUser = isFromUser;
    }
}