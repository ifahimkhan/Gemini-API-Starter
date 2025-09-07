package com.c178.parthacharya;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

    private static final int SPEECH_REQUEST_CODE = 100;
    private static final int RECORD_AUDIO_PERMISSION_CODE = 101;

    private EditText promptEditText;
    private ProgressBar progressBar;
    private ResponseAdapter responseAdapter;
    private RecyclerView recyclerView;
    private TextView welcomeTextView;
    private ImageButton voiceInputButton;
    private Button clearHistoryButton;

    private final ArrayList<ChatMessage> chatMessageEntities = new ArrayList<>();

    private AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initializeUI();
        setupRecyclerView();
        setupSendButton();
        setupVoiceInputButton();
        setupClearHistoryButton();

        appDatabase = AppDatabase.getInstance(this);
        loadChatHistory();
    }

    private void initializeUI() {
        welcomeTextView = findViewById(R.id.welcomeTextView);
        promptEditText = findViewById(R.id.promptEditText);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.responseRecyclerView);
        voiceInputButton = findViewById(R.id.micButton);
        clearHistoryButton = findViewById(R.id.clearHistoryButton);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        responseAdapter = new ResponseAdapter(this, chatMessageEntities);
        recyclerView.setAdapter(responseAdapter);
    }

    private void setupSendButton() {
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString();
            promptEditText.setError(null);

            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                chatMessageEntities.add(new ChatMessage(
                        "", getString(R.string.aistring)
                ));
                runOnUiThread(() -> {
                    responseAdapter.notifyItemInserted(chatMessageEntities.size() - 1);
                    recyclerView.scrollToPosition(chatMessageEntities.size() - 1);
                    if (recyclerView.getItemAnimator() != null) {
                        recyclerView.getItemAnimator().setChangeDuration(250);
                    }
                });
                return;
            }

            // Clear input box immediately after reading input
            promptEditText.setText("");

            progressBar.setVisibility(VISIBLE);

            GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);
            generativeModel.generateContent(prompt, new Continuation<GenerateContentResponse>() {
                @Override
                public @NonNull CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    GenerateContentResponse response = (GenerateContentResponse) o;
                    String responseString = response.getText();
                    assert responseString != null;
                    Log.d("Response", responseString);

                    new Thread(() -> {
                        appDatabase.chatMessageDao().insert(new ChatMessage(prompt, responseString));
                    }).start();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);
                        Spannable userText = getPrefixedText("Me: ", prompt);
                        Spannable aiText = getPrefixedText("AI: ", responseString);
                        chatMessageEntities.add(new ChatMessage(userText.toString(), aiText.toString()));
                        responseAdapter.notifyItemInserted(chatMessageEntities.size() - 1);
                        recyclerView.scrollToPosition(chatMessageEntities.size() - 1);
                        if (recyclerView.getItemAnimator() != null) {
                            recyclerView.getItemAnimator().setChangeDuration(250);
                        }
                    });
                }
            });
        });
    }

    private void setupVoiceInputButton() {
        voiceInputButton.setOnClickListener(v -> checkAudioPermissionAndStart());
    }

    private void setupClearHistoryButton() {
        clearHistoryButton.setOnClickListener(v -> clearHistoryFromDb());
    }

    private void loadChatHistory() {
        new Thread(() -> {
            List<ChatMessage> savedMessages = appDatabase.chatMessageDao().getAllMessages();
            for (ChatMessage entity : savedMessages) {
                ChatMessage chatMessage = new ChatMessage(
                        getPrefixedText("Me: ", entity.userMessage).toString(),
                        getPrefixedText("AI: ", entity.aiResponse).toString());
                chatMessageEntities.add(chatMessage);
            }
            runOnUiThread(() -> {
                responseAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(chatMessageEntities.size() - 1);
            });
        }).start();
    }

    private void clearHistoryFromDb() {
        new Thread(() -> {
            appDatabase.chatMessageDao().deleteAll();
            runOnUiThread(() -> {
                chatMessageEntities.clear();
                responseAdapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Chat history cleared", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void checkAudioPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION_CODE);
        } else {
            startSpeechRecognition();
        }
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your query");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech input not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                promptEditText.setText(results.get(0));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition();
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static SpannableStringBuilder getPrefixedText(String prefix, String content) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(prefix);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, prefix.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(content);
        return ssb;
    }
}
