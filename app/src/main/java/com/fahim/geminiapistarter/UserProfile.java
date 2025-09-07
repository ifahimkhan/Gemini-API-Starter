package com.fahim.geminiapistarter;

import java.util.ArrayList;
import java.util.List;

public class UserProfile {
    private String userName;
    private String preferredLanguage;
    private String aiPersonality;
    private boolean darkModeEnabled;
    private boolean voiceInputEnabled;
    private List<ConversationSession> conversationHistory;
    private long lastActiveTime;
    private int totalMessages;
    private String favoriteTopics;

    public UserProfile() {
        this.userName = "User";
        this.preferredLanguage = "en-US";
        this.aiPersonality = "helpful";
        this.darkModeEnabled = false;
        this.voiceInputEnabled = true;
        this.conversationHistory = new ArrayList<>();
        this.lastActiveTime = System.currentTimeMillis();
        this.totalMessages = 0;
        this.favoriteTopics = "";
    }

    // Getters and Setters
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getAiPersonality() {
        return aiPersonality;
    }

    public void setAiPersonality(String aiPersonality) {
        this.aiPersonality = aiPersonality;
    }

    public boolean isDarkModeEnabled() {
        return darkModeEnabled;
    }

    public void setDarkModeEnabled(boolean darkModeEnabled) {
        this.darkModeEnabled = darkModeEnabled;
    }

    public boolean isVoiceInputEnabled() {
        return voiceInputEnabled;
    }

    public void setVoiceInputEnabled(boolean voiceInputEnabled) {
        this.voiceInputEnabled = voiceInputEnabled;
    }

    public List<ConversationSession> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<ConversationSession> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(int totalMessages) {
        this.totalMessages = totalMessages;
    }

    public String getFavoriteTopics() {
        return favoriteTopics;
    }

    public void setFavoriteTopics(String favoriteTopics) {
        this.favoriteTopics = favoriteTopics;
    }

    public void incrementMessageCount() {
        this.totalMessages++;
    }

    public void updateLastActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }
}
