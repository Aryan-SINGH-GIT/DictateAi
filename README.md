# DictateAI — Voice Dictation Keyboard (Android IME)

A custom Android Input Method Editor (IME) that records audio while you press and hold a single mic button, sends the complete recording to Groq Whisper for transcription after release, and inserts the resulting text at the cursor.

## Feature Highlights

- **Custom IME (Keyboard)**: Single, centered mic button with a clean UI
- **Press & Hold Recording**: Recording starts on press-down, ends on release
- **Post-Recording Transcription**: No streaming; the entire file is uploaded after release
- **Text Insertion**: Inserts transcription at the current cursor position
- **Polished UX**:
  - Continuous, non-intrusive pulse animation around the mic while recording
  - Progress spinner during transcription; brief status messages for success/errors
  - Settings gear opens Android keyboard settings
  - Backspace button: tap deletes 1 character; press-and-hold auto-repeats with acceleration
- **Modern Stack**: Kotlin, MVVM, Coroutines, Retrofit/OkHttp, Compose test screen

## How It Works (Core Workflow)

1. User presses and holds the mic button → audio recording starts immediately
2. User keeps holding → continuous recording (no live transcription)
3. User releases the button → recording stops, audio is saved
4. App uploads the complete audio file to Groq Whisper (`whisper-large-v3`)
5. On success, the text is inserted at the active cursor
6. Keyboard returns to idle, ready for the next dictation

## Project Structure

- `app/src/main/java/com/example/dictateai/ime/DictateImeService.kt`
  - IME entry point: recording logic, animations, state collection, text insertion
  - Recording: `AudioRecorder` (MediaRecorder → AAC/m4a) in `app/src/main/java/com/example/dictateai/audio/AudioRecorder.kt`
  - Network: `RetrofitClient` + `GroqApi` + `TranscriptionRepository`
  - State: `TranscriptionViewModel` with a simple `UIState`
- `app/src/main/res/layout/ime_keyboard.xml`
  - UI with title, settings button, centered mic, backspace button, progress bar, status text
  - Pulse ring view layered outside the mic button
- `app/src/main/res/anim/*.xml`
  - `pulse_animation.xml` is a looping scale/alpha pulse (non-resizing keyboard)
- `app/src/main/java/com/example/dictateai/MainActivity.kt`
  - Simple in-app tester: text field + quick actions (show keyboard, picker, settings)

## Requirements

- Android Studio Jellyfish+ (AGP 8.10.x)
- Android SDK 36 (compileSdk 36, targetSdk 36, minSdk 24)
- A valid Groq API key

## Configure Your API Key (local.properties)

Do NOT commit secrets. This project reads your API key from `local.properties` (which is already gitignored).

1. Open the file at the project root: `local.properties`
2. Add your key:

```properties
# Groq API key for Whisper transcription
groq.api.key=YOUR_GROQ_API_KEY
```

The Gradle script exposes it to code via `BuildConfig.GROQ_API_KEY`.

## Open in Android Studio (recommended)

1. Open Android Studio → File → Open… → select the project root folder (`DictateAI/`)
2. Let Gradle Sync finish (first sync may take a few minutes)
3. Tools → SDK Manager → ensure Android API 36 (Android 15) SDK is installed
4. If prompted, install missing build tools or accept licenses
5. Build → Make Project to verify

Run the sample app:
- Select the `app` run configuration → click Run ▶
- The tester screen opens; grant Microphone permission when prompted

## Build from command line (optional)

```bash
./gradlew assembleDebug
```

## Enable and Use the Keyboard

1. Open the app once (so Android can request Microphone permission)
2. Settings → System → Languages & input → On-screen keyboard → Manage keyboards → enable “Dictate AI Keyboard”
3. In any text field, tap the input method picker and select “Dictate AI Keyboard”
4. Press and hold the mic to record, release to transcribe; text inserts at the cursor

## Networking

- Base URL: `https://api.groq.com/`
- Endpoint: `POST openai/v1/audio/transcriptions`
- Headers: `Authorization: Bearer <BuildConfig.GROQ_API_KEY>`
- Multipart Params:
  - `file`: recorded m4a (AAC) (single channel, 16 kHz, ~64 kbps)
  - `model`: `whisper-large-v3`
  - `response_format`: `json`

## UX Details

- Recording uses a looping pulse animation rendered outside the mic (no size jumps)
- No “Recording… Release to stop” text during hold
- During processing, a small progress indicator is shown; keyboard size remains fixed
- Backspace: tap deletes 1 char; long-press auto-repeats with acceleration (starts ~300 ms after hold, ramps from ~120 ms to ~40 ms per delete)

## Permissions

- `RECORD_AUDIO` is required for recording
- `INTERNET` is required to call Groq

Grant Microphone permission by launching the app once; IME services cannot reliably show permission prompts by themselves.

## Troubleshooting

- Gradle sync fails: check SDK 36 installed; click “Install missing components” in Android Studio
- API key not picked up: confirm `local.properties` contains `groq.api.key=...`, then File → Sync Project with Gradle Files
- Keyboard not visible: enable it in system settings and switch via the input method picker
- Transcription fails: verify network, API key validity, and that the Whisper model name is correct

## License

Educational/demo usage. Ensure compliance with Groq API Terms of Service. 
