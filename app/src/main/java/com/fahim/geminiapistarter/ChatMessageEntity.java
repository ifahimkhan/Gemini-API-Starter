package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessageEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String message;  // Renamed from 'text' to 'message' for consistency
    private boolean isUser;

    // Constructor
    public ChatMessageEntity(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getMessage() {  // ✅ Renamed from 'getText()' to 'getMessage()'
        return message;
    }

    public boolean isUser() {
        return isUser;
    }

    // Setter for ID
    public void setId(int id) {
        this.id = id;
    }
}
