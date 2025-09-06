package com.fahim.geminiapistarter;

public class ChatMessage {
    private final String message;
    private final boolean isUser;
    private final long timestamp;

    public ChatMessage(String message, boolean isUser, long timestamp) {
        this.message = message;
        this.isUser = isUser;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return isUser;
    }

    public long getTimestamp() { return timestamp; }
}
