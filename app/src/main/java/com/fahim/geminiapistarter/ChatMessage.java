package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String text;
    public String sender;
    public long timestamp;

    public ChatMessage() { }

    public ChatMessage(int id, String text, String sender, long timestamp) {
        this.id = id;
        this.text = text;
        this.sender = sender;
        this.timestamp = timestamp;
    }
}

