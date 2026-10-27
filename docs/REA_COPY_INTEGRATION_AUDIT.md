# REA-inspired audit: COPY Android ↔ Rust runtime

Date: 2026-10-27
Branch: `copy-foundation`

## Scope and evidence

This is a repository-source audit using GitHub file contents and the Android CI, following REA's evidence-first investigation style. It is **not** a claim that the REA MCP server ran inside this session; that server is not registered in the current tool environment.

Primary inspected files:
- `android/app/src/main/java/com/otiniel/copy/CopyApp.kt`
- `android/app/src/main/java/com/otiniel/copy/ProviderChatClient.kt`
- `android/app/src/main/java/com/otiniel/copy/AssistantEngine.kt`
- `android/app/src/main/java/com/otiniel/copy/ProviderConfig.kt`
- `crates/codegen/xai-grok-shell/Cargo.toml`
- `crates/codegen/xai-grok-shell/src/lib.rs`
- `crates/codegen/xai-grok-shell/src/agent/init.rs`
- `crates/codegen/xai-grok-pager-bin/Cargo.toml`
- `crates/codegen/xai-grok-pager-bin/src/main.rs`
- `crates/codegen/xai-acp-lib/src/lib.rs`

## Confirmed observations

1. The Android UI currently chooses between `AssistantEngine.reply` (deterministic local commands) and `ProviderChatClient.complete` (optional HTTP chat-completions API). No Kotlin source imports a Rust/JNI API.
2. The Rust agent bootstrap is not a simple stateless text function: `xai-grok-shell/src/agent/init.rs` bootstraps config, auth and a model catalog, and `xai-grok-pager-bin/src/main.rs` is a composition root that drives the pager/headless agent.
3. The pager binary depends on the shell, workspace, sandbox, update, telemetry, auth and other crates. The shell crate also uses process, filesystem, networking, terminal and Tokio dependencies. Directly adding this full binary as a mobile library is therefore not a minimal or low-risk first integration.
4. `xai-acp-lib` provides typed ACP channels and message abstractions. This is a possible protocol reference, but it is not itself a ready-to-call Android API and does not prove that the existing runtime can be embedded safely on Android.
5. Android CI has passed for the existing foundation and is configured to run unit tests plus `assembleDebug`. A successful build proves compilation/tests in CI only; it does not prove Rust integration, device behavior, Figma fidelity or production readiness.

## Integration decision

Do not attempt to link the entire desktop CLI into the Android process in one step. First isolate the agent's request/response contract and identify which dependencies are essential for a single turn. Then choose between:

- **Native Rust library (JNI/UniFFI):** appropriate only if a small, stable library boundary can be separated from terminal/process-specific code and the necessary dependency graph builds for Android ABIs.
- **Bundled headless process + ACP/stdio:** possible only if a compatible Rust executable can be built and packaged for supported Android ABIs, process creation is viable, and auth/config/runtime requirements are met. This is not equivalent to merely compiling the existing desktop binary.
- **Remote provider adapter:** currently implemented as a separate optional path, but it is not the Rust runtime and may incur provider charges.

The first implementation milestone should be a **small, testable interface contract**, not a speculative giant build-system change.

## Current limitations and next verification gates

- No REA MCP server execution is claimed.
- No Kotlin–Rust bridge exists yet.
- No Android Rust target build has been demonstrated.
- The current app has no bundled local model.
- A real device test and a full user-approved design comparison remain pending.

Gate 1: keep Android tests green.
Gate 2: add a platform-neutral Rust request/response core or adapter boundary with unit tests.
Gate 3: compile a minimal Rust library for Android arm64 and run a smoke test.
Gate 4: connect one Android message end-to-end with timeout, cancellation and error handling.
Gate 5: test on the POCO X5 Pro and at least one different compatible Android configuration before claiming broad compatibility.
