package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Removed: import com.fahim.geminiapistarter.ui.color.ColorSelectedListener;
// Removed: import com.fahim.geminiapistarter.ui.color.MinColorPickerDialogFragment;
// Import for UiTheme might still be used if other parts of your app use it, but not for this reverted color picker.
// import com.fahim.geminiapistarter.ui.theme.UiTheme;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity { // Removed: implements ColorSelectedListener

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private ImageButton micButton;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;

    private ImageButton themeToggleButton;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "ThemePrefs";
    private static final String KEY_THEME = "themeMode";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;

    private ImageButton colorPaletteButton;
    private MaterialToolbar topAppBar;
    private static final String KEY_CUSTOM_COLOR = "customColor"; // For the simple app bar color
    private int currentCustomColor; // To hold the loaded custom color

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        applyTheme(); // Apply light/dark theme first

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        topAppBar = findViewById(R.id.topAppBar);
        loadAndApplyCustomColorToAppBar(); // Apply custom app bar color

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        promptEditText = findViewById(R.id.promptEditText);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);
        micButton = findViewById(R.id.micButton);
        themeToggleButton = findViewById(R.id.themeToggleButton);
        colorPaletteButton = findViewById(R.id.colorPaletteButton);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);

        GenerativeModel generativeModel = new GenerativeModel("gemini-1.5-flash",
                BuildConfig.API_KEY);

        setupSpeechRecognizer();
        setupThemeToggleButton();

        colorPaletteButton.setOnClickListener(v -> showColorPickerDialog()); // Reverted to old method

        micButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                startVoiceInput();
            }
        });

        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }
            ChatMessage userMessage = new ChatMessage(prompt, ChatMessage.MessageType.USER);
            chatAdapter.addMessage(userMessage);
            recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            promptEditText.setText("");
            progressBar.setVisibility(VISIBLE);
            generativeModel.generateContent(prompt, new Continuation<>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    if (o instanceof GenerateContentResponse) {
                        GenerateContentResponse response = (GenerateContentResponse) o;
                        String responseString = response.getText();
                        if (responseString != null) {
                            Log.d("Response", responseString);
                            runOnUiThread(() -> {
                                progressBar.setVisibility(GONE);
                                ChatMessage apiMessage = new ChatMessage(responseString, ChatMessage.MessageType.API);
                                chatAdapter.addMessage(apiMessage);
                                recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                            });
                        } else {
                            Log.e("Response", "Response text is null");
                            runOnUiThread(() -> {
                                progressBar.setVisibility(GONE);
                                ChatMessage errorMessage = new ChatMessage("Error: Received empty response.", ChatMessage.MessageType.API);
                                chatAdapter.addMessage(errorMessage);
                                recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                            });
                        }
                    } else if (o instanceof Throwable) {
                        Throwable throwable = (Throwable) o;
                        Log.e("Response", "Error generating content", throwable);
                        runOnUiThread(() -> {
                            progressBar.setVisibility(GONE);
                            ChatMessage errorMessage = new ChatMessage("Error: " + throwable.getMessage(), ChatMessage.MessageType.API);
                            chatAdapter.addMessage(errorMessage);
                            recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                        });
                    }
                }
            });
        });
    }

    // Removed onColorSelected method

    private void showColorPickerDialog() { // Restored simple color picker
        final String[] colorNames = {"Default", "Red", "Blue", "Green", "Purple", "Orange"};
        final int[] colorValues = {
                0, // Special value for default
                Color.parseColor("#F44336"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#FF9800")
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose App Bar Color");
        builder.setItems(colorNames, (dialog, which) -> {
            currentCustomColor = colorValues[which];
            saveCustomColor(currentCustomColor);
            loadAndApplyCustomColorToAppBar(); // Apply immediately
        });
        builder.show();
    }

    private void saveCustomColor(int color) { // Restored
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (color == 0) { // 0 represents default
            editor.remove(KEY_CUSTOM_COLOR);
        } else {
            editor.putInt(KEY_CUSTOM_COLOR, color);
        }
        editor.apply();
    }

    private void loadAndApplyCustomColorToAppBar() { // Restored
        currentCustomColor = sharedPreferences.getInt(KEY_CUSTOM_COLOR, 0); // 0 for default
        if (topAppBar != null) {
            if (currentCustomColor != 0) {
                topAppBar.setBackgroundColor(currentCustomColor);
            } else {
                // Apply default theme color for app bar
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
                topAppBar.setBackgroundColor(typedValue.data);
            }
        }
    }

    private void setupThemeToggleButton() {
        updateThemeIcon();
        themeToggleButton.setOnClickListener(v -> {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                saveThemePreference(THEME_LIGHT);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                saveThemePreference(THEME_DARK);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            // No longer applying full UiTheme.applyAccent here
            // App bar color will be reapplied by onConfigurationChanged -> loadAndApplyCustomColorToAppBar
        });
    }

    private void applyTheme() {
        int savedTheme = sharedPreferences.getInt(KEY_THEME, THEME_SYSTEM);
        if (savedTheme == THEME_LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (savedTheme == THEME_DARK) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void saveThemePreference(int themeMode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_THEME, themeMode);
        editor.apply();
    }

    private void updateThemeIcon() {
        if (themeToggleButton == null) return;
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            themeToggleButton.setImageResource(R.drawable.ic_sun_24dp);
            themeToggleButton.setContentDescription(getString(R.string.switch_to_light_mode));
        } else {
            themeToggleButton.setImageResource(R.drawable.ic_moon_24dp);
            themeToggleButton.setContentDescription(getString(R.string.switch_to_dark_mode));
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateThemeIcon();
        loadAndApplyCustomColorToAppBar(); // Re-apply custom app bar color if configuration changes
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                promptEditText.setHint("Listening...");
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                promptEditText.setHint(getString(R.string.enter_your_prompt_here));
            }

            @Override
            public void onError(int error) {
                promptEditText.setHint(getString(R.string.enter_your_prompt_here));
                String errorMessage = getErrorText(error);
                Log.e("SpeechRecognizer", "Error: " + errorMessage);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    promptEditText.setText(matches.get(0));
                    promptEditText.setSelection(promptEditText.getText().length());
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    promptEditText.setText(matches.get(0));
                    promptEditText.setSelection(promptEditText.getText().length());
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startVoiceInput() {
        if (speechRecognizer != null) {
            promptEditText.setText("");
            promptEditText.setHint("Listening...");
            speechRecognizer.startListening(speechRecognizerIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput();
            } else {
                Toast.makeText(this, "Audio permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
