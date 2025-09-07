package com.c008.akshajramakrishnan;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_table")
public class Chat {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String prompt;
    private String response;

    public Chat(String prompt, String response) {
        this.prompt = prompt;
        this.response = response;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPrompt() { return prompt; }
    public String getResponse() { return response; }
}
