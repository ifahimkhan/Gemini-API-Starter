package com.c008.akshajramakrishnan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.SpannableStringBuilder;
import android.util.Pair;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
    private ResponseAdapter adapter;
    private List<Pair<String, SpannableStringBuilder>> responseList;

    private ImageButton micButton;
    private SpeechRecognizer speechRecognizer;

    private AppDatabase db;
    private ChatDao chatDao;

    private static final String PREFS_NAME = "UserPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setContentView(R.layout.activity_main);

        promptEditText = findViewById(R.id.promptEditText);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        micButton = findViewById(R.id.micButton);
        progressBar = findViewById(R.id.progressBar);

        RecyclerView recyclerView = findViewById(R.id.responseRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        responseList = new ArrayList<>();
        adapter = new ResponseAdapter(this, responseList);
        recyclerView.setAdapter(adapter);

        // Initialize Room database
        db = AppDatabase.getDatabase(this);
        chatDao = db.chatDao();

        // Load last prompt from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastPrompt = prefs.getString("last_prompt", "");
        promptEditText.setText(lastPrompt);

        // Load existing chat history from DB
        new Thread(() -> {
            List<Chat> chats = chatDao.getAllChats();
            runOnUiThread(() -> {
                for (Chat chat : chats) {
                    SpannableStringBuilder formattedResponse = TextFormatter.getBoldSpannableText(chat.getResponse());
                    responseList.add(new Pair<>(chat.getPrompt(), formattedResponse));
                    adapter.notifyItemInserted(responseList.size() - 1);
                }
            });
        }).start();

        // Initialize speech recognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        } else {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show();
        }

        // Mic button click
        micButton.setOnClickListener(v -> startListening());

        // Send button click
        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString();
            promptEditText.setError(null);

            if (prompt.isEmpty()) {
                promptEditText.setError("Field cannot be empty");
                return;
            }

            // Save prompt in SharedPreferences
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString("last_prompt", prompt);
            editor.apply();

            progressBar.setVisibility(android.view.View.VISIBLE);

            // Call your Generative AI API
            GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);
            generativeModel.generateContent(prompt, new Continuation<>() {
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(Object o) {
                    GenerateContentResponse response = (GenerateContentResponse) o;
                    String responseString = response.getText();
                    if (responseString == null) responseString = "No response";

                    String finalResponseString = responseString;
                    runOnUiThread(() -> {
                        progressBar.setVisibility(android.view.View.GONE);

                        // Add to RecyclerView
                        SpannableStringBuilder formattedResponse = TextFormatter.getBoldSpannableText(finalResponseString);
                        responseList.add(new Pair<>(prompt, formattedResponse));
                        adapter.notifyItemInserted(responseList.size() - 1);

                        // Save chat to Room database
                        new Thread(() -> chatDao.insert(new Chat(prompt, finalResponseString))).start();
                    });
                }
            });
        });
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { }
                @Override public void onBeginningOfSpeech() { }
                @Override public void onRmsChanged(float rmsdB) { }
                @Override public void onBufferReceived(byte[] buffer) { }
                @Override public void onEndOfSpeech() { }

                @Override
                public void onError(int error) {
                    Toast.makeText(MainActivity.this, "Error recognizing speech: " + error, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && matches.size() > 0) {
                        promptEditText.setText(matches.get(0));
                    }
                }

                @Override public void onPartialResults(Bundle partialResults) { }
                @Override public void onEvent(int eventType, Bundle params) { }
            });

            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Speech recognition failed", Toast.LENGTH_SHORT).show();
        }
    }
}
