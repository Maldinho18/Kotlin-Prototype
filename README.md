# Sidequests — Kotlin Native Prototype (team draft)

This repository is a **native Android draft** of the Sidequests experience currently represented in the Figma Make / React prototype. Its purpose is to give the Kotlin subgroup a concrete version to review together before deciding what to keep, change, or split among teammates.

## What is implemented in this draft

- Native Android app written in Kotlin with Jetpack Compose.
- The Sidequests visual system translated from the current mockup:
  - Explorer Indigo `#5B4BDB`
  - Quest Amber `#FFB347`
  - Discovery Teal `#2AB7A9`
  - Light and dark surfaces derived from the mockup.
- Four onboarding states.
- Explorer feed with available-time, location and category filters.
- Local smart recommendation scoring and skip/not-for-me behavior.
- Simulated contextual recommendation banner.
- Quest detail view.
- Active quest view with four-step progress.
- Native camera preview launcher for photo-proof steps.
- Save-and-exit flow.
- Rating/feedback flow.
- Group quest view.
- Editable profile/preferences view.
- MVVM-style UI state + repository separation.
- Observable `StateFlow` state used by Compose, giving the project a concrete Observer-style reactive flow for discussion and refinement.

## Current 12-view mapping

The native prototype covers the 12 visual states needed by the current design by treating the four onboarding stages as separate user-visible views:

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

See [`docs/MS7_VIEW_MAP.md`](docs/MS7_VIEW_MAP.md) for the rationale.

## Architecture

The current draft intentionally stays small:

`Compose UI -> AppViewModel -> SidequestsRepository -> in-memory seed data`

The UI observes `StateFlow<SidequestsUiState>`. Actions update the ViewModel state, and Compose automatically renders the new state. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Important scope boundary

This is a **native prototype / Sprint 2 foundation**, not a production app. These pieces are currently simulated or local and should not be represented as completed production integrations:

- contextual recommendation trigger: simulated after entering Explorer;
- recommendation engine: local scoring, not analytics/backend-driven;
- user identity: local demo profile, no authentication provider yet;
- data: in-memory seed data, no persistence/backend yet;
- location: UI filter only, no real GPS query yet;
- external service integration: not connected yet;
- analytics pipeline/business-question implementation: not connected yet.

Those are deliberately listed in [`docs/SPRINT2_NEXT.md`](docs/SPRINT2_NEXT.md) so the team can extend this draft instead of rewriting the UI.

## Open in Android Studio

The project is configured with:

- Android Gradle Plugin 9.1.1
- Kotlin / Compose compiler 2.4.20
- Compose BOM 2026.08.00
- `compileSdk = 37`
- `minSdk = 26`
- Java 17

Open the repository root in a recent Android Studio version and let Gradle sync. This draft does not yet commit a Gradle wrapper binary, so the first local setup may need Android Studio to configure/use the matching Gradle installation.

The repository CI uses a provisioned Gradle installation to run `assembleDebug` and catch integration problems before the group starts building on top of the draft.

## Suggested team review

Before using this as the shared implementation, decide together:

1. Which visual details should match Figma exactly and which can follow Android conventions.
2. Whether the architecture stays as this compact MVVM + Repository structure.
3. Who owns each view and each Sprint 2 functionality.
4. Which real backend/auth/location/analytics stack the main group will use.
5. How the Kotlin repository will connect to the separate backend and analytics repositories.
