# Sidequests — Kotlin Native Prototype (team draft)

This repository contains a **native Android draft** of the Sidequests experience represented in the current Figma Make / React prototype. It is intentionally isolated from the final course repository so the Kotlin subgroup can review, test, change, and divide the work before deciding what should be carried over.

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

## Important scope boundary

This is a **native prototype / Sprint 2 foundation**, not a claim that all Sprint 2 integrations are finished. The following remain outside this branch:

- contextual trigger: simulated after entering Explorer;
- location: UI filter only, no real GPS query;
- external service: not connected;
- analytics ETL/dashboard: not contained in this mobile repository;
- offline upload queue across process restarts: not implemented yet.

This branch does include Supabase Auth, the remote quest catalogue, lifecycle persistence,
analytics event writes, BQ5 recommendation RPC consumption, camera-backed photo proof,
and the BQ6 event contract. See [`docs/SENSOR_QUEST_LIFECYCLE.md`](docs/SENSOR_QUEST_LIFECYCLE.md)
for the Storage policy and validation boundary.

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

## Open in Android Studio

1. Clone or download the `draft/native-prototype` branch.
2. Open the repository root in a recent Android Studio version.
3. Let Gradle sync.
4. Run the `app` configuration on an Android emulator or device with API 26+.
5. Walk through onboarding and the remaining screens using the controls in the app.

The repository intentionally does not commit a Gradle wrapper JAR; CI provisions Gradle 9.3.1 explicitly. Android Studio can use the configured Gradle environment when opening the project.

## Suggested team review

Before moving anything into the final course repository, the subgroup should decide:

1. Which visual details should match Figma exactly and which should follow Android conventions.
2. Whether to keep the compact MVVM + Repository structure.
3. Who owns each view and each Sprint 2 functionality.
4. Which backend/auth/location/analytics stack the full team will use.
5. How the Kotlin repository will integrate with the separate backend and analytics repositories.
6. Which follow-up items from `docs/SPRINT2_NEXT.md` are approved for implementation.
