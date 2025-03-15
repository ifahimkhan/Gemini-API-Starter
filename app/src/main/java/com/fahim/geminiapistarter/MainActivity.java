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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private TextView responseTextView;
    private ProgressBar progressBar;
    private ImageButton submitPromptButton, micButton;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;

    private static final int REQUEST_MIC_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getPreferences(MODE_PRIVATE).getBoolean("dark_mode", false)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Rest of your existing onCreate code...

        // Theme ToggleButton setup
        ToggleButton themeToggleButton = findViewById(R.id.themeToggleButton);
        themeToggleButton.setChecked(getPreferences(MODE_PRIVATE).getBoolean("dark_mode", false));
        themeToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
            editor.putBoolean("dark_mode", isChecked);
            editor.apply();

            // Restart the activity to apply the theme change
            recreate();
        });


        requestMicrophonePermission();

        promptEditText = findViewById(R.id.promptEditText);
        submitPromptButton = findViewById(R.id.sendButton);
        responseTextView = findViewById(R.id.displayTextView);
        progressBar = findViewById(R.id.progressBar);
        micButton = findViewById(R.id.micButton);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d("Speech", "Ready for speech input...");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("Speech", "Speech started...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                Log.d("Speech", "Speech ended...");
            }

            @Override
            public void onError(int error) {
                Log.e("Speech", "Error: " + error);
                handleError(new Exception("Speech recognition error with code: " + error));
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    promptEditText.setText(matches.get(0));
                    makeGeminiApiCall(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        micButton.setOnClickListener(v -> {
            Log.d("Speech", "Listening...");
            speechRecognizer.startListening(speechRecognizerIntent);
        });

        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                responseTextView.setText(getString(R.string.aistring));
                return;
            }
            progressBar.setVisibility(VISIBLE);
            makeGeminiApiCall(prompt);
        });
    }

    private void requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
        }
    }

    private void makeGeminiApiCall(String input) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyDGJOTF1_cFZIKMVV5GJuP-0r6XyS4LIHM");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String jsonInputString = "{"
                        + "\"contents\": ["
                        + "{"
                        + "\"parts\":["
                        + "{"
                        + "\"text\": \"" + input + "\""
                        + "}"
                        + "]"
                        + "}"
                        + "]"
                        + "}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] inputBytes = jsonInputString.getBytes("utf-8");
                    os.write(inputBytes, 0, inputBytes.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine = null;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        handleApiResponse(response.toString());
                    }
                } else {
                    handleError(new Exception("API request failed with status: " + responseCode));
                }
            } catch (Exception e) {
                Log.e("API", "Error during Gemini API call", e);
                handleError(e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void handleApiResponse(String response) {
        final String parsedResponse = parseJsonResponse(response);
        runOnUiThread(() -> {
            progressBar.setVisibility(GONE);
            responseTextView.setText(parsedResponse);
        });
    }

    private String parseJsonResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray candidates = jsonObject.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONArray parts = firstCandidate.getJSONObject("content").getJSONArray("parts");
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text");
                }
            }
        } catch (JSONException e) {
            Log.e("JSON", "Error parsing JSON", e);
        }
        return "Failed to parse response";
    }

    private void handleError(Exception e) {
        runOnUiThread(() -> {
            progressBar.setVisibility(GONE);
            responseTextView.setText("Error: " + e.getMessage());
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

}
