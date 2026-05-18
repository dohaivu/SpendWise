# AGENTS.md - SpendWise

## Purpose

Kotlin Multiplatform app for expenses. Targets Android and iOS with shared Compose UI.

## Build & Test Commands

```bash
# Android
./gradlew :androidApp:assembleDebug

# Shared tests
./gradlew :shared:allTests

# iOS framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

## Testing Strategy (Required)
- Many unit tests: fast, isolated, and focused on edge cases and logic branches at function/module level.
- Some integration tests: verify component boundaries and wiring (for example repository/service to DAO/database behavior).
- Few end-to-end tests: cover only the 3-5 most critical user flows.

When changing code:
- Always run relevant tests before finishing.
- At minimum for shared/business logic changes, run `./gradlew :shared:allTests`.
- For Android-impacting changes, also run `./gradlew :androidApp:assembleDebug` and any relevant Android test task.
- Add or update tests whenever behavior changes, bug fixes are made, or uncovered branches are introduced.
- Prefer unit tests first; add integration/E2E tests only when interaction-level behavior is what must be verified.

## Coding Conventions
- Platform actual file naming: `{Name}.{platform}.kt`
- Add new dependencies in `gradle/libs.versions.toml` first, then consume via version catalog.
- Use suspend functions + coroutines for async work.
- Prefer fakes over mocks in tests.
- Keep changes minimal and scoped; avoid unrelated refactors.

## Android Architecture Best Practices
- Follow UDF/MVI-style state: immutable UI state + explicit UI events/actions.
- Keep ViewModels platform-light and business logic in use cases.
- Depend on interfaces in domain; bind implementations in DI modules.
- Model loading/error/success states explicitly; avoid nullable-state ambiguity.
- Use structured concurrency (`viewModelScope`, supervisor boundaries, cancellation-aware code).
- Keep persistence and networking behind repositories; avoid direct DAO/network calls from UI.
- Write tests at use-case/repository boundaries and for ViewModel state transitions.

## Task Completion
After each completed task:
1. Build `:androidApp:assembleDebug` unless the task is docs-only.
2. Commit only if explicitly asked.

Before making edits, always re-read the target file to verify exact string content and avoid edit failures due to mismatches
After any code changes, always run ./gradlew build (or platform-specific build) to catch compilation errors before committing
Prefer simple, direct solutions over comprehensive plans unless complexity is required; ask user if unsure about approach scope