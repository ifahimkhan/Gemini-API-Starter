package com.fahim.sharvil;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.ActivityNotFoundException;
import android.content.Intent;
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

    private static final int MAX_HISTORY = 20; // last 20 messages
    private static final int SPEECH_REQUEST_CODE = 100;

    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> chatList = new ArrayList<>();

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
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        ImageButton voiceButton = findViewById(R.id.mic); // mic button
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.chats);

        // RecyclerView setup
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(chatList);
        recyclerView.setAdapter(chatAdapter);

        // GenerativeModel
        GenerativeModel generativeModel = new GenerativeModel(
                "gemini-2.0-flash",
                BuildConfig.API_KEY
        );

        // Handle Send button
        submitPromptButton.setOnClickListener(v -> sendMessage(generativeModel));

        // Handle Voice button
        voiceButton.setOnClickListener(v -> startVoiceInput());
    }

    /** Send typed message to Gemini */
    private void sendMessage(GenerativeModel generativeModel) {
        String prompt = promptEditText.getText().toString().trim();
        promptEditText.setError(null);

        if (prompt.isEmpty()) {
            promptEditText.setError(getString(R.string.field_cannot_be_empty));
            return;
        }

        addMessage(new ChatMessage(prompt, true));
        promptEditText.setText("");
        progressBar.setVisibility(VISIBLE);

        // Build context
        String fullPrompt = buildContext() + "\nUser: " + prompt;

        // Call Gemini
        generativeModel.generateContent(fullPrompt, new Continuation<>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                GenerateContentResponse response = (GenerateContentResponse) o;
                String responseString = response.getText();
                if (responseString == null) responseString = "⚠️ No response";

                String cleanedResponse = cleanResponse(responseString);
                Log.d("GeminiResponse", cleanedResponse);

                runOnUiThread(() -> {
                    progressBar.setVisibility(GONE);
                    addMessage(new ChatMessage(cleanedResponse, false));
                });
            }
        });
    }

    /** Add message to chat with scroll behavior */
    private void addMessage(ChatMessage message) {
        if (chatList.size() >= MAX_HISTORY) chatList.remove(0);
        chatList.add(message);
        chatAdapter.notifyItemInserted(chatList.size() - 1);

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            int lastVisible = layoutManager.findLastCompletelyVisibleItemPosition();
            if (lastVisible >= chatList.size() - 2) {
                recyclerView.smoothScrollToPosition(chatList.size() - 1);
            }
        }
    }

    /** Build conversation context for Gemini */
    private String buildContext() {
        StringBuilder context = new StringBuilder();
        int start = Math.max(0, chatList.size() - MAX_HISTORY);
        for (int i = start; i < chatList.size(); i++) {
            ChatMessage msg = chatList.get(i);
            if (msg.isUser()) {
                context.append("User: ").append(msg.getMessage()).append("\n");
            } else {
                context.append("Bot: ").append(msg.getMessage()).append("\n");
            }
        }
        return context.toString();
    }

    /** Clean API response (remove markdown) */
    private String cleanResponse(String rawText) {
        return rawText
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("\\*(.*?)\\*", "$1")
                .replaceAll("`{1,3}(.*?)`{1,3}", "$1")
                .replaceAll("#+", "")
                .replaceAll(">+", "")
                .trim();
    }

    /** Start speech-to-text input */
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your query");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    /** Handle speech result */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                promptEditText.setText(result.get(0));
            }
        }
    }
}
