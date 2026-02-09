# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**VibeNote** — an Android app (`com.xz.vibenote`). Single-module Gradle project using Jetpack Compose with Material 3. Currently at scaffold/template stage.

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

## Architecture

Single-activity app (`MainActivity`) using `ComponentActivity` with `setContent` for Compose. Theme system in `ui/theme/` supports light/dark/dynamic color schemes via `VibeNoteTheme`.

All source lives under `app/src/main/java/com/xz/vibenote/`.
