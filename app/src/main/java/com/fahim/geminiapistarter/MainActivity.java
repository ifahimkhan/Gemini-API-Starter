package com.fahim.geminiapistarter;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fahim.geminiapistarter.adapter.ChatAdapter;
import com.fahim.geminiapistarter.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private androidx.recyclerview.widget.RecyclerView recyclerView;
    private android.widget.EditText promptEditText;
    private android.widget.ImageButton sendButton, voiceButton;
    private android.widget.ProgressBar progressBar;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;

    private static final int VOICE_REQ_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Initialize UI
        recyclerView = findViewById(R.id.recyclerView);
        promptEditText = findViewById(R.id.promptEditText);
        sendButton = findViewById(R.id.sendButton);
        voiceButton = findViewById(R.id.voiceButton);
        progressBar = findViewById(R.id.progressBar);

        // ✅ RecyclerView setup
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // ✅ Send text input
        sendButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }
            addUserMessage(prompt);
            generateResponse(prompt);
            promptEditText.setText("");
        });

        // ✅ Voice input
        voiceButton.setOnClickListener(v -> startVoiceInput());
    }

    private void addUserMessage(String text) {
        ChatMessage msg = new ChatMessage(text, true);
        chatAdapter.addMessage(msg);
        recyclerView.scrollToPosition(messages.size() - 1);
    }

    private void addGeminiMessage(String text) {
        ChatMessage msg = new ChatMessage(text, false);
        chatAdapter.addMessage(msg);
        recyclerView.scrollToPosition(messages.size() - 1);
    }

    private void generateResponse(String prompt) {
        progressBar.setVisibility(View.VISIBLE);

        // 🔹 TODO: Replace with Gemini API call
        new Thread(() -> {
            try {
                // Fake Gemini response (replace with API integration)
                Thread.sleep(2000); // simulate network delay
                String response = "Gemini says: " + prompt;

                runOnUiThread(() -> {
                    addGeminiMessage(response);
                    progressBar.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                });
                Log.e("GeminiAPI", "Error", e);
            }
        }).start();
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        try {
            startActivityForResult(intent, VOICE_REQ_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQ_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String voiceInput = result.get(0);
                addUserMessage(voiceInput);
                generateResponse(voiceInput);
            }
        }
    }
}
