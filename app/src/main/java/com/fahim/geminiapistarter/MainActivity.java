package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private static final int MIC_PERMISSION_CODE = 1;
    private static final String LOG_TAG = "GeminiApp";
    private static final String PREFS_NAME = "ChatAppPrefs";
    private static final String THEME_KEY = "ThemeMode";

    private EditText promptInput;
    private ProgressBar loadingIndicator;
    private RecyclerView chatList;
    private ChatAdapter adapter;
    private ImageButton micBtn;
    private SharedPreferences sharedPreferences;

    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private AppDatabase db;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<Intent> speechLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedTheme = sharedPreferences.getInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        promptInput = findViewById(R.id.promptEditText);
        ImageButton sendBtn = findViewById(R.id.sendButton);
        micBtn = findViewById(R.id.micButton);
        loadingIndicator = findViewById(R.id.progressBar);
        chatList = findViewById(R.id.chatRecyclerView);

        adapter = new ChatAdapter(chatMessages);
        chatList.setLayoutManager(new LinearLayoutManager(this));
        chatList.setAdapter(adapter);

        db = AppDatabase.getDb(this);
        loadHistory();

        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> speech = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (speech != null && !speech.isEmpty()) {
                            promptInput.setText(speech.get(0));
                        }
                    }
                });

        GenerativeModel gemini = new GenerativeModel("gemini-1.5-flash", BuildConfig.API_KEY);

        sendBtn.setOnClickListener(v -> {
            String prompt = promptInput.getText().toString().trim();
            if (!prompt.isEmpty()) {
                callGemini(prompt, gemini);
            }
        });

        micBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_CODE);
            } else {
                startSpeechInput();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem themeToggle = menu.findItem(R.id.action_toggle_theme);
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            themeToggle.setIcon(R.drawable.ic_light_mode);
        } else {
            themeToggle.setIcon(R.drawable.ic_dark_mode);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_theme) {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                sharedPreferences.edit().putInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_NO).apply();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                sharedPreferences.edit().putInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_YES).apply();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now");
        speechLauncher.launch(intent);
    }

    private void loadHistory() {
        dbExecutor.execute(() -> {
            List<ChatMessage> history = db.messageDao().getAll();
            runOnUiThread(() -> {
                chatMessages.addAll(history);
                adapter.notifyDataSetChanged();
                if (!chatMessages.isEmpty()) {
                    chatList.scrollToPosition(chatMessages.size() - 1);
                }
            });
        });
    }

    private void callGemini(String prompt, GenerativeModel model) {
        addMessage(prompt, true);
        promptInput.setText("");
        loadingIndicator.setVisibility(VISIBLE);

        model.generateContent(prompt, new Continuation<GenerateContentResponse>() {
            @NonNull @Override
            public CoroutineContext getContext() { return EmptyCoroutineContext.INSTANCE; }

            @Override
            public void resumeWith(@NonNull Object o) {
                runOnUiThread(() -> {
                    loadingIndicator.setVisibility(GONE);
                    if (o instanceof GenerateContentResponse) {
                        String text = ((GenerateContentResponse) o).getText();
                        addMessage(text != null ? text : "Received an empty response.", false);
                    } else if (o instanceof Throwable) {
                        addMessage("API Error: " + ((Throwable) o).getMessage(), false);
                        Log.e(LOG_TAG, "API call failed", (Throwable) o);
                    }
                });
            }
        });
    }

    private void addMessage(String text, boolean isUser) {
        ChatMessage message = new ChatMessage(text, isUser);
        chatMessages.add(message);
        adapter.notifyItemInserted(chatMessages.size() - 1);
        chatList.scrollToPosition(chatMessages.size() - 1);
        dbExecutor.execute(() -> db.messageDao().insert(message));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechInput();
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}