package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private SharedPreferences preferences;
    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Initialize preferences and dark mode
        preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        isDarkMode = preferences.getBoolean("dark_mode", false);
        applyTheme();
        
        // Initialize views
        promptEditText = findViewById(R.id.promptEditText);
        FloatingActionButton sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        
        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        
        // Setup RecyclerView
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(false);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setHasFixedSize(false);
        chatRecyclerView.setNestedScrollingEnabled(true);
        
        // Add welcome message
        chatAdapter.addMessage(new ChatMessage("Hello! How can I help you today?", false));


        // Create GenerativeModel
        GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash",
                BuildConfig.API_KEY);


        sendButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }
            
            // Add user message to chat
            chatAdapter.addMessage(new ChatMessage(prompt, true));
            promptEditText.setText("");
            
            // Scroll to bottom smoothly
            chatRecyclerView.post(() -> {
                chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            });
            
            progressBar.setVisibility(VISIBLE);
            generativeModel.generateContent(prompt, new Continuation<>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    GenerateContentResponse response = (GenerateContentResponse) o;
                    String responseString = response.getText();
                    assert responseString != null;
                    Log.d("Response", responseString);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);
                        // Add AI response to chat
                        chatAdapter.addMessage(new ChatMessage(responseString, false));
                        // Scroll to bottom smoothly
                        chatRecyclerView.post(() -> {
                            chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                        });
                    });
                }
            });
        });

    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_theme) {
            toggleDarkMode();
            return true;
        } else if (item.getItemId() == R.id.action_clear_chat) {
            chatAdapter.clearMessages();
            chatAdapter.addMessage(new ChatMessage("Hello! How can I help you today?", false));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        preferences.edit().putBoolean("dark_mode", isDarkMode).apply();
        applyTheme();
        recreate();
    }
    
    private void applyTheme() {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}