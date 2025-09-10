package com.C029.AryaChawan;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    private EditText promptEditText;
    private ImageButton sendButton, voiceButton;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;

    private ChatAdapter chatAdapter;
    private List<Message> messages = new ArrayList<>();

    private AppDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private GenerativeModel generativeModel;

    // ✅ Modern Activity Result API for voice input
    private ActivityResultLauncher<Intent> voiceLauncher;

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
        sendButton = findViewById(R.id.sendButton);
        voiceButton = findViewById(R.id.voiceButton);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);

        chatAdapter = new ChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "chat_db")
                .build();

        loadMessagesFromDB();

        generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);

        sendButton.setOnClickListener(v -> sendMessage());

        // ✅ Initialize voice input launcher
        voiceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> results = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (results != null && !results.isEmpty()) {
                            promptEditText.setText(results.get(0));
                            sendMessage();
                        }
                    }
                });

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

        addMessageToChat(new Message(prompt, true));
        promptEditText.setText("");
        progressBar.setVisibility(VISIBLE);

        executor.execute(() -> {
            try {
                generativeModel.generateContent(prompt, new Continuation<GenerateContentResponse>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object o) {
                        if (o instanceof Throwable) {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Error: " + ((Throwable) o).getMessage(), Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(GONE);
                            });
                        } else {
                            GenerateContentResponse response = (GenerateContentResponse) o;
                            String responseText = response.getText();
                            if (responseText == null) responseText = "No response";
                            String finalResponseText = responseText;
                            runOnUiThread(() -> {
                                addMessageToChat(new Message(finalResponseText, false));
                                progressBar.setVisibility(GONE);
                            });
                        }
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

        executor.execute(() -> db.messageDao().insert(new MessageEntity(message.text, message.isUser)));
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        try {
            voiceLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
