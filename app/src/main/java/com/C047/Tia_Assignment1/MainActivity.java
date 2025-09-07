package com.C047.Tia_Assignment1;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

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
import androidx.room.Room;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private static final int SPEECH_REQUEST_CODE = 100;

    private EditText promptEditText;
    private ImageButton sendButton, voiceButton;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;

    private ChatAdapter chatAdapter;
    private List<Message> messages = new ArrayList<>();

    private AppDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private GenerativeModel generativeModel;

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

        // Initialize UI
        promptEditText = findViewById(R.id.promptEditText);
        sendButton = findViewById(R.id.sendButton);
        voiceButton = findViewById(R.id.voiceButton);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);

        // RecyclerView setup
        chatAdapter = new ChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // Room DB initialization
        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "chat_db")
                .build();

        loadMessagesFromDB();

        // Initialize Gemini model
        generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);

        sendButton.setOnClickListener(v -> sendMessage());
        voiceButton.setOnClickListener(v -> startVoiceInput());
    }

    private void loadMessagesFromDB() {
        executor.execute(() -> {
            List<MessageEntity> stored = db.messageDao().getAllMessages();
            for (MessageEntity me : stored) {
                messages.add(new Message(me.text, me.isUser));
            }
            runOnUiThread(() -> chatAdapter.notifyDataSetChanged());
        });
    }

    private void sendMessage() {
        String prompt = promptEditText.getText().toString().trim();
        if (prompt.isEmpty()) {
            promptEditText.setError("Message cannot be empty");
            return;
        }

        // Add user message
        addMessageToChat(new Message(prompt, true));

        // Clear input
        promptEditText.setText("");
        progressBar.setVisibility(VISIBLE);

        // Call Gemini API on background thread
        executor.execute(() -> {
            try {
                generativeModel.generateContent(prompt, new Continuation<>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object o) {
                        GenerateContentResponse response = (GenerateContentResponse) o;
                        String responseText = response.getText();
                        if (responseText == null) responseText = "No response";
                        String finalResponseText = responseText;
                        runOnUiThread(() -> {
                            addMessageToChat(new Message(finalResponseText, false));
                            progressBar.setVisibility(GONE);
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(GONE);
                });
            }
        });
    }

    private void addMessageToChat(Message message) {
        messages.add(message);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);

        // Save message to Room
        executor.execute(() -> {
            db.messageDao().insert(new MessageEntity(message.text, message.isUser));
        });
    }

    // Voice input
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                promptEditText.setText(results.get(0));
                sendMessage();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
