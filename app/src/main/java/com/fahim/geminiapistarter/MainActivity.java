package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;

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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private CircularProgressIndicator progressBar;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    private ImageButton darkModeToggle;

    private static final int VOICE_REQUEST_CODE = 100;
    private static final int RECORD_AUDIO_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPadding(0, systemBars.top, 0, 0);

            return insets;
        });

        promptEditText = findViewById(R.id.promptEditText);
        ImageButton sendButton = findViewById(R.id.sendButton);
        ImageButton micButton = findViewById(R.id.micButton);
        progressBar = findViewById(R.id.progressBar);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        darkModeToggle = findViewById(R.id.darkModeToggle);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Restore saved state (prompt + messages)
        if (savedInstanceState != null) {
            String promptText = savedInstanceState.getString("prompt_text");
            if (promptText != null) {
                promptEditText.setText(promptText);
            }
            List<ChatMessage> savedMessages =
                    (List<ChatMessage>) savedInstanceState.getSerializable("chat_messages");
            if (savedMessages != null) {
                chatMessages.clear();
                chatMessages.addAll(savedMessages);
                chatAdapter.notifyDataSetChanged();
            }
        }

        GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash",
                BuildConfig.API_KEY);

        Log.d("API_KEY_CHECK", "API Key: " + BuildConfig.API_KEY);

        sendButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString();
            promptEditText.setError(null);

            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }

            chatMessages.add(new ChatMessage(prompt, true));
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            promptEditText.setText("");

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
                    if (responseString == null) {
                        responseString = "No response received.";
                    }
                    String finalResponse = responseString;
                    Log.d("Response", finalResponse);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);
                        chatMessages.add(new ChatMessage(finalResponse, false));
                        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                    });
                }
            });
            Log.d("GeminiDebug", "Making API request with prompt: " + prompt);
        });

        micButton.setOnClickListener(v -> {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},
                        RECORD_AUDIO_REQUEST_CODE);
            } else {
                startVoiceRecognition();
            }
        });

        updateDarkModeIcon();
        darkModeToggle.setOnClickListener(v -> {
            int currentMode = AppCompatDelegate.getDefaultNightMode();
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            updateDarkModeIcon();
        });
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your prompt");
        startActivityForResult(intent, VOICE_REQUEST_CODE);
    }

    private void updateDarkModeIcon() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            darkModeToggle.setImageResource(R.drawable.ic_sunny_24px);
        } else {
            darkModeToggle.setImageResource(R.drawable.ic_nightlight_24px);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                promptEditText.setText(result.get(0));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("prompt_text", promptEditText.getText().toString());
        outState.putSerializable("chat_messages", new ArrayList<>(chatMessages));
    }
}
