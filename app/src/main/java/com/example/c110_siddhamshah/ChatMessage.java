package com.example.c110_siddhamshah;

public class ChatMessage {
    private String message;
    private boolean isUser;
    private long timestamp;

    public ChatMessage(String message, boolean isUser, long timestamp) {
        this.message = message;
        this.isUser = isUser;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getMessage() { return message; }
    public boolean isUser() { return isUser; }
    public long getTimestamp() { return timestamp; }
}
