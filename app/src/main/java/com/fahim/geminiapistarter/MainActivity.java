package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.content.Intent;
import android.util.Log;
import android.view.View;
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

    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private static final int SPEECH_REQUEST_CODE = 100;
    private ChatMessage typingIndicator;

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
        ImageButton voiceInputButton = findViewById(R.id.voiceInputButton);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatMessages, this);
        recyclerView.setAdapter(chatAdapter);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        loadChatHistory();

        GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);

        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            promptEditText.setError(null);

            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }

            sendMessage(prompt, generativeModel);
        });

        voiceInputButton.setOnClickListener(v -> startVoiceInput());
    }

    private void sendMessage(String prompt, GenerativeModel generativeModel) {
        progressBar.setVisibility(VISIBLE);

        chatMessages.add(new ChatMessage(prompt, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
        promptEditText.setText("");

        saveChatHistory();

        typingIndicator = new ChatMessage("AI is typing...", false);
        chatMessages.add(typingIndicator);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();

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

                if (responseString != null) {
                    Log.d("Response", responseString);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);

                        chatMessages.remove(typingIndicator);
                        chatAdapter.notifyItemRemoved(chatMessages.size());

                        chatMessages.add(new ChatMessage(responseString, false));
                        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                        scrollToBottom();

                        saveChatHistory();
                    });
                }
            }
        });
    }

    private void scrollToBottom() {
        recyclerView.post(() -> recyclerView.smoothScrollToPosition(chatMessages.size() - 1));
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

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

    private void saveChatHistory() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("chatSize", chatMessages.size());

        for (int i = 0; i < chatMessages.size(); i++) {
            editor.putString("chat_" + i, chatMessages.get(i).getMessage());
            editor.putBoolean("chat_isUser_" + i, chatMessages.get(i).isUser());
        }

        editor.apply();
    }

    private void loadChatHistory() {
        int chatSize = sharedPreferences.getInt("chatSize", 0);
        chatMessages.clear();

        for (int i = 0; i < chatSize; i++) {
            String message = sharedPreferences.getString("chat_" + i, "");
            boolean isUser = sharedPreferences.getBoolean("chat_isUser_" + i, true);
            chatMessages.add(new ChatMessage(message, isUser));
        }

        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }
    }
}