package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CODE_SPEECH_INPUT = 1001;
    private EditText promptEditText;
    private ImageButton micButton;
    private ImageButton sendButton;
    private ProgressBar progressBar;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private final ArrayList<ChatMessage> chatMessages = new ArrayList<>();
    private GenerativeModelFutures model;
    private ChatDatabase chatDatabase;
    private ExecutorService ioExecutor;
    private SharedPreferences prefs;
    private Switch darkModeSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        promptEditText = findViewById(R.id.promptEditText);
        micButton = findViewById(R.id.micButton);
        sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);

        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            darkModeSwitch.setChecked(true);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            darkModeSwitch.setChecked(false);
        }

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        chatAdapter = new ChatAdapter();
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        chatDatabase = ChatDatabase.getInstance(getApplicationContext());
        ioExecutor = Executors.newSingleThreadExecutor();

        loadChatHistory();

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", BuildConfig.API_KEY);
        model = GenerativeModelFutures.from(gm);

        micButton.setOnClickListener(v -> startVoiceInput());

        sendButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }

            long ts = System.currentTimeMillis();
            ChatMessage userMessage = new ChatMessage(prompt, true, ts);
            addMessageToUiAndDb(userMessage);

            promptEditText.setText("");
            progressBar.setVisibility(VISIBLE);

            Content content = new Content.Builder().addText(prompt).build();
            ListenableFuture<GenerateContentResponse> future = model.generateContent(content);

            future.addListener(() -> {
                try {
                    GenerateContentResponse response = future.get();
                    String responseString = response != null ? response.getText() : null;
                    if (responseString == null) responseString = getString(R.string.response);

                    long rts = System.currentTimeMillis();
                    ChatMessage aiMessage = new ChatMessage(responseString, false, rts);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);
                        addMessageToUiAndDb(aiMessage);
                    });
                } catch (Exception e) {
                    long rts = System.currentTimeMillis();
                    ChatMessage errMessage = new ChatMessage("Error: " + e.getMessage(), false, rts);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);
                        addMessageToUiAndDb(errMessage);
                    });
                    Log.e("GeminiError", "Error generating content", e);
                }
            }, Runnable::run);
        });
    }

    private void addMessageToUiAndDb(ChatMessage message) {
        List<ChatMessage> current = new ArrayList<>(chatAdapter.getCurrentList());
        current.add(message);
        chatAdapter.submitList(current);
        chatRecyclerView.scrollToPosition(current.size() - 1);

        // persist
        ioExecutor.execute(() -> {
            ChatEntity e = new ChatEntity(message.getMessage(), message.isUser(), message.getTimestamp());
            chatDatabase.chatDao().insert(e);
        });
    }

    private void loadChatHistory() {
        ioExecutor.execute(() -> {
            List<ChatEntity> saved = chatDatabase.chatDao().getAllChats();
            List<ChatMessage> messages = new ArrayList<>();
            for (ChatEntity e : saved) {
                messages.add(new ChatMessage(e.message, e.isUser, e.timestamp));
            }
            runOnUiThread(() -> {
                chatAdapter.submitList(messages);
                if (!messages.isEmpty()) {
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                }
            });
        });
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                promptEditText.setText(results.get(0));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }
}
