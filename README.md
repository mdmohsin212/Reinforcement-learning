# Ollama Cloud Chat Android App

A simple native Android app that lets you paste an Ollama API key, load available Ollama Cloud models, choose a model or type a manual model name, and chat from your phone.

## API used

- Base URL: `https://ollama.com/api`
- List models: `GET /api/tags`
- Chat: `POST /api/chat`
- Auth header: `Authorization: Bearer YOUR_OLLAMA_API_KEY`

## Build APK without Android Studio: GitHub Actions

This repo includes `.github/workflows/build-apk.yml`. You can build the APK online using GitHub Actions.

1. Create a new GitHub repository.
2. Upload all files from this project to the repository.
3. Go to the repository's **Actions** tab.
4. Choose **Build Android APK**.
5. Click **Run workflow**.
6. After it finishes, open the latest workflow run.
7. Download the artifact named **OllamaCloudChat-debug-apk**.
8. Inside it, install `app-debug.apk` on your phone.

Android may show an “unknown app” warning because this is a debug APK, not Play Store signed. For personal testing, that is normal.

## Build APK with Android Studio

1. Open this `OllamaCloudChat` folder.
2. Let Gradle sync.
3. Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Install the generated APK on your Android phone.

## Features

- Save Ollama API key locally on phone
- Default Ollama Cloud base URL: `https://ollama.com/api`
- Load model list from `GET /api/tags`
- Select model from dropdown
- Manual model name input if dropdown/model loading fails
- Manual model name takes priority over dropdown selection
- Send chat requests to `POST /api/chat`
- Basic in-session conversation memory
- GitHub Actions APK build, no Android Studio required

## How to use

1. Open the app.
2. Paste your Ollama API key.
3. Keep base URL as `https://ollama.com/api`.
4. Tap **Load Models**.
5. Select a model from the dropdown.
6. If model loading fails, type the model name manually, for example `gpt-oss:20b-cloud`.
7. Start chatting.

Note: If the manual model field is not empty, the app uses that model instead of the dropdown model.

## Security note

This MVP saves the API key in normal Android SharedPreferences. For production, replace this with Android Keystore or EncryptedSharedPreferences.
