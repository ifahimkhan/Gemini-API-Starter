package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.content.SharedPreferences;


import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.util.ArrayList;
import java.util.List;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;


public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages = new ArrayList<>();
    private ImageButton sendButton, voiceButton;

    private ActivityResultLauncher<Intent> speechRecognitionLauncher;
    private static final String KEY_CHAT_HISTORY = "chat_history";
    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        promptEditText = findViewById(R.id.promptEditText);
        sendButton = findViewById(R.id.sendButton);
        voiceButton = findViewById(R.id.voiceButton);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.chatRecyclerView);
        sharedPreferences = getSharedPreferences("chat_prefs", MODE_PRIVATE);


        setupRecyclerView();
        loadChatHistory();
        setupSpeechRecognition();
        setupSendButton();
        setupVoiceButton();

    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatMessages);
        recyclerView.setAdapter(chatAdapter);
    }

    private void setupSpeechRecognition() {
        speechRecognitionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            promptEditText.setText(matches.get(0));
                        }
                    } else {
                        Toast.makeText(this, "Speech recognition failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupVoiceButton() {
        voiceButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 101);
            } else {
                startSpeechRecognition();
            }
        });
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "en-US");

        try {
            speechRecognitionLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            if (prompt.isEmpty()) {
                promptEditText.setError("Cannot be empty");
                return;
            }
            addUserMessage(prompt);
            promptEditText.setText("");
            sendToAI(prompt);
        });
    }

    // Call this in onCreate to load chat history
    private void loadChatHistory() {
        String json = sharedPreferences.getString(KEY_CHAT_HISTORY, null);
        if (json != null) {
            Type type = new TypeToken<List<ChatMessage>>(){}.getType();
            List<ChatMessage> savedMessages = new Gson().fromJson(json, type);
            if (savedMessages != null) {
                chatMessages.addAll(savedMessages);
                chatAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        }
    }

    // Call this whenever chatMessages changes
    private void saveChatHistory() {
        String json = new Gson().toJson(chatMessages);
        sharedPreferences.edit().putString(KEY_CHAT_HISTORY, json).apply();
    }

    private void addUserMessage(String message) {
        chatMessages.add(new ChatMessage(message, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);
        saveChatHistory();
    }

    private void sendToAI(String prompt) {
        progressBar.setVisibility(VISIBLE);
        GenerativeModel model = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);
        model.generateContent(prompt, new Continuation<>() {
            @NonNull
            @Override
            public CoroutineContext getContext() { return EmptyCoroutineContext.INSTANCE; }

            @Override
            public void resumeWith(@NonNull Object o) {
                GenerateContentResponse response = (GenerateContentResponse) o;
                runOnUiThread(() -> {
                    chatMessages.add(new ChatMessage(response.getText(), false));
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    recyclerView.scrollToPosition(chatMessages.size() - 1);
                    saveChatHistory();
                    progressBar.setVisibility(GONE);
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition();
        } else {
            Toast.makeText(this, "Audio permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
