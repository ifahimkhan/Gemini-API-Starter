package com.C147Tanush.assignment1;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
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

    private static final int VOICE_REQUEST_CODE = 100;

    private EditText promptEditText;
    private ProgressBar progressBar;

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<ChatMessage> messages;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_LAST_PROMPT = "last_prompt";
    private AppDatabase db;
    private MessageDao messageDao;

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

        ImageButton clearButton = findViewById(R.id.clearButton);

        db = androidx.room.Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "chat_db"
        ).allowMainThreadQueries().build();

        messageDao = db.messageDao();

        messages = new ArrayList<>();
        adapter = new MessageAdapter(messages);

        recyclerView = findViewById(R.id.recyclerViewMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Restore saved messages from DB
        List<MessageEntity> savedMessages = messageDao.getAllMessages();
        for (MessageEntity msg : savedMessages) {
            boolean isUser = msg.messageText.startsWith("You:");
            messages.add(new ChatMessage(msg.messageText, isUser));
        }
        adapter.notifyDataSetChanged();

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        promptEditText = findViewById(R.id.promptEditText);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        ImageButton voiceButton = findViewById(R.id.voiceButton);
        progressBar = findViewById(R.id.progressBar);

        String lastPrompt = sharedPreferences.getString(KEY_LAST_PROMPT, "");
        if (!lastPrompt.isEmpty()) {
            promptEditText.setText(lastPrompt);
        }

        GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);

        submitPromptButton.setOnClickListener(v -> handleUserPrompt(generativeModel));
        voiceButton.setOnClickListener(v -> startVoiceInput());

        clearButton.setOnClickListener(v -> {
            messageDao.clearMessages();
            messages.clear();
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleUserPrompt(GenerativeModel generativeModel) {
        String prompt = promptEditText.getText().toString().trim();
        promptEditText.setError(null);

        if (prompt.isEmpty()) {
            promptEditText.setError(getString(R.string.field_cannot_be_empty));
            return;
        }

        sharedPreferences.edit().putString(KEY_LAST_PROMPT, prompt).apply();

        // Add user message
        ChatMessage userMessage = new ChatMessage("You: " + prompt, true);
        messages.add(userMessage);
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);
        messageDao.insertMessage(new MessageEntity(userMessage.text, System.currentTimeMillis()));

        promptEditText.setText("");
        progressBar.setVisibility(VISIBLE);

        // AI Response
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

                runOnUiThread(() -> {
                    progressBar.setVisibility(GONE);

                    ChatMessage aiMessage = new ChatMessage("AI: " + responseString, false);
                    messages.add(aiMessage);
                    adapter.notifyItemInserted(messages.size() - 1);
                    recyclerView.scrollToPosition(messages.size() - 1);
                    messageDao.insertMessage(new MessageEntity(aiMessage.text, System.currentTimeMillis()));
                });
            }
        });
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your query...");

        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                promptEditText.setText(spokenText);
            }
        }
    }
}
