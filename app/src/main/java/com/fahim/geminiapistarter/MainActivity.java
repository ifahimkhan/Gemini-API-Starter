package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
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
import androidx.room.Room;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static final String PREFS_NAME = "GeminiAppPrefs";
    private static final String DARK_MODE_KEY = "dark_mode";
    private static final String LAST_PROMPT_KEY = "last_prompt";

    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ImageButton sendButton;
    private ImageButton micButton;

    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private AppDatabase database;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences preferences;

    private GenerativeModel generativeModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize preferences and set theme
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = preferences.getBoolean(DARK_MODE_KEY, false);
        setThemeMode(isDarkMode);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Room database
        database = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "chat_database")
                .fallbackToDestructiveMigration() // This will destroy and recreate the database instead of migrating
                .build();

        // Initialize UI components
        initializeViews();
        setupRecyclerView();

        // Load messages from database
        loadMessages();

        // Create GenerativeModel
        generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);

        // Restore last prompt
        String lastPrompt = preferences.getString(LAST_PROMPT_KEY, "");
        if (!lastPrompt.isEmpty()) {
            promptEditText.setText(lastPrompt);
        }
    }

    private void initializeViews() {
        promptEditText = findViewById(R.id.promptEditText);
        sendButton = findViewById(R.id.sendButton);
        micButton = findViewById(R.id.mic_button);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recycler_view_messages);

        sendButton.setOnClickListener(v -> sendPrompt());
        micButton.setOnClickListener(v -> startSpeechRecognition());
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadMessages() {
        executor.execute(() -> {
            List<Message> messages = database.messageDao().getAllMessages();
            runOnUiThread(() -> {
                messageList.clear();
                messageList.addAll(messages);
                adapter.notifyDataSetChanged();
                scrollToBottom();
            });
        });
    }

    private void sendPrompt() {
        String prompt = promptEditText.getText().toString().trim();
        promptEditText.setError(null);

        if (prompt.isEmpty()) {
            promptEditText.setError(getString(R.string.field_cannot_be_empty));
            return;
        }

        // Save the prompt
        preferences.edit().putString(LAST_PROMPT_KEY, prompt).apply();

        // Add user message to the list
        Message userMessage = new Message(prompt, true);
        addMessageToList(userMessage);

        // Clear the input field
        promptEditText.setText("");

        // Show progress
        progressBar.setVisibility(VISIBLE);

        // Send to Gemini API
        generativeModel.generateContent(prompt, new Continuation<GenerateContentResponse>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(GONE);

                    if (o instanceof GenerateContentResponse) {
                        GenerateContentResponse response = (GenerateContentResponse) o;
                        String responseString = response.getText();

                        if (responseString != null) {
                            // Add AI response to the list
                            Message aiMessage = new Message(responseString, false);
                            addMessageToList(aiMessage);
                        } else {
                            // Handle empty response
                            Toast.makeText(MainActivity.this,
                                    "Received empty response from Gemini", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Handle error - the object is probably a Result.Failure
                        Toast.makeText(MainActivity.this,
                                "Error connecting to Gemini API", Toast.LENGTH_SHORT).show();

                        // Add fallback response
                        Message errorMessage = new Message("Sorry, I couldn't process your request. Please try again later.", false);
                        addMessageToList(errorMessage);
                    }
                });
            }
        });
    }

    private void addMessageToList(Message message) {
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();

        // Save to database
        executor.execute(() -> database.messageDao().insert(message));
    }

    private void scrollToBottom() {
        if (messageList.size() > 0) {
            recyclerView.smoothScrollToPosition(messageList.size() - 1);
        }
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to type...");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported on this device",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setThemeMode(boolean darkMode) {
        int mode = darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String speechText = result.get(0);
                promptEditText.setText(speechText);
                promptEditText.setSelection(speechText.length());
            }
        }
    }

    @Override

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem darkModeItem = menu.findItem(R.id.action_dark_mode);
        boolean isDarkMode = preferences.getBoolean(DARK_MODE_KEY, false);
        darkModeItem.setIcon(isDarkMode ? R.drawable.baseline_light_mode_24 : R.drawable.baseline_dark_mode_24);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_dark_mode) {
            boolean currentMode = preferences.getBoolean(DARK_MODE_KEY, false);
            boolean newMode = !currentMode;
            preferences.edit().putBoolean(DARK_MODE_KEY, newMode).apply();
            setThemeMode(newMode);
            invalidateOptionsMenu();
            return true;
        } else if (id == R.id.action_clear_history) {
            clearHistory();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void clearHistory() {
        executor.execute(() -> {
            database.messageDao().deleteAllMessages();
            runOnUiThread(() -> {
                messageList.clear();
                adapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "History cleared", Toast.LENGTH_SHORT).show();
            });
        });
    }
}