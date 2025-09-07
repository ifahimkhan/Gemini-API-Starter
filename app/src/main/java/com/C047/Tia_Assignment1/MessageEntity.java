package com.C047.Tia_Assignment1;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String text;
    public boolean isUser;

    public MessageEntity(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }
}
