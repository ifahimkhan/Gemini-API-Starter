package com.shah.c058_mad_assignment1.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageChat {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String text;
    private boolean isUser;

    // Constructor
    public MessageChat(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }

    // Getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isUser() { return isUser; }
    public void setUser(boolean user) { isUser = user; }
}