# VibeNote

A minimal Android app for creating daily notes using voice input or text, with Firebase cloud sync.

## Features

- **Voice-to-text** — Tap the mic button to dictate notes using Android's built-in speech recognition
- **Note detail view** — Tap a note card to view it full-screen with date, time, and content
- **Cloud sync** — Notes sync to Firebase Firestore in real-time across devices
- **User accounts** — Email/password authentication via Firebase Auth
- **Offline-first** — Notes saved locally to Room first, synced to cloud when online
- **Daily grouping** — Notes organized by date (Today, Yesterday, or full date)
- **Edit & delete** — Edit or delete notes from the list or the detail screen

## Tech Stack

- Kotlin, Jetpack Compose, Material 3
- Room (local database, offline-first cache)
- Firebase Auth (email/password authentication)
- Firebase Cloud Firestore (cloud sync)
- ViewModel + StateFlow
- RecognizerIntent (voice input)

## Setup

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Register an Android app with package name `com.xz.vibenote`
3. Download `google-services.json` to the `app/` directory
4. Enable Email/Password authentication in Firebase Console
5. Deploy Firestore rules: `npx firebase-tools deploy --only firestore:rules`

## Build & Run

```bash
./gradlew installDebug       # Build and install on connected device/emulator
./gradlew connectedDebugAndroidTest  # Run UI tests
```

## Screenshots

<p float="left">
  <img src="screenshots/note_list.png" width="300" alt="Note list screen">
  <img src="screenshots/note_detail.png" width="300" alt="Note detail screen">
</p>
