# VibeNote

A minimal Android app for creating daily notes using voice input or text.

## Features

- **Voice-to-text** — Tap the mic button to dictate notes using Android's built-in speech recognition
- **Local persistence** — Notes stored in a Room database, available offline
- **Daily grouping** — Notes organized by date (Today, Yesterday, or full date)
- **Edit & delete** — Tap the pencil icon to edit or the trash icon to remove a note

## Tech Stack

- Kotlin, Jetpack Compose, Material 3
- Room (local database)
- ViewModel + StateFlow
- RecognizerIntent (voice input)

## Build & Run

```bash
./gradlew installDebug       # Build and install on connected device/emulator
./gradlew connectedDebugAndroidTest  # Run UI tests
```

## Screenshot

<img src="https://github.com/user-attachments/assets/placeholder" width="300" alt="VibeNote screenshot">

<!-- Replace the placeholder above with an actual screenshot -->
