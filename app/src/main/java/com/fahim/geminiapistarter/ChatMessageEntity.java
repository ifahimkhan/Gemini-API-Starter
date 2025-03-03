package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessageEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String message;
    private boolean isUser;

    // Constructor
    public ChatMessageEntity(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }


    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return isUser;
    }


    public void setId(int id) {
        this.id = id;
    }
}
