# DictateAI - Custom Android Keyboard with Voice Transcription

A custom Android Input Method Editor (IME) that provides voice-to-text functionality using Groq's Whisper API.

## Features

- **Single Button Interface**: Press and hold to record, release to transcribe
- **Voice Recording**: Continuous audio recording during button press
- **AI Transcription**: Uses Groq's Whisper API for accurate speech recognition
- **Text Insertion**: Automatically inserts transcribed text at cursor position
- **Backspace Support**: Delete text with the backspace button
- **Visual Feedback**: Clear status indicators for recording, processing, and completion

## Setup Instructions

### 1. Get Groq API Key

1. Sign up at [Groq Console](https://console.groq.com/)
2. Generate an API key
3. Copy your API key

### 2. Configure API Key

**Add to local.properties (Recommended)**
```properties
# Add this line to your local.properties file
groq.api.key=your_actual_api_key_here
```

**Note**: The `local.properties` file is already in `.gitignore` and won't be committed to version control.

### 3. Build and Install

```bash
./gradlew assembleDebug
```

### 4. Enable Keyboard

1. Install the APK on your device
2. Go to **Settings → System → Languages & input → On-screen keyboard → Manage keyboards**
3. Enable **"Dictate AI Keyboard"**
4. In any text field, tap the keyboard icon and select **"Dictate AI Keyboard"**

## Usage

1. **Focus a text field** (the keyboard will appear)
2. **Press and hold** the blue button to start recording
3. **Speak** your text while holding
4. **Release** the button to stop recording and start transcription
5. **Wait** for "Transcribing..." to complete
6. **Text is inserted** at the cursor position
7. **Use the red backspace button** (⌫) to delete text

## Architecture

- **MVVM Pattern**: Uses ViewModel and StateFlow for state management
- **Retrofit**: HTTP client for Groq API communication
- **Coroutines**: Asynchronous operations for API calls
- **MediaRecorder**: Audio recording functionality
- **InputMethodService**: Custom keyboard implementation

## Permissions

- `RECORD_AUDIO`: Required for voice recording
- `INTERNET`: Required for API communication

## Security Notes

- **Never commit your API key** to version control
- Use `local.properties` for local development (already in `.gitignore`)
- For production, use secure key management systems

## Troubleshooting

- **Microphone permission denied**: Launch the main app once to grant permission
- **Keyboard not appearing**: Ensure it's enabled in system settings
- **Transcription fails**: Check your internet connection and API key validity

## License

This project is for educational purposes. Please respect Groq's API terms of service. 