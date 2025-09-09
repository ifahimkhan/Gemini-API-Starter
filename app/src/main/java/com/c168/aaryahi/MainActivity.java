package com.c168.aaryahi;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.Chat;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.Content;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private EditText promptEditText;
    private ImageButton submitButton;
    private ImageButton micButton;
    private ProgressBar progressBar;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private final List<MessageData> messageList = new ArrayList<>();
    private GenerativeModel generativeModel;
    private AppDatabase db;
    private ExecutorService databaseExecutor;
    private Conversation currentConversation;
    private ActionBarDrawerToggle toggle;
    private ActivityResultLauncher<Intent> speechResultLauncher;
    private Chat chat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("user_settings", MODE_PRIVATE);
        int savedTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        initializeComponents();
        setupNavigationAndToolbar();
        setupChatRecyclerView();
        setupListeners();
        setupSpeechToText();

        if (BuildConfig.API_KEY.isEmpty() || BuildConfig.API_KEY.equals("YOUR_API_KEY_HERE")) {
            Toast.makeText(this, "API Key not set in local.properties", Toast.LENGTH_LONG).show();
            return;
        }
        startNewConversation();
    }

    private void initializeComponents() {
        db = AppDatabase.getInstance(this);
        databaseExecutor = Executors.newSingleThreadExecutor();
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        promptEditText = findViewById(R.id.promptEditText);
        submitButton = findViewById(R.id.sendButton);
        micButton = findViewById(R.id.micButton);
        progressBar = findViewById(R.id.progressBar);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        generativeModel = new GenerativeModel("gemini-1.5-flash", BuildConfig.API_KEY);
    }

    private void setupNavigationAndToolbar() {
        setSupportActionBar(toolbar);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupChatRecyclerView() {
        chatAdapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
        loadConversations();
    }

    private void setupListeners() {
        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            int itemId = item.getItemId();
            if (itemId == R.id.nav_new_chat) {
                startNewConversation();
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else {
                loadConversationById(itemId);
            }
            return true;
        });

        submitButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            if (!prompt.isEmpty()) {
                sendMessage(prompt);
            }
        });

        micButton.setOnClickListener(v -> {
            if (PermissionHelper.hasRecordAudioPermission(this)) {
                startSpeechToText();
            } else {
                PermissionHelper.requestRecordAudioPermission(this);
            }
        });
    }

    private void setupSpeechToText() {
        speechResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> speechResults = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (speechResults != null && !speechResults.isEmpty()) {
                            promptEditText.setText(speechResults.get(0));
                        }
                    }
                });
    }

    private void startSpeechToText() {
        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        try {
            speechResultLauncher.launch(speechIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech-to-text is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechToText();
            } else {
                Toast.makeText(this, "Microphone permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadConversations() {
        databaseExecutor.execute(() -> {
            List<Conversation> conversations = db.conversationDao().getAllConversations();
            runOnUiThread(() -> {
                Menu menu = navigationView.getMenu();
                menu.removeGroup(R.id.group_conversations);
                for (Conversation conv : conversations) {
                    MenuItem addedItem = menu.add(R.id.group_conversations, conv.getId(), Menu.NONE, conv.getTitle());
                    addedItem.setIcon(R.drawable.star);
                    addedItem.setOnMenuItemClickListener(menuItem -> {
                        loadConversationById(conv.getId());
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    });
                }
            });
        });
    }

    private void showRenameDialog(final Conversation conv) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Chat");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(conv.getTitle());
        builder.setView(input);
        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                conv.setTitle(newTitle);
                databaseExecutor.execute(() -> {
                    db.conversationDao().updateConversation(conv);
                    runOnUiThread(this::loadConversations);
                });
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteConfirmationDialog(final Conversation conv) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Chat")
                .setMessage("Are you sure you want to delete this chat?")
                .setPositiveButton("Delete", (dialog, which) -> databaseExecutor.execute(() -> {
                    db.conversationDao().deleteConversationById(conv.getId());
                    runOnUiThread(() -> {
                        if (currentConversation != null && currentConversation.getId() == conv.getId()) {
                            startNewConversation();
                        }
                        loadConversations();
                    });
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startNewConversation() {
        currentConversation = null;
        messageList.clear();
        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }
        Objects.requireNonNull(getSupportActionBar()).setTitle("New Chat");
        chat = generativeModel.startChat(new ArrayList<>());
    }

    private void loadConversationById(int conversationId) {
        databaseExecutor.execute(() -> {
            Conversation conv = db.conversationDao().getConversationById(conversationId);
            if (conv == null) {
                runOnUiThread(this::startNewConversation);
                return;
            }
            db.conversationDao().updateTimestamp(conversationId, System.currentTimeMillis());
            List<MessageEntity> messages = db.messageDao().getMessagesForConversation(conversationId);
            List<Content> history = new ArrayList<>();
            for (MessageEntity msg : messages) {
                String role = (msg.viewType == MessageData.VIEW_TYPE_USER) ? "user" : "model";
                Content.Builder contentBuilder = new Content.Builder();
                contentBuilder.setRole(role);
                contentBuilder.addText(msg.text);
                history.add(contentBuilder.build());
            }

            runOnUiThread(() -> {
                currentConversation = conv;
                chat = generativeModel.startChat(history);
                Objects.requireNonNull(getSupportActionBar()).setTitle(currentConversation.getTitle());
                messageList.clear();
                for (MessageEntity msg : messages) {
                    messageList.add(new MessageData(msg.text, msg.viewType));
                }
                chatAdapter.notifyDataSetChanged();
            });
        });
    }

    private void sendMessage(String prompt) {
        if (chat == null) {
            Toast.makeText(this, "Chat not initialized.", Toast.LENGTH_SHORT).show();
            return;
        }
        addToChat(prompt, MessageData.VIEW_TYPE_USER);
        progressBar.setVisibility(VISIBLE);
        databaseExecutor.execute(() -> {
            boolean isNewConversation = (currentConversation == null);
            if (isNewConversation) {
                String title = prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt;
                Conversation newConv = new Conversation(title);
                long newId = db.conversationDao().insertConversation(newConv);
                newConv.setId((int) newId);
                currentConversation = newConv;
            } else {
                db.conversationDao().updateTimestamp(currentConversation.getId(), System.currentTimeMillis());
            }
            db.messageDao().insertMessage(new MessageEntity(currentConversation.getId(), prompt, MessageData.VIEW_TYPE_USER));
            GeminiHelper.generateText(chat, prompt, (text, error) -> {
                runOnUiThread(() -> {
                    progressBar.setVisibility(GONE);
                    if (error != null) {
                        addToChat("Error: " + error.getMessage(), MessageData.VIEW_TYPE_GEMINI);
                    } else {
                        addToChat(text, MessageData.VIEW_TYPE_GEMINI);
                        databaseExecutor.execute(() -> {
                            if (currentConversation != null) {
                                db.messageDao().insertMessage(new MessageEntity(currentConversation.getId(), text, MessageData.VIEW_TYPE_GEMINI));
                            }
                        });
                    }
                    if (isNewConversation) {
                        runOnUiThread(this::loadConversations);
                    }
                });
                return null;
            });
        });
    }

    private void addToChat(String text, int viewType) {
        messageList.add(new MessageData(text, viewType));
        promptEditText.setText("");
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }
}
