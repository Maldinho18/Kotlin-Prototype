# Validation and Team Review Checklist

This document separates what has actually been validated from what remains a design/prototype assumption.

## Build validation

Validated on GitHub Actions for branch `draft/native-prototype`:

- JDK 17 setup: PASS
- Android SDK setup: PASS
- Android 36 platform/build tools installation: PASS
- Gradle 9.3.1 setup: PASS
- `:app:assembleDebug`: PASS
- debug APK generation: PASS
- debug APK artifact upload: PASS

The successful workflow produces the artifact named `sidequests-debug-apk`.

## Source-level design review

Compared against the current Figma Make / React Sidequests source provided to the subgroup.

### Preserved design tokens

- Explorer Indigo: `#5B4BDB`
- Quest Amber: `#FFB347`
- Discovery Teal: `#2AB7A9`
- Light background: `#F6F5FF`
- Light surface: `#FFFFFF`
- Light alternative surface: `#EEEAFF`
- Light text: `#1A1830`
- Dark background: `#0E0D1A`
- Dark surface: `#19182D`
- Dark alternative surface: `#232140`
- Dark text: `#EDE9FF`

The original prototype specifies Outfit for headings and DM Sans for body text. The Android draft currently uses the Android sans-serif fallback rather than bundling those font files. This should be reviewed by the team during the final visual pass.

### Main flows represented

- onboarding interests
- onboarding difficulty
- onboarding time
- onboarding budget/social preference
- Explorer
- Quest detail
- Active quest/progress
- photo-proof camera interaction
- save/exit flow
- rating flow
- group quest
- profile/preferences
- contextual recommendation state
- dark/light mode

## MS7 12-view coverage

The 12 user-visible states are mapped in `MS7_VIEW_MAP.md`.

Status: COVERED IN SOURCE AND NAVIGATION FLOW.

## Architecture review

Implemented draft structure:

`Compose UI -> AppViewModel -> SidequestsRepository -> in-memory data`

Reactive state:

`MutableStateFlow -> StateFlow -> collectAsStateWithLifecycle -> Compose recomposition`

This provides an Observer-style relationship between the observable UI state and the UI subscribers.

## Functional validation boundaries

The following claims are supported:

- The project compiles into a debug APK.
- Native Compose views and navigation/state logic are implemented.
- Camera preview is wired through an Android Activity Result contract.
- Recommendations are computed locally from prototype preferences.
- Contextual notification UX is represented by a simulated timed trigger.

The following claims are NOT yet supported and must not be presented as complete:

- real user authentication;
- real GPS/context sensing;
- a dedicated sensor-backed feature;
- backend persistence;
- external service integration;
- analytics pipeline;
- implemented Business Questions against analytics data.

See `SPRINT2_NEXT.md` for the transition from prototype to Sprint 2 implementation.

## Manual device/emulator checklist before adopting the draft

The subgroup should run the generated APK or Android Studio project and verify:

- no clipped content on the chosen test phone;
- all four onboarding stages are readable and tappable;
- onboarding reaches Explorer;
- time/location/category filters respond;
- a quest can be opened;
- an active quest advances through steps;
- camera preview opens on the target device/emulator where camera support is available;
- save/exit navigation returns correctly;
- rating flow completes;
- group quest view opens;
- profile values can be edited;
- dark/light mode remains legible;
- contextual recommendation banner does not obscure essential controls;
- back/navigation behavior is acceptable for the final product direction.

## Recommendation before moving to the final repository

Treat this branch as a reference implementation. Review it together, then move approved features into teammate-owned feature branches in the final repository so ownership, issues, pull requests, reviews, and commits reflect the actual team workflow.
