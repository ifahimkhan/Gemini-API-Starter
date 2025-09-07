package C156.Bhushan.geminiapistarter;

public class ChatMessage {
    private String message;
    private boolean isUser;
    private boolean isLoading;

    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
        this.isLoading = false;
    }

    public ChatMessage(boolean isLoading) {
        this.message = "";
        this.isUser = false;
        this.isLoading = isLoading;
    }

    public String getMessage() { return message; }
    public boolean isUser() { return isUser; }
    public boolean isLoading() { return isLoading; }
}

