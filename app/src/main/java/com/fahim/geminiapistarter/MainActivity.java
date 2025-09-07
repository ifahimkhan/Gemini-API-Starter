package com.fahim.geminiapistarter;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fahim.geminiapistarter.ui.ChatAdapter;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// Room
import com.fahim.geminiapistarter.data.ChatDao;
import com.fahim.geminiapistarter.data.ChatDatabase;
import com.fahim.geminiapistarter.data.ChatMessage;

public class MainActivity extends AppCompatActivity {

    // UI
    private RecyclerView rvChat;
    private ChatAdapter chatAdapter;
    private TextInputEditText etInput;
    private MaterialButton btnSend;
    private ImageButton btnMic;

    // Gemini
    private GenerativeModel generativeModel;

    // Speech-to-text
    private ActivityResultLauncher<Intent> speechLauncher;
    private static final int REQ_RECORD_AUDIO = 1001;

    // Prefs
    private static final String PREFS = "prefs";
    private static final String KEY_MODEL = "model";
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String KEY_THEME = "theme";   // "system" | "light" | "dark"
    private SharedPreferences prefs;
    private String currentModelName;

    // Room
    private ChatDao chatDao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Preferences + theme must be applied BEFORE inflating views
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        applySavedTheme();

        setContentView(R.layout.activity_main);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);


        // Views
        rvChat = findViewById(R.id.rvChat);
        etInput = findViewById(R.id.etInput);
        btnSend = findViewById(R.id.btnSend);
        btnMic  = findViewById(R.id.btnMic);

        if (!isSpeechAvailable()) {
            btnMic.setEnabled(false);      // or: btnMic.setVisibility(View.GONE);
            btnMic.setAlpha(0.4f);         // visual cue it's disabled
        }

        // Recycler setup
        chatAdapter = new ChatAdapter();
        rvChat.setAdapter(chatAdapter);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvChat.setLayoutManager(lm);
        rvChat.setHasFixedSize(true);

        // ===== Room DB =====
        ChatDatabase db = ChatDatabase.getInstance(getApplicationContext());
        chatDao = db.chatDao();

        // Load saved history off main thread
        dbExecutor.execute(() -> {
            List<ChatMessage> saved = chatDao.getAll();
            runOnUiThread(() -> {
                for (ChatMessage cm : saved) {
                    chatAdapter.add(new Message(cm.text, cm.isUser, cm.timestamp));
                }
                if (chatAdapter.getItemCount() > 0) {
                    rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                }
            });
        });

        // ===== Model preference & Gemini init =====
        currentModelName = prefs.getString(KEY_MODEL, DEFAULT_MODEL);
        generativeModel = new GenerativeModel(currentModelName, BuildConfig.API_KEY);

        // ===== Send click =====
        btnSend.setOnClickListener(v -> {
            String prompt = etInput.getText() == null ? "" : etInput.getText().toString().trim();
            if (prompt.isEmpty()) return;

            long now = System.currentTimeMillis();

            // Show + persist user message
            chatAdapter.add(new Message(prompt, true, now));
            rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
            etInput.setText("");
            btnSend.setEnabled(false);
            dbExecutor.execute(() -> chatDao.insert(new ChatMessage(prompt, true, now)));

            // Call Gemini (Java interop with Kotlin Continuation)
            generativeModel.generateContent(prompt, new kotlin.coroutines.Continuation<com.google.ai.client.generativeai.type.GenerateContentResponse>() {
                @org.jetbrains.annotations.NotNull
                @Override
                public kotlin.coroutines.CoroutineContext getContext() {
                    return kotlin.coroutines.EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@org.jetbrains.annotations.NotNull Object result) {
                    try {
                        com.google.ai.client.generativeai.type.GenerateContentResponse resp =
                                (com.google.ai.client.generativeai.type.GenerateContentResponse) result;
                        final String text = (resp.getText() == null) ? "(no response)" : resp.getText();
                        final long t = System.currentTimeMillis();

                        runOnUiThread(() -> {
                            chatAdapter.add(new Message(text, false, t));
                            rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                            btnSend.setEnabled(true);
                        });

                        // persist bot reply
                        dbExecutor.execute(() -> chatDao.insert(new ChatMessage(text, false, t)));

                    } catch (Throwable t) {
                        runOnUiThread(() -> {
                            chatAdapter.add(new Message("Error: " + t.getMessage(), false, System.currentTimeMillis()));
                            rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                            btnSend.setEnabled(true);
                        });
                    }
                }
            });
        });

        // ===== Speech result launcher =====
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> results =
                                result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (results != null && !results.isEmpty()) {
                            etInput.setText(results.get(0));
                            etInput.setSelection(etInput.getText().length());
                        }
                    }
                }
        );

        // ===== Mic click: permission → speech =====
        btnMic.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQ_RECORD_AUDIO
                );
            } else {
                startSpeechToText();
            }
        });
    }

    private boolean isSpeechAvailable() {
        return getPackageManager().queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
        ).size() > 0;
    }


    // ===== Menu =====

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu); // menu_main must have: action_settings, action_theme, action_clear_history
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showModelPicker();
            return true;
        } else if (id == R.id.action_theme) {
            showThemePicker();
            return true;
        } else if (id == R.id.action_clear_history) {
            confirmClearHistory();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showModelPicker() {
        final String[] models = new String[] { "gemini-1.5-flash", "gemini-2.0-flash" };
        int checked = currentModelName.equals(models[1]) ? 1 : 0;

        new AlertDialog.Builder(this)
                .setTitle("Choose model")
                .setSingleChoiceItems(models, checked, (dialog, which) -> {
                    currentModelName = models[which];
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    prefs.edit().putString(KEY_MODEL, currentModelName).apply();
                    generativeModel = new GenerativeModel(currentModelName, BuildConfig.API_KEY);
                    long ts = System.currentTimeMillis();
                    chatAdapter.add(new Message("Model set to " + currentModelName, false, ts));
                    rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                    dbExecutor.execute(() ->
                            chatDao.insert(new ChatMessage("Model set to " + currentModelName, false, ts)));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showThemePicker() {
        final String[] options = new String[]{"Follow system", "Light", "Dark"};
        String pref = prefs.getString(KEY_THEME, "system");
        int checked = "light".equals(pref) ? 1 : "dark".equals(pref) ? 2 : 0;

        new AlertDialog.Builder(this)
                .setTitle("Theme")
                .setSingleChoiceItems(options, checked, (d, which) -> {})
                .setPositiveButton("Apply", (d, w) -> {
                    int sel = ((AlertDialog) d).getListView().getCheckedItemPosition();
                    String value = sel == 1 ? "light" : sel == 2 ? "dark" : "system";
                    prefs.edit().putString(KEY_THEME, value).apply();
                    applySavedTheme();
                    recreate(); // re-inflate with new theme
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Clear chat history?")
                .setMessage("This will remove all saved messages.")
                .setPositiveButton("Clear", (dialog, which) -> clearHistory())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearHistory() {
        dbExecutor.execute(() -> {
            chatDao.clear();
            runOnUiThread(() -> {
                // requires ChatAdapter#clear()
                chatAdapter.clear();
            });
        });
    }

    private void applySavedTheme() {
        String pref = prefs.getString(KEY_THEME, "system");
        int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if ("light".equals(pref)) mode = AppCompatDelegate.MODE_NIGHT_NO;
        else if ("dark".equals(pref)) mode = AppCompatDelegate.MODE_NIGHT_YES;
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    // ===== Speech helpers =====

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now");
        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            long ts = System.currentTimeMillis();
            chatAdapter.add(new Message("Voice input unavailable on this device.", false, ts));
            rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
            dbExecutor.execute(() ->
                    chatDao.insert(new ChatMessage("Voice input unavailable on this device.", false, ts)));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSpeechToText();
        } else if (requestCode == REQ_RECORD_AUDIO) {
            long ts = System.currentTimeMillis();
            chatAdapter.add(new Message("Microphone permission denied.", false, ts));
            rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
            dbExecutor.execute(() ->
                    chatDao.insert(new ChatMessage("Microphone permission denied.", false, ts)));
        }
    }
}
