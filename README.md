# Sidequests — Kotlin Native App (Sprint 2 integration)

This repository contains the validated Kotlin/Jetpack Compose implementation used by the Sidequests Kotlin subgroup during Sprint 2. It remains temporarily isolated from the official course repository so the subgroup can finish end-to-end validation before transferring the Git history to the organization repository.

## Current validation status

✅ GitHub Actions successfully runs `:app:assembleDebug` on the draft branch.

✅ A debug APK is generated as the `sidequests-debug-apk` workflow artifact.

✅ The 12 required user-visible MS7 states are represented in the native flow.

⚠️ This validates that the Android project compiles and packages successfully. It does **not** by itself prove pixel-perfect visual parity on every physical device. The UI was reviewed against the current design source, and the final visual pass should still be done on the subgroup's target phones/emulators.

See [`docs/VALIDATION.md`](docs/VALIDATION.md) for the validation checklist.

## What is implemented in this draft

- Native Android application written in Kotlin with Jetpack Compose.
- Sidequests visual system translated from the current mockup:
  - Explorer Indigo `#5B4BDB`
  - Quest Amber `#FFB347`
  - Discovery Teal `#2AB7A9`
  - Light background `#F6F5FF`
  - Dark background `#0E0D1A`
- Four onboarding states.
- Explorer feed with available-time, location, and category filters.
- Local smart recommendation scoring and skip behavior.
- Simulated contextual recommendation banner.
- Quest detail view.
- Active quest view with four-step progress.
- Full-resolution native camera capture with one photo proof per required step.
- Private Supabase Storage upload for photo proofs, including local state and retry.
- Save-and-exit flow.
- Structured abandonment reasons and BQ6 analytics evidence for duration, cost, distance,
  progress, and photo-proof state.
- Rating/feedback flow.
- Group quest view.
- Editable profile/preferences view.
- Light/dark mode.
- MVVM-style UI state + repository separation.
- Observable `StateFlow` state used by Compose, providing a concrete Observer-style reactive flow for the architecture discussion.

## 12-view MS7 mapping

1. Onboarding — interests
2. Onboarding — difficulty
3. Onboarding — typical time
4. Onboarding — budget & social vibe
5. Explorer
6. Quest detail
7. Active quest
8. Exit flow
9. Rating
10. Group quest
11. Profile
12. Contextual recommendation state/banner

See [`docs/MS7_VIEW_MAP.md`](docs/MS7_VIEW_MAP.md) for the detailed mapping and rationale.

## Architecture

The draft intentionally uses a small architecture that can grow into Sprint 2:

`Compose UI -> AppViewModel -> repositories -> Supabase / local fallback`

The UI observes `StateFlow<SidequestsUiState>`. Actions update state through the ViewModel and Compose reacts to the new state. This is the concrete Observer-style flow used in the draft.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Current Sprint 2 scope

The integration branch includes:

- Supabase Auth for sign-up, sign-in and sign-out.
- Remote quest catalogue and quest-step loading from the shared Supabase backend.
- Persistent quest lifecycle/progress.
- BQ5 Smart Picks using the shared `recommend_quests` backend RPC.
- Real Android location permission flow and GPS context.
- Context-aware re-ranking using location, time of day and Open-Meteo weather.
- Location-independent (Anywhere) mode and location-mode analytics events for BQ10.
- Camera-backed photo proof for required quest steps.
- Local photo-proof gating: a required photo step cannot be completed before a usable photo is captured.
- Supabase Storage upload/retry integration for photo proofs.
- Structured abandonment events used by BQ6.
- Observable `StateFlow` UI state consumed by Compose.

Known limitation at this checkpoint:

- photo capture works locally, but remote upload remains pending until the shared Supabase backend provisions the private `quest-proofs` bucket and policies. The app deliberately allows the quest to continue with local proof and exposes a retry action, preserving eventual-connectivity behavior.

The analytics ETL/dashboard lives in the separate `Sidequests-Analytics` repository.

## Toolchain

The validated CI build currently uses:

- Android Gradle Plugin 9.1.1
- Kotlin / Compose compiler 2.4.20
- Compose BOM 2026.06.00
- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 26`
- Java 17
- Gradle 9.3.1 in CI

## Local setup and validation

### 1. Requirements

- Recent Android Studio version.
- Android SDK 36 installed.
- Java/JBR available through Android Studio.
- Android emulator or physical device with API 26+.
- Access to the shared Sidequests Supabase project configuration.

### 2. Clone and open

Open the repository root in Android Studio and let Gradle sync.

For the validated Sprint 2 state, use:

```bash
git fetch origin
git checkout integration/sprint2-kotlin
git pull origin integration/sprint2-kotlin
```

### 3. Local Gradle properties

Do **not** commit project credentials. Copy the values from the shared project into the user-level Gradle properties file:

Windows:

```text
C:\\Users\\<YOUR_USER>\\.gradle\\gradle.properties
```

Required values:

```properties
SUPABASE_URL=...
SUPABASE_PUBLISHABLE_KEY=...
SUPABASE_QUEST_PROOFS_BUCKET=quest-proofs
```

The Android SDK path remains in the local Android `local.properties` file and is ignored by Git.

A secret-free template is available in `gradle.properties.example`.

### 4. Build

```powershell
.\\gradlew.bat clean
.\\gradlew.bat :app:assembleDebug
```

Expected result:

```text
BUILD SUCCESSFUL
```

### 5. Run

In Android Studio:

1. Select the `app` run configuration.
2. Start an Android emulator (the subgroup validated with the Android Studio Medium Phone emulator).
3. Run the app.
4. Sign in with a confirmed Supabase user or create a new account and confirm the email.

### 6. Smoke-test checklist

Validate at least:

- Auth sign-in/sign-out.
- Remote Supabase quest catalogue.
- BQ5 Smart Picks under different time/interests.
- `Not for me` removes and replaces recommendations when possible.
- Nearby requests location permission and uses GPS context.
- Anywhere continues without requiring GPS.
- Live Context displays Open-Meteo weather when available and falls back without crashing.
- Quest tab becomes available after accepting a quest.
- Required photo steps block completion before capture.
- Camera capture creates local photo proof.
- Storage retry reaches the uploaded state once the backend `quest-proofs` bucket/policies are provisioned.
- Accept/start/progress/abandon/complete quest lifecycle.

## Design-pattern evidence

### Sebastián Maldonado — Observer-style reactive flow

`AppViewModel` exposes observable `StateFlow` state (`uiState` and `contextState`). Compose subscribes using `collectAsStateWithLifecycle()`. When the ViewModel changes state, observers receive the update and Compose recomposes the affected UI. This is the concrete Observer-style contribution used for Sebastián's Sprint 2 architecture rationale.

### Julián Ramírez — Facade

`ContextManager` provides one context-facing API over location, weather and time providers, hiding their individual acquisition details from the rest of the app.

### Lex Betancourt — Factory/Abstract Factory

The assigned Factory/Abstract Factory pattern remains to be finalized and documented by its owner in the sensor/photo-proof integration before the final course-repository submission.

## Suggested team review

Before moving anything into the final course repository, the subgroup should decide:

1. Which visual details should match Figma exactly and which should follow Android conventions.
2. Whether to keep the compact MVVM + Repository structure.
3. Who owns each view and each Sprint 2 functionality.
4. Which backend/auth/location/analytics stack the full team will use.
5. How the Kotlin repository will integrate with the separate backend and analytics repositories.
6. Which follow-up items from `docs/SPRINT2_NEXT.md` are approved for implementation.
