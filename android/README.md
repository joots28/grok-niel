# Copy Android — foundation 0.1.0

Native Android starter using Kotlin and Jetpack Compose. This directory is intentionally isolated from the root Rust workspace.

## Current scope

- Dark interface foundation matching the Copy prototype's information architecture.
- Navigation between Home, Commands, Control Center, Memory and History.
- Local demonstration interactions for searching/filtering, adding/deleting demo memories, and composing a demo conversation.
- Explicit labels for demo-only controls and AI behavior.

## Not implemented yet

- Figma source extraction and pixel-level comparison.
- Connection to an AI provider or the Rust runtime.
- Persistent database or cross-session memory.
- Real microphone, Android device controls, background services, or system integrations.
- Automated Android build, emulator tests, or APK validation.

The UI must not describe a demonstration state as a completed device operation. Integrations must remain behind explicit permission and risk checks when implemented.

## Build locally

Requirements: Android Studio with JDK 17 and Android SDK Platform 35.

Open the `android/` directory as the Gradle project in Android Studio, allow Gradle sync, then run the `app` configuration on an emulator or connected Android device.

No Gradle wrapper is committed in this first pass, so use Android Studio's Gradle import or install a compatible Gradle distribution locally. Build configuration and dependencies have not yet been verified by an Android build in this environment.
