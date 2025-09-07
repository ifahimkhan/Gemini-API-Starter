package com.example.c077_assignment1;

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
