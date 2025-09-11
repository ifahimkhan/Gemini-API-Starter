package com.C162.AhaanMehta;

import android.Manifest;
import android.content.Context; // Added for clarity
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
// import android.widget.ImageButton; // Already imported if btnMic is ImageButton
import android.widget.Toast;

import androidx.annotation.NonNull; // Added for onOptionsItemSelected
import androidx.appcompat.app.AlertDialog; // For confirmation dialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.C162.AhaanMehta.databinding.ActivityMainBinding;

import java.io.IOException; // Added for OkHttp
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody; // Added for explicit try-with-resources



public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // For logging
    private ActivityMainBinding binding;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages = new ArrayList<>(); // Initialize here
    private static final int REQ_SPEECH_INPUT = 1001; // Renamed for clarity
    private static final int REQ_RECORD_AUDIO_PERMISSION = 2001; // For voice input permission

    private SharedPreferences prefs;
    private AppDatabase db;
    private final OkHttpClient client = new OkHttpClient(); // Re-use OkHttpClient instance
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // Re-use ExecutorService

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Context is available after this call

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize SharedPreferences
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // Apply saved dark mode setting
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        binding.switchDark.setChecked(isDarkMode);

        binding.switchDark.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean("dark_mode", checked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Initialize Room Database
        // It's safe to initialize db here as 'this' (applicationContext) is valid
        db = AppDatabase.getInstance(getApplicationContext()); // Use applicationContext

        // Setup RecyclerView
        messages = new ArrayList<>(); // Ensure messages is initialized before adapter
        chatAdapter = new ChatAdapter(messages);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(chatAdapter);

        loadMessagesFromDB();

        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etPrompt.getText().toString().trim();
            if (!text.isEmpty()) {
                addMessageToUIAndSave(new ChatMessage(text, true)); // User message
                binding.etPrompt.setText("");
                callGeminiAPI(text);
            }
        });

        binding.btnMic.setOnClickListener(v -> startVoiceInput());
    }

    private void startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO_PERMISSION);
        } else {
            launchSpeechRecognizer();
        }
    }

    private void launchSpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your prompt");

        try {
            startActivityForResult(intent, REQ_SPEECH_INPUT);
        } catch (Exception e) {
            Log.e(TAG, "Speech recognition not available", e);
            Toast.makeText(this, "Speech not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchSpeechRecognizer();
            } else {
                Toast.makeText(this, "Audio permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                binding.etPrompt.setText(results.get(0));
                binding.etPrompt.setSelection(binding.etPrompt.length()); // Move cursor to end
            }
        }
    }

    private void addMessageToUIAndSave(ChatMessage chatMessage) {
        // Add to UI
        messages.add(chatMessage);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        binding.recyclerView.scrollToPosition(messages.size() - 1);

        // Save to DB in background
        executorService.execute(() -> {
            MessageEntity entity = new MessageEntity();
            entity.text = chatMessage.getMessage();
            entity.isUser = chatMessage.isUser();
            entity.timestamp = System.currentTimeMillis();
            try {
                db.messageDao().insert(entity);
                Log.d(TAG, "Message saved to DB: " + entity.text);
            } catch (Exception e) {
                Log.e(TAG, "Error saving message to DB", e);
            }
        });
    }

    private void callGeminiAPI(String prompt) {
        executorService.execute(() -> {
            String apiKey = BuildConfig.API_KEY; // Make sure BuildConfig is imported and API_KEY is valid
            // IMPORTANT: Replace with a model you have access to, e.g., "gemini-1.5-flash-latest"
            String modelName = "gemini-1.5-flash-latest"; // <<< CHANGE THIS TO A VALID, ACCESSIBLE MODEL
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            String requestJson = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJsonString(prompt) + "\"}]}]}";
            RequestBody body = RequestBody.create(requestJson, mediaType);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json") // Good practice
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error, no body";
                    Log.e(TAG, "API Error: " + response.code() + " - " + errorBody);
                    runOnUiThread(() -> addMessageToUIAndSave(new ChatMessage("⚠ API Error: " + response.code() + " " + trimError(errorBody), false)));
                    return;
                }

                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String responseString = responseBody.string();
                    Log.d(TAG, "Raw API response: " + responseString);
                    String aiText = parseGeminiResponse(responseString);

                    if (aiText.startsWith("Error:")) { // If parser returned an error message
                        Log.e(TAG, "Parsed response indicates error: " + aiText);
                    } else {
                        Log.d(TAG, "Parsed AI text: " + aiText);
                    }
                    runOnUiThread(() -> addMessageToUIAndSave(new ChatMessage(aiText, false)));
                } else {
                    Log.e(TAG, "API response body was null");
                    runOnUiThread(() -> addMessageToUIAndSave(new ChatMessage("⚠ Error: Empty response from API", false)));
                }
            } catch (IOException e) {
                Log.e(TAG, "API Call failed", e);
                runOnUiThread(() -> addMessageToUIAndSave(new ChatMessage("⚠ Network Error: " + e.getMessage(), false)));
            }
        });
    }

    // Helper to escape strings for JSON
    private String escapeJsonString(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Helper to trim long error messages for display
    private String trimError(String errorMsg) {
        if (errorMsg.length() > 150) {
            try {
                // Attempt to get the "message" field from an error JSON
                org.json.JSONObject errorJson = new org.json.JSONObject(errorMsg);
                if (errorJson.has("error") && errorJson.getJSONObject("error").has("message")) {
                    return errorJson.getJSONObject("error").getString("message");
                }
            } catch (Exception e) {
                // fallback to simple trim
            }
            return errorMsg.substring(0, 147) + "...";
        }
        return errorMsg;
    }


    private String parseGeminiResponse(String json) {
        String parsedText = "Error: Could not parse AI response."; // Default error
        Log.d("GeminiParser", "Attempting to parse: " + json);
        try {
            org.json.JSONObject obj = new org.json.JSONObject(json);

            if (obj.has("error")) { // Check for API-level error object first
                org.json.JSONObject errorObj = obj.getJSONObject("error");
                String errorMessage = errorObj.optString("message", "Unknown API error structure.");
                Log.e("GeminiParser", "API returned an error object: " + errorMessage);
                return "Error: API - " + errorMessage;
            }

            if (!obj.has("candidates")) {
                Log.e("GeminiParser", "JSON response does not contain 'candidates' array. Full response: " + json);
                return "Error: Response format unexpected (no candidates)";
            }
            org.json.JSONArray candidates = obj.getJSONArray("candidates");

            if (candidates.length() > 0) {
                org.json.JSONObject firstCandidate = candidates.getJSONObject(0);
                if (!firstCandidate.has("content")) {
                    Log.e("GeminiParser", "Candidate 0 does not have 'content'. Full response: " + json);
                    return "Error: Response format unexpected (no content in candidate)";
                }
                org.json.JSONObject content = firstCandidate.getJSONObject("content");

                if (!content.has("parts")) {
                    Log.e("GeminiParser", "Content does not have 'parts'. Full response: " + json);
                    return "Error: Response format unexpected (no parts in content)";
                }
                org.json.JSONArray parts = content.getJSONArray("parts");

                if (parts.length() > 0) {
                    org.json.JSONObject firstPart = parts.getJSONObject(0);
                    if (!firstPart.has("text")) {
                        Log.e("GeminiParser", "Part 0 does not have 'text'. Full response: " + json);
                        return "Error: Response format unexpected (no text in part)";
                    }
                    parsedText = firstPart.getString("text");
                    Log.d("GeminiParser", "Parsed text: " + parsedText);
                } else {
                    Log.e("GeminiParser", "'parts' array is empty. Full response: " + json);
                    parsedText = "Error: No text parts in response";
                }
            } else {
                Log.e("GeminiParser", "'candidates' array is empty. Full response: " + json);
                parsedText = "Error: No candidates in response";
            }
        } catch (org.json.JSONException e) {
            Log.e("GeminiParser", "JSON parsing error: " + e.getMessage() + ". Full response: " + json, e);
            parsedText = "Error: Could not parse AI response (JSONException).";
        } catch (Exception e) { // Catch other potential exceptions
            Log.e("GeminiParser", "Unexpected error during parsing: " + e.getMessage() + ". Full response: " + json, e);
            parsedText = "Error: Unexpected issue parsing AI response.";
        }
        return parsedText;
    }

    private void loadMessagesFromDB() {
        executorService.execute(() -> {
            List<MessageEntity> savedMessages = db.messageDao().getAll();

            runOnUiThread(() -> {
                messages.clear(); // 🔑 never replace the list
                if (!savedMessages.isEmpty()) {
                    for (MessageEntity entity : savedMessages) {
                        messages.add(new ChatMessage(entity.text, entity.isUser));
                    }
                    Log.d("MainActivity", "Loaded " + savedMessages.size() + " messages from DB.");
                } else {
                    Log.d("MainActivity", "No messages found in DB.");
                }
                chatAdapter.notifyDataSetChanged();
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu); // Ensure you have res/menu/main_menu.xml
        Log.d(TAG, "onCreateOptionsMenu CALLED and menu inflated for: " + this.toString()); // More specific log
        return true;
    }

// Inside MainActivity.java




    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor when activity is destroyed to prevent resource leaks
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
