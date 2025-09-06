package com.fahim.geminiapistarter;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatViewModel extends AndroidViewModel {
    private final ChatDao chatDao;
    private final LiveData<List<ChatMessage>> allMessages;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ChatViewModel(@NonNull Application application) {
        super(application);
        ChatDatabase db = ChatDatabase.getInstance(application);
        chatDao = db.chatDao();
        allMessages = chatDao.getAllMessages();
    }

    public LiveData<List<ChatMessage>> getAllMessages() {
        return allMessages;
    }

    public void insert(final ChatMessage message) {
        executor.execute(() -> chatDao.insertMessage(message));
    }

    public void update(final ChatMessage message) {
        executor.execute(() -> chatDao.updateMessage(message));
    }

    public void clearAll() {
        executor.execute(() -> chatDao.clearAll());
    }
}

