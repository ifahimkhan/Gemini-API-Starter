package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {
    private static final int SPEECH_REQUEST_CODE = 100;

    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private UserPreferences userPreferences;
    private AppDatabase database;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize preferences and apply theme
        userPreferences = new UserPreferences(this);
        applyTheme();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize views
        promptEditText = findViewById(R.id.promptEditText);
        MaterialButton submitPromptButton = findViewById(R.id.sendButton);
        ImageButton micButton = findViewById(R.id.micButton);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);

        // Initialize RecyclerView
        chatAdapter = new ChatAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // Initialize database and thread pool
        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        // Load chat history
        loadChatHistory();

        // Initialize speech recognition
        initializeSpeechRecognition(micButton);

        // Create GenerativeModel
        GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash",
                BuildConfig.API_KEY);

        // Send button click listener
        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString();
            promptEditText.setError(null);

            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }

            // Add user message to chat
            ChatMessage userMessage = new ChatMessage(ChatMessage.TYPE_USER, prompt);
            chatAdapter.addMessage(userMessage);

            // Save to database
            saveMessageToDatabase(userMessage);

            // Clear input field
            promptEditText.setText("");

            // Scroll to bottom
            recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

            // Show progress
            progressBar.setVisibility(VISIBLE);

            // Send request to Gemini
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

                    // Create AI message
                    ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_AI, responseString);

                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);

                        // Add AI message to chat
                        chatAdapter.addMessage(aiMessage);

                        // Save to database
                        saveMessageToDatabase(aiMessage);

                        // Scroll to bottom
                        recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    });
                }
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_clear_history) {
            clearChatHistory();
            return true;
        } else if (id == R.id.action_toggle_theme) {
            boolean currentMode = userPreferences.isDarkMode();
            userPreferences.setDarkMode(!currentMode);
            applyTheme();
            recreate();
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadChatHistory() {
        executorService.execute(() -> {
            List<ChatMessageEntity> messageEntities = database.chatMessageDao().getAllMessages();
            List<ChatMessage> messages = new ArrayList<>();

            for (ChatMessageEntity entity : messageEntities) {
                messages.add(new ChatMessage(entity.getMessageType(), entity.getMessageContent()));
            }

            runOnUiThread(() -> {
                chatAdapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    recyclerView.smoothScrollToPosition(messages.size() - 1);
                }
            });
        });
    }

    private void saveMessageToDatabase(ChatMessage message) {
        executorService.execute(() -> {
            ChatMessageEntity entity = new ChatMessageEntity(
                    message.getMessageType(),
                    message.getMessageContent(),
                    message.getTimestamp()
            );
            database.chatMessageDao().insertMessage(entity);
        });
    }

    private void clearChatHistory() {
        executorService.execute(() -> {
            database.chatMessageDao().clearAllMessages();
            runOnUiThread(() -> {
                chatAdapter.clearMessages();
                Toast.makeText(MainActivity.this, "Chat history cleared", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void initializeSpeechRecognition(ImageButton micButton) {
        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your question...");

        micButton.setOnClickListener(v -> {
            try {
                startActivityForResult(speechIntent, SPEECH_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                promptEditText.setText(spokenText);
            }
        }
    }

    private void applyTheme() {
        if (userPreferences.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}