package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import android.widget.Button;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private static final int SPEECH_REQUEST_CODE = 1;
    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatList = new ArrayList<>();
    private GenerativeModel generativeModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Button toggleDarkModeButton = findViewById(R.id.toggleDarkModeButton);

// Load dark mode state from SharedPreferences
//        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
//        boolean isDarkMode = preferences.getBoolean("dark_mode", false);

// Set button text based on current theme
//        toggleDarkModeButton.setText(isDarkMode ? "Light Mode" : "Dark Mode");

// Toggle Dark Mode on button click
        toggleDarkModeButton.setOnClickListener(v -> {
            new Thread(() -> {
                UserPreferencesDao dao = AppDatabase.getInstance(this).userPreferencesDao();
                UserPreferences preferences = dao.getPreferences();

                if (preferences == null) {
                    preferences = new UserPreferences(false);
                    dao.insert(preferences);
                }

                boolean isDarkMode = preferences.darkMode;
                AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
                dao.update(new UserPreferences(!isDarkMode));

                runOnUiThread(() -> {
                    toggleDarkModeButton.setText(isDarkMode ? "Dark Mode" : "Light Mode");
                    recreate();
                });
            }).start();
        });



        promptEditText = findViewById(R.id.promptEditText);
        ImageButton sendButton = findViewById(R.id.sendButton);
        ImageButton micButton = findViewById(R.id.micButton);
        progressBar = findViewById(R.id.progressBar);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        new Thread(() -> {
            List<ChatMessageEntity> chatHistory = AppDatabase.getInstance(this).chatDao().getAllChats();
            runOnUiThread(() -> {
                for (ChatMessageEntity entity : chatHistory) {
                    chatList.add(new ChatMessage(entity.message, entity.isUserMessage));
                }
                chatAdapter.notifyDataSetChanged();
            });
        }).start();


        // Setup RecyclerView
        chatAdapter = new ChatAdapter(chatList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Initialize GenerativeModel
        generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);

        // Send button click listener
        sendButton.setOnClickListener(v -> sendPrompt());

        // Mic button click listener for speech input
        micButton.setOnClickListener(v -> startVoiceInput());
    }

    private void sendPrompt() {
        String prompt = promptEditText.getText().toString();
        if (prompt.isEmpty()) {
            promptEditText.setError(getString(R.string.field_cannot_be_empty));
            return;
        }

        // Add user message to RecyclerView and Database
        ChatMessage userMessage = new ChatMessage(prompt, true);
        chatList.add(userMessage);
        chatAdapter.notifyItemInserted(chatList.size() - 1);
        chatRecyclerView.scrollToPosition(chatList.size() - 1);

        new Thread(() -> {
            AppDatabase.getInstance(this).chatDao().insert(new ChatMessageEntity(prompt, true));
        }).start();

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

                runOnUiThread(() -> {
                    progressBar.setVisibility(GONE);

                    // Add AI response to RecyclerView and Database
                    ChatMessage aiMessage = new ChatMessage(responseString, false);
                    chatList.add(aiMessage);
                    chatAdapter.notifyItemInserted(chatList.size() - 1);
                    chatRecyclerView.scrollToPosition(chatList.size() - 1);

                    new Thread(() -> {
                        AppDatabase.getInstance(MainActivity.this).chatDao().insert(new ChatMessageEntity(responseString, false));
                    }).start();
                });
            }
        });
    }


    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input is not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            List<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                promptEditText.setText(result.get(0));
            }
        }
    }
}