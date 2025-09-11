package com.shah.c058_mad_assignment1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.C058.Tanay.BuildConfig;
import com.C058.Tanay.R;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.shah.c058_mad_assignment1.database.AppDatabase;
import com.shah.c058_mad_assignment1.database.MessageChat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<Message> messages;
    private AppDatabase db;
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
        ImageButton deleteButton = findViewById(R.id.deleteButton);

        messages = new ArrayList<>();
        adapter = new MessageAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Initialize Room database
        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "chat_db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();

        // Load past messages
        new Thread(() -> {
            List<MessageChat> oldMessages = db.messageDao().getAllMessages();
            runOnUiThread(() -> {
                for (MessageChat msg : oldMessages) {
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
        generativeModel = new GenerativeModel("gemini-1.5-flash-latest", BuildConfig.API_KEY);

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
            speechRecognizerLauncher.launch(intent);
        });

        // Delete button click
        deleteButton.setOnClickListener(v -> {
            new Thread(() -> {
                db.messageDao().deleteAll();
                runOnUiThread(() -> {
                    messages.clear();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this, "Chat history deleted", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });
    }

    // New Activity Result Launcher for speech
    private final ActivityResultLauncher<Intent> speechRecognizerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> spokenResults = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (spokenResults != null && !spokenResults.isEmpty()) {
                        String spokenText = spokenResults.get(0);
                        promptEditText.setText(spokenText);
                        sendMessage(spokenText);
                    }
                }
            }
    );

    private void sendMessage(String prompt) {
        if (prompt.isEmpty()) return;

        promptEditText.setError(null);

        // Add user message to RecyclerView
        messages.add(new Message(prompt, true));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);
        promptEditText.setText("");

        // Save user message to Room
        new Thread(() -> db.messageDao().insert(new MessageChat(prompt, true))).start();

        progressBar.setVisibility(View.VISIBLE);

        // Run AI request on a background thread
        new Thread(() -> {
            try {
                String contextPrompt = buildChatContext() + "User: " + prompt;

                // This is a blocking call to the API using Kotlin's runBlocking
                GenerateContentResponse response = BuildersKt.runBlocking(
                        Dispatchers.getIO(),
                        (scope, continuation) -> generativeModel.generateContent(contextPrompt, (Continuation<? super GenerateContentResponse>) continuation)
                );

                String responseText = response.getText();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (responseText != null) {
                        messages.add(new Message(responseText, false));
                        adapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.scrollToPosition(messages.size() - 1);

                        // Save AI message to Room
                        new Thread(() -> db.messageDao().insert(new MessageChat(responseText, false))).start();
                    } else {
                        messages.add(new Message("No response from Gemini.", false));
                        adapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.scrollToPosition(messages.size() - 1);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
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
}