package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private SharedPreferences preferences;
    private boolean isDarkMode = false;
    
    // Voice recognition
    private SpeechRecognizer speechRecognizer;
    private FloatingActionButton voiceButton;
    private boolean isListening = false;
    private static final int RECORD_AUDIO_PERMISSION_CODE = 1;
    private static final int SPEECH_REQUEST_CODE = 100;
    
    // User preferences and personalization
    private UserPreferencesManager preferencesManager;
    private UserProfile userProfile;
    private ConversationSession currentSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Initialize user preferences and personalization
        preferencesManager = new UserPreferencesManager(this);
        userProfile = preferencesManager.getUserProfile();
        currentSession = preferencesManager.getCurrentSession();
        
        // Apply user's theme preference
        isDarkMode = userProfile.isDarkModeEnabled();
        applyTheme();
        
        // Initialize views
        promptEditText = findViewById(R.id.promptEditText);
        FloatingActionButton sendButton = findViewById(R.id.sendButton);
        voiceButton = findViewById(R.id.voiceButton);
        progressBar = findViewById(R.id.progressBar);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        
        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        
        // Setup RecyclerView
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(false);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setHasFixedSize(false);
        chatRecyclerView.setNestedScrollingEnabled(true);
        
        // Load conversation history or add welcome message
        if (currentSession.getMessages().isEmpty()) {
            String welcomeMessage = "Hello " + userProfile.getUserName() + "! How can I help you today?";
            chatAdapter.addMessage(new ChatMessage(welcomeMessage, false));
            currentSession.addMessage(new ChatMessage(welcomeMessage, false));
        } else {
            // Load existing conversation
            for (ChatMessage message : currentSession.getMessages()) {
                chatAdapter.addMessage(message);
            }
        }
        
        // Initialize speech recognition
        initializeSpeechRecognition();


        // Create GenerativeModel
        GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash",
                BuildConfig.API_KEY);

        // Voice button click listener
        voiceButton.setOnClickListener(v -> {
            Log.d("Speech", "Voice button clicked, isListening: " + isListening);
            
            // Test if button click is working
            Toast.makeText(this, "Voice button clicked!", Toast.LENGTH_SHORT).show();
            
            if (isListening) {
                Log.d("Speech", "Stopping listening");
                stopListening();
            } else {
                Log.d("Speech", "Starting listening");
                // Try the simple approach first
                startVoiceInput();
            }
        });

        sendButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }
            
            // Add user message to chat and session
            ChatMessage userMessage = new ChatMessage(prompt, true);
            chatAdapter.addMessage(userMessage);
            currentSession.addMessage(userMessage);
            userProfile.incrementMessageCount();
            userProfile.updateLastActiveTime();
            
            promptEditText.setText("");
            
            // Scroll to bottom smoothly
            chatRecyclerView.post(() -> {
                chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            });
            
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
                    assert responseString != null;
                    Log.d("Response", responseString);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);
                        // Add AI response to chat and session
                        ChatMessage aiMessage = new ChatMessage(responseString, false);
                        chatAdapter.addMessage(aiMessage);
                        currentSession.addMessage(aiMessage);
                        
                        // Save conversation state
                        preferencesManager.updateCurrentSession(currentSession);
                        preferencesManager.saveUserProfile(userProfile);
                        
                        // Scroll to bottom smoothly
                        chatRecyclerView.post(() -> {
                            chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                        });
                    });
                }
            });
        });

    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_user_settings) {
            Intent settingsIntent = new Intent(this, UserSettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        } else if (item.getItemId() == R.id.action_conversation_history) {
            showConversationHistory();
            return true;
        } else if (item.getItemId() == R.id.action_toggle_theme) {
            toggleDarkMode();
            return true;
        } else if (item.getItemId() == R.id.action_clear_chat) {
            clearCurrentChat();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        preferencesManager.setDarkModeEnabled(isDarkMode);
        userProfile.setDarkModeEnabled(isDarkMode);
        preferencesManager.saveUserProfile(userProfile);
        applyTheme();
        recreate();
    }
    
    private void clearCurrentChat() {
        // Save current session to history
        if (currentSession.getMessageCount() > 1) { // More than just welcome message
            currentSession.endSession();
            currentSession.setSessionTitle(currentSession.getFirstUserMessage());
            preferencesManager.addConversationSession(currentSession);
        }
        
        // Clear current chat
        chatAdapter.clearMessages();
        currentSession = new ConversationSession();
        preferencesManager.clearCurrentSession();
        
        // Add new welcome message
        String welcomeMessage = "Hello " + userProfile.getUserName() + "! How can I help you today?";
        chatAdapter.addMessage(new ChatMessage(welcomeMessage, false));
        currentSession.addMessage(new ChatMessage(welcomeMessage, false));
    }
    
    private void showConversationHistory() {
        // For now, show a simple dialog with conversation count
        List<ConversationSession> history = preferencesManager.getConversationHistory();
        String message = "You have " + history.size() + " saved conversations.\n";
        message += "Total messages: " + userProfile.getTotalMessages() + "\n";
        message += "Last active: " + new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date(userProfile.getLastActiveTime()));
        
        new android.app.AlertDialog.Builder(this)
                .setTitle("Conversation History")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
    
    private void applyTheme() {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    
    private void initializeSpeechRecognition() {
        // Check if speech recognition is available
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                if (speechRecognizer != null) {
                    speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d("Speech", "Ready for speech");
                    runOnUiThread(() -> {
                        voiceButton.setImageResource(R.drawable.baseline_mic_off_24);
                        Toast.makeText(MainActivity.this, "Listening...", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d("Speech", "Beginning of speech");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Optional: Update UI based on audio level
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    // Not used
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d("Speech", "End of speech");
                }

                @Override
                public void onError(int error) {
                    Log.e("Speech", "Error: " + error);
                    runOnUiThread(() -> {
                        isListening = false;
                        voiceButton.setImageResource(R.drawable.baseline_mic_24);
                        String errorMessage = getErrorMessage(error);
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResults(Bundle results) {
                    runOnUiThread(() -> {
                        isListening = false;
                        voiceButton.setImageResource(R.drawable.baseline_mic_24);
                        
                        if (results != null) {
                            java.util.ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                            if (matches != null && !matches.isEmpty()) {
                                String spokenText = matches.get(0);
                                promptEditText.setText(spokenText);
                                Toast.makeText(MainActivity.this, "Voice input received", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    // Optional: Show partial results
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                    // Not used
                }
                    });
                } else {
                    Log.e("Speech", "Failed to create SpeechRecognizer");
                    voiceButton.setEnabled(false);
                    Toast.makeText(this, "Failed to initialize speech recognition", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e("Speech", "Error initializing speech recognition", e);
                voiceButton.setEnabled(false);
                Toast.makeText(this, "Error initializing speech recognition", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.e("Speech", "Speech recognition not available");
            voiceButton.setEnabled(false);
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show();
        }
    }
    
    private void startListening() {
        Log.d("Speech", "startListening called");
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Speech", "Requesting microphone permission");
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.RECORD_AUDIO}, 
                    RECORD_AUDIO_PERMISSION_CODE);
            return;
        }
        
        if (speechRecognizer == null) {
            Log.e("Speech", "SpeechRecognizer is null");
            Toast.makeText(this, "Speech recognition not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            
            Log.d("Speech", "Starting speech recognition");
            isListening = true;
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e("Speech", "Error starting speech recognition", e);
            Toast.makeText(this, "Error starting speech recognition: " + e.getMessage(), Toast.LENGTH_LONG).show();
            isListening = false;
            voiceButton.setImageResource(R.drawable.baseline_mic_24);
        }
    }
    
    private void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            voiceButton.setImageResource(R.drawable.baseline_mic_24);
        }
    }
    
    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech input recognized";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error occurred";
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(this, "Microphone permission required for voice input", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void startVoiceInput() {
        Log.d("Speech", "startVoiceInput called");
        
        // Test if we can access the EditText
        if (promptEditText == null) {
            Log.e("Speech", "promptEditText is null!");
            Toast.makeText(this, "Text input not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Speech", "Requesting microphone permission");
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.RECORD_AUDIO}, 
                    RECORD_AUDIO_PERMISSION_CODE);
            return;
        }
        
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            
            Log.d("Speech", "Starting speech recognition activity");
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Log.e("Speech", "Error starting speech recognition activity", e);
            Toast.makeText(this, "Error starting voice input: " + e.getMessage(), Toast.LENGTH_LONG).show();
            
            // Fallback: Add test text to verify text input works
            String currentText = promptEditText.getText().toString();
            if (currentText.isEmpty()) {
                promptEditText.setText("Test voice input - " + System.currentTimeMillis());
            } else {
                promptEditText.setText(currentText + " Test voice input - " + System.currentTimeMillis());
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d("Speech", "onActivityResult called - requestCode: " + requestCode + ", resultCode: " + resultCode);
        
        if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Log.d("Speech", "Speech recognition successful");
                java.util.ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                Log.d("Speech", "Results: " + (result != null ? result.toString() : "null"));
                
                if (result != null && !result.isEmpty()) {
                    String spokenText = result.get(0);
                    Log.d("Speech", "Voice input received: '" + spokenText + "'");
                    
                    // Append to existing text instead of replacing
                    String currentText = promptEditText.getText().toString();
                    if (currentText.isEmpty()) {
                        promptEditText.setText(spokenText);
                    } else {
                        promptEditText.setText(currentText + " " + spokenText);
                    }
                    
                    // Move cursor to end
                    promptEditText.setSelection(promptEditText.getText().length());
                    
                    Toast.makeText(this, "Voice input received: " + spokenText, Toast.LENGTH_LONG).show();
                } else {
                    Log.d("Speech", "No voice input received - empty results");
                    Toast.makeText(this, "No speech detected", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d("Speech", "Speech recognition cancelled or failed - resultCode: " + resultCode);
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Voice input cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Voice input failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}