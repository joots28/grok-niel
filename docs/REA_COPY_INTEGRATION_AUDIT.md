# REA-inspired audit: COPY Android ↔ Rust runtime

Date: 2026-10-27
Branch: `copy-foundation`

## Integration update

The first JNI bridge is implemented under `android/rust-core/`, isolated from the large desktop Cargo workspace. The shared library exports a stable local reply function consumed by `CopyRustBridge.kt`. Rust produces the local response text for implemented deterministic commands, using device-local time/date strings from Android. The Android build workflow tests the Rust crate, builds native libraries for Android `arm64-v8a` and `x86_64`, and packages them into the debug APK. The app UI reports whether the native response engine is available; when absent, it explicitly reports the Kotlin fallback.

## Confirmed source boundaries

- Android turns currently choose between `AssistantEngine.reply` (local deterministic commands) and `ProviderChatClient.complete` (optional OpenAI-compatible HTTP API).
- The desktop agent is orchestrated through `xai-grok-shell` and `xai-grok-pager-bin`, with authentication, configuration, sessions, workspace, sandbox and terminal dependencies. It is not a drop-in mobile library.
- `xai-acp-lib` provides protocol abstractions, not a prebuilt Android runtime.
- The new Rust core intentionally has no dependency on those desktop crates. This reduces Android build risk while proving JNI packaging and the cross-language call path.

## Validation gates

1. Rust unit tests pass.
2. Android target shared libraries compile and are packaged.
3. Android unit tests and debug APK build pass in CI.
4. A real device test confirms the UI reports Rust active and local command responses are correct.
5. The full desktop agent is not considered integrated until its dependencies and lifecycle are made mobile-compatible and an end-to-end request is tested.

This workflow follows evidence-first inspection practices. It does not assert that the REA MCP server ran in this session.
