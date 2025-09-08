package com.example.c110_siddhamshah;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

// NEW IMPORTS ADDED FOR ListenableFuture FUNCTIONALITY
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

import android.widget.Switch;
import androidx.appcompat.app.AppCompatDelegate;


public class MainActivity extends AppCompatActivity {
    private static final int RECORD_AUDIO_PERMISSION_CODE = 1;
    private static final String API_KEY = "YOUR_GEMINI_API_KEY_HERE";

    private EditText editTextQuery;
    private ImageButton buttonSend, buttonVoice;
    private RecyclerView recyclerViewChat;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private SpeechRecognizer speechRecognizer;
    private SharedPreferences sharedPreferences;
    private ChatDatabase chatDatabase;
    private Executor executor;
    private View emptyStateLayout;

    private Switch switchDarkMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerView();
        setupSpeechRecognizer();
        setupDatabase();
        loadChatHistory();
        setupDarkMode();


        buttonSend.setOnClickListener(v -> sendMessage());
        buttonVoice.setOnClickListener(v -> startVoiceInput());
    }

    private void initializeViews() {
        editTextQuery = findViewById(R.id.editTextQuery);
        buttonSend = findViewById(R.id.buttonSend);
        buttonVoice = findViewById(R.id.buttonVoice);
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        switchDarkMode = findViewById(R.id.switchDarkMode);


        sharedPreferences = getSharedPreferences("GeminiApp", MODE_PRIVATE);
        executor = Executors.newSingleThreadExecutor();
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(chatAdapter);
    }

    private void setupDatabase() {
        chatDatabase = ChatDatabase.getInstance(this);
    }

    private void sendMessage() {
        String query = editTextQuery.getText().toString().trim();
        if (!query.isEmpty()) {
            addMessageToChat(query, true);
            editTextQuery.setText("");
            callGeminiAPI(query);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    private void callGeminiAPI(String query) {
        // Check internet connection first
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
            return;
        }

        executor.execute(() -> {
            // Try one reliable model first
            try {
                GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY);
                GenerativeModelFutures model = GenerativeModelFutures.from(gm);

                Content content = new Content.Builder()
                        .addText(query)
                        .build();

                ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(content);
                GenerateContentResponse response = responseFuture.get(); // This blocks until complete
                String responseText = response.getText();

                runOnUiThread(() -> {
                    addMessageToChat(responseText, false);
                    saveChatToDatabase(query, responseText);
                });

            } catch (Exception e) {
                Log.e("API_ERROR", "Gemini API Error: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }



    private void setupDarkMode() {
        boolean isDark = sharedPreferences.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDark);
        applyTheme(isDark);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            applyTheme(isChecked);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("dark_mode", isChecked);
            editor.apply();
            Toast.makeText(this, isChecked ? "Dark mode enabled" : "Light mode enabled", Toast.LENGTH_SHORT).show();
        });
    }

    private void applyTheme(boolean isDark) {
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }


    private void addMessageToChat(String message, boolean isUser) {
        // Hide welcome message when first message is added
        if (emptyStateLayout.getVisibility() == View.VISIBLE) {
            emptyStateLayout.setVisibility(View.GONE);
        }

        ChatMessage chatMessage = new ChatMessage(message, isUser, System.currentTimeMillis());
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerViewChat.scrollToPosition(chatMessages.size() - 1);
    }


    // Voice input implementation
    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                Log.d("SPEECH_DEBUG", "Ready for speech");
            }

            @Override public void onBeginningOfSpeech() {
                Log.d("SPEECH_DEBUG", "Speech started");
            }

            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                Log.d("SPEECH_DEBUG", "Speech ended");
            }

            @Override
            public void onError(int error) {
                String errorMessage = getErrorText(error);
                Log.e("SPEECH_ERROR", "Speech recognition error: " + errorMessage);
                Toast.makeText(MainActivity.this, "Speech error: " + errorMessage, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResults(Bundle results) {
                List<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    Log.d("SPEECH_DEBUG", "Speech result: " + matches.get(0));
                    editTextQuery.setText(matches.get(0));
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    // Add this helper method to understand errors
    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "RecognitionService busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }


    private void startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d("SPEECH_DEBUG", "Audio permission not granted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_CODE);
        } else {
            Log.d("SPEECH_DEBUG", "Starting speech recognition");
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizer.startListening(intent);
        }
    }


    private void saveChatToDatabase(String userMessage, String geminiResponse) {
        executor.execute(() -> {
            ChatEntity chatEntity = new ChatEntity(userMessage, geminiResponse, System.currentTimeMillis());
            chatDatabase.chatDao().insertChat(chatEntity);
        });
    }

    private void loadChatHistory() {
        executor.execute(() -> {
            List<ChatEntity> history = chatDatabase.chatDao().getAllChats();
            runOnUiThread(() -> {
                for (ChatEntity chat : history) {
                    addMessageToChat(chat.userMessage, true);
                    addMessageToChat(chat.geminiResponse, false);
                }
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("SPEECH_DEBUG", "Audio permission granted");
                startVoiceInput();
            } else {
                Log.e("SPEECH_ERROR", "Audio permission denied");
                Toast.makeText(this, "Audio permission is required for voice input", Toast.LENGTH_LONG).show();
            }
        }
    }
    private boolean hasMicrophone() {
        PackageManager packageManager = getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }


}

