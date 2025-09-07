package com.fahim.geminiapistarter;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class UserPreferencesManager {
    private static final String PREFS_NAME = "user_preferences";
    private static final String KEY_USER_PROFILE = "user_profile";
    private static final String KEY_CONVERSATION_HISTORY = "conversation_history";
    private static final String KEY_CURRENT_SESSION = "current_session";
    
    private SharedPreferences preferences;
    private Gson gson;
    
    public UserPreferencesManager(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    // User Profile Management
    public void saveUserProfile(UserProfile profile) {
        String profileJson = gson.toJson(profile);
        preferences.edit().putString(KEY_USER_PROFILE, profileJson).apply();
    }
    
    public UserProfile getUserProfile() {
        String profileJson = preferences.getString(KEY_USER_PROFILE, null);
        if (profileJson != null) {
            return gson.fromJson(profileJson, UserProfile.class);
        }
        return new UserProfile(); // Return default profile if none exists
    }
    
    // Conversation History Management
    public void saveConversationHistory(List<ConversationSession> sessions) {
        String historyJson = gson.toJson(sessions);
        preferences.edit().putString(KEY_CONVERSATION_HISTORY, historyJson).apply();
    }
    
    public List<ConversationSession> getConversationHistory() {
        String historyJson = preferences.getString(KEY_CONVERSATION_HISTORY, null);
        if (historyJson != null) {
            Type listType = new TypeToken<List<ConversationSession>>(){}.getType();
            return gson.fromJson(historyJson, listType);
        }
        return new ArrayList<>();
    }
    
    public void addConversationSession(ConversationSession session) {
        List<ConversationSession> history = getConversationHistory();
        history.add(0, session); // Add to beginning of list
        
        // Keep only last 50 sessions to prevent storage bloat
        if (history.size() > 50) {
            history = history.subList(0, 50);
        }
        
        saveConversationHistory(history);
    }
    
    public void updateCurrentSession(ConversationSession session) {
        String sessionJson = gson.toJson(session);
        preferences.edit().putString(KEY_CURRENT_SESSION, sessionJson).apply();
    }
    
    public ConversationSession getCurrentSession() {
        String sessionJson = preferences.getString(KEY_CURRENT_SESSION, null);
        if (sessionJson != null) {
            return gson.fromJson(sessionJson, ConversationSession.class);
        }
        return new ConversationSession();
    }
    
    public void clearCurrentSession() {
        preferences.edit().remove(KEY_CURRENT_SESSION).apply();
    }
    
    // Quick preference getters/setters
    public boolean isDarkModeEnabled() {
        return getUserProfile().isDarkModeEnabled();
    }
    
    public void setDarkModeEnabled(boolean enabled) {
        UserProfile profile = getUserProfile();
        profile.setDarkModeEnabled(enabled);
        saveUserProfile(profile);
    }
    
    public boolean isVoiceInputEnabled() {
        return getUserProfile().isVoiceInputEnabled();
    }
    
    public void setVoiceInputEnabled(boolean enabled) {
        UserProfile profile = getUserProfile();
        profile.setVoiceInputEnabled(enabled);
        saveUserProfile(profile);
    }
    
    public String getAiPersonality() {
        return getUserProfile().getAiPersonality();
    }
    
    public void setAiPersonality(String personality) {
        UserProfile profile = getUserProfile();
        profile.setAiPersonality(personality);
        saveUserProfile(profile);
    }
    
    public String getUserName() {
        return getUserProfile().getUserName();
    }
    
    public void setUserName(String name) {
        UserProfile profile = getUserProfile();
        profile.setUserName(name);
        saveUserProfile(profile);
    }
    
    // Clear all data
    public void clearAllData() {
        preferences.edit().clear().apply();
    }
}
