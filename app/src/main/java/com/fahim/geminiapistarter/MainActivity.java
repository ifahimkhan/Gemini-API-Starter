package com.fahim.geminiapistarter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

    private EditText promptEditText;
    private RecyclerView rvMessages;
    private MessagesAdapter adapter;
    private GenerativeModel generativeModel;
    private static final int REQUEST_CODE_SPEECH = 100;
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_LAST_PROMPT = "last_prompt";
    private static final String KEY_DARK_MODE = "dark_mode";
    private SharedPreferences prefs;
    private ChatViewModel chatViewModel;

    private void saveLastPrompt(String lastPrompt) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_PROMPT, lastPrompt)
                .apply();
    }

    private void saveDarkMode(boolean enabled) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DARK_MODE, enabled)
                .apply();
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your query...");
        startActivityForResult(intent, REQUEST_CODE_SPEECH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                promptEditText.setText(result.get(0));
            }
        }
    }

    private void applySavedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastPrompt = prefs.getString(KEY_LAST_PROMPT, "");
        boolean darkMode = prefs.getBoolean(KEY_DARK_MODE, false);

        if (!lastPrompt.isEmpty()) {
            promptEditText.setText(lastPrompt);
        }

        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private List<Message> mapChatMessagesToUiMessages(List<ChatMessage> chatMessages) {
        List<Message> list = new ArrayList<>();
        for (ChatMessage cm : chatMessages) {
            list.add(new Message(cm.text, cm.sender, cm.timestamp));
        }
        return list;
    }

    private void getBotResponse(String userPrompt) {
        adapter.addMessage(new Message("Bot is typing...", "typing", System.currentTimeMillis()));
        int typingIndex = adapter.getItemCount() - 1;
        rvMessages.scrollToPosition(typingIndex);

        rvMessages.post(() -> generativeModel.generateContent(userPrompt, new Continuation<Object>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                runOnUiThread(() -> {
                    try {
                        String botText = ((GenerateContentResponse) o).getText();
                        if (botText == null) botText = "No response";
                        botText = botText.replace("**", "");

                        long timestamp = System.currentTimeMillis();

                        adapter.updateMessage(typingIndex, new Message(botText, "bot", timestamp));
                        rvMessages.scrollToPosition(adapter.getItemCount() - 1);

                        chatViewModel.insert(new ChatMessage(0, botText, "bot", timestamp));
                    } catch (Exception e) {
                        e.printStackTrace();
                        adapter.updateMessage(typingIndex, new Message(
                                "Error: " + e.getMessage(), "bot", System.currentTimeMillis()));
                        rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                    }
                });
            }
        }));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        applySavedPreferences();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        promptEditText = findViewById(R.id.promptEditText);
        rvMessages = findViewById(R.id.rvMessages);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        ImageButton micButton = findViewById(R.id.micButton);
        ImageButton btnDarkMode = findViewById(R.id.btnDarkMode);

        //RecyclerView & Adapter
        adapter = new MessagesAdapter(this);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        //ViewModel
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        chatViewModel.getAllMessages().observe(this, chatMessages -> {
            adapter.setMessages(mapChatMessagesToUiMessages(chatMessages));
            rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        });

        //Generative Model
        generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);

        //Voice Input
        micButton.setOnClickListener(v -> startVoiceInput());

        //Dark Mode Toggle
        btnDarkMode.setOnClickListener(v -> {
            boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);

            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                saveDarkMode(false);  // <-- call this here
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                saveDarkMode(true);   // <-- call this here
            }
            btnDarkMode.setImageTintList(ColorStateList.valueOf(
                    isDarkMode ? Color.BLACK : Color.WHITE
            ));
        });


        //Send Button
        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            if (!prompt.isEmpty()) {
                saveLastPrompt(prompt);
                ChatMessage userMsg = new ChatMessage(0, prompt, "user", System.currentTimeMillis());
                chatViewModel.insert(userMsg);
                adapter.addMessage(new Message(prompt, "user", System.currentTimeMillis()));
            }

            promptEditText.setError(null);
            long timestamp = System.currentTimeMillis();
            promptEditText.setText("");

            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(promptEditText.getWindowToken(), 0);

            adapter.addMessage(new Message(prompt, "user", timestamp));
            rvMessages.scrollToPosition(adapter.getItemCount() - 1);

            getBotResponse(prompt);
        });
        ImageButton btnClearChat = findViewById(R.id.btnClearChat);

        //Clear Chat
        btnClearChat.setOnClickListener(v -> {
            chatViewModel.clearAll();
            adapter.clearMessages();
        });

    }
}
