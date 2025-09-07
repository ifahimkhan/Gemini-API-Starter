package com.fahim.geminiapistarter;

import java.util.ArrayList;
import java.util.List;

public class ConversationSession {
    private String sessionId;
    private long startTime;
    private long endTime;
    private List<ChatMessage> messages;
    private String sessionTitle;
    private String summary;

    public ConversationSession() {
        this.sessionId = generateSessionId();
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        this.messages = new ArrayList<>();
        this.sessionTitle = "New Conversation";
        this.summary = "";
    }

    public ConversationSession(String sessionId) {
        this.sessionId = sessionId;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        this.messages = new ArrayList<>();
        this.sessionTitle = "New Conversation";
        this.summary = "";
    }

    private String generateSessionId() {
        return "session_" + System.currentTimeMillis();
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public void setSessionTitle(String sessionTitle) {
        this.sessionTitle = sessionTitle;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
    }

    public void endSession() {
        this.endTime = System.currentTimeMillis();
    }

    public long getDuration() {
        if (endTime == 0) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }

    public int getMessageCount() {
        return messages.size();
    }

    public String getFirstUserMessage() {
        for (ChatMessage message : messages) {
            if (message.isUser()) {
                return message.getMessage();
            }
        }
        return "No user messages";
    }
}
