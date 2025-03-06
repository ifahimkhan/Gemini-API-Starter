Added a feature to communicate via Microphone device and a dark-light mode theme toggle button allowing user to choose according to their preference

# Changes in MainActivity.java
Message Handling: Replaced TextView with a RecyclerView to display chat messages. Messages are stored in List<Pair<String, String>> for user/model distinction. A MessageAdapter class handles dynamic message rendering.
Voice Input: Added a microphone button (voiceButton) for speech input using RecognizerIntent.ACTION_RECOGNIZE_SPEECH. Requires RECORD_AUDIO permission.
UI Updates: A ToggleButton (themToggle) switches between light and dark themes and updates the background color.
AI Interaction: Replaced generateContent() with startChat() for dynamic message display in RecyclerView, instead of overwriting in a TextView.
Progress Bar: Improved ProgressBar visibility handling to show when responses are being processed.

# New Files
MessageAdapter.java: Custom adapter for RecyclerView, dynamically managing message alignment (left for model, right for user).
MessageViewHolder.java: ViewHolder class for handling message UI elements, containing a TextView to display messages.

#Installation steps

1) Fork the repo
2) Clone the repo - git clone https://github.com/ifahimkhan/Gemini-API-Starter.git
3) Add you api key - GEMINI_API_KEY="" in gradle.properties
4) Sync and rebuild the project and you're good to go
5) Make changes accoording to your preference and keep testing :)