package com.c013.ashmit_assignment1;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.c013.ashmit_assignment1.R;
import com.c013.ashmit_assignment1.database.ChatDatabase;
import com.c013.ashmit_assignment1.database.ChatMessage;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<Message> messages;
    private ChatDatabase db;
    private GenerativeModel generativeModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        promptEditText = findViewById(R.id.promptEditText);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        ImageButton micButton = findViewById(R.id.micButton);

        messages = new ArrayList<>();
        adapter = new MessageAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Initialize Room database
        db = Room.databaseBuilder(getApplicationContext(),
                        ChatDatabase.class, "chat_db")
                .allowMainThreadQueries() // for simplicity
                .build();

        // Load past messages
        new Thread(() -> {
            List<ChatMessage> oldMessages = db.chatDao().getAllMessages();
            runOnUiThread(() -> {
                for (ChatMessage msg : oldMessages) {
                    messages.add(new Message(msg.getText(), msg.isUser()));
                }
                adapter.notifyDataSetChanged();
            });
        }).start();

        // Load last prompt from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String lastPrompt = prefs.getString("last_prompt", "");
        promptEditText.setText(lastPrompt);

        // Initialize AI model
        generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);

        // Send button click
        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            sendMessage(prompt);
        });

        // Mic button click
        micButton.setOnClickListener(v -> {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            startActivityForResult(intent, 1001);
        });
    }

    // Helper method to send messages (user or speech)
    private void sendMessage(String prompt) {
        if (prompt.isEmpty()) return;

        promptEditText.setError(null);

        // Add user message to RecyclerView
        messages.add(new Message(prompt, true));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);
        promptEditText.setText("");

        // Save user message to Room
        new Thread(() -> db.chatDao().insert(new ChatMessage(prompt, true))).start();

        progressBar.setVisibility(VISIBLE);

        // Run AI request
        new Thread(() -> {
            try {
                // Build full context including previous messages
                String contextPrompt = buildChatContext() + "User: " + prompt;

                GenerateContentResponse response = BuildersKt.runBlocking(
                        (CoroutineContext) Dispatchers.getDefault(),
                        (scope, cont) -> generativeModel.generateContent(contextPrompt, (Continuation<? super GenerateContentResponse>) cont)
                );


                String responseText = response.getText();
                runOnUiThread(() -> {
                    progressBar.setVisibility(GONE);

                    if (responseText != null) {
                        messages.add(new Message(responseText, false));
                        adapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.scrollToPosition(messages.size() - 1);

                        // Save AI message to Room
                        new Thread(() -> db.chatDao().insert(new ChatMessage(responseText, false))).start();
                    } else {
                        messages.add(new Message("No response from Gemini.", false));
                        adapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.scrollToPosition(messages.size() - 1);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(GONE);
                    messages.add(new Message("Error: " + e.getMessage(), false));
                    adapter.notifyItemInserted(messages.size() - 1);
                    recyclerView.scrollToPosition(messages.size() - 1);
                });
            }
        }).start();




    }
    private String buildChatContext() {
        StringBuilder context = new StringBuilder();
        for (Message msg : messages) {
            if (msg.isUser()) {
                context.append("User: ").append(msg.getText()).append("\n");
            } else {
                context.append("AI: ").append(msg.getText()).append("\n");
            }
        }
        return context.toString();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                promptEditText.setText(spokenText);

                // Send automatically after speech
                sendMessage(spokenText);
            }
        }
    }
}
