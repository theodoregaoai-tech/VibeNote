# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**VibeNote** — an Android app (`com.xz.vibenote`). Single-module Gradle project using Jetpack Compose with Material 3. Voice-to-text daily notes app with Firebase backend.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)
./gradlew test --tests "com.xz.vibenote.ExampleUnitTest"  # Run a single test class
```

## Tech Stack

- **Language:** Kotlin (JVM target 11)
- **UI:** Jetpack Compose with Material 3, dynamic color (Android 12+)
- **Build:** Gradle 8.13 with Kotlin DSL, AGP 8.13.2, version catalog (`gradle/libs.versions.toml`)
- **Min SDK:** 24 | **Target/Compile SDK:** 36
- **Database:** Room (offline-first local cache, currently v2)
- **Backend:** Firebase Auth (email/password) + Cloud Firestore (cloud sync)
- **No DI framework** — manual instantiation in ViewModels

## Architecture

Single-activity app (`MainActivity`) with auth-gated navigation. Room is the single source of truth; Firestore syncs bidirectionally via `NoteSyncManager`. Firestore structure: `users/{userId}/notes/{noteId}`.

- `data/` — Note entity, NoteDao, NoteDatabase (Room), NoteRepository
- `auth/` — AuthRepository, AuthViewModel (Firebase Auth)
- `sync/` — NoteSyncManager (Firestore push/pull)
- `ui/` — AuthScreen, NoteScreen, NoteDetailScreen, NoteViewModel
- `ui/theme/` — VibeNoteTheme (light/dark/dynamic color)

All source lives under `app/src/main/java/com/xz/vibenote/`.

## Firebase

- Project: `android-3790e` | Package: `com.xz.vibenote`
- `app/google-services.json` is gitignored — must be downloaded from Firebase Console
- `firebase_init` only creates local config; run `npx firebase-tools deploy --only firestore:rules` to provision cloud database
- Firestore rules: `firestore.rules` (per-user ownership)

## Gotchas

- `Icons.Filled.Logout` is deprecated → use `Icons.AutoMirrored.Filled.Logout`
- Room migrations: bump version in `@Database`, add `Migration` object, call `.addMigrations()` on builder
- When filtering per-user data, use `flatMapLatest` on userId StateFlow — don't expose unfiltered `allNotes`
