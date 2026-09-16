# MS7 native-view map

The Figma Make save contains eight primary screen components, but the onboarding component contains four distinct full-screen states. The native draft therefore exposes twelve user-visible states without inventing an unrelated flow.

| # | Native state | Source in current design | Purpose |
|---|---|---|---|
| 1 | Onboarding — Interests | `OnboardingScreen` step 1 | Select interests |
| 2 | Onboarding — Difficulty | `OnboardingScreen` step 2 | Select challenge level |
| 3 | Onboarding — Time | `OnboardingScreen` step 3 | Set typical free-time slot |
| 4 | Onboarding — Budget & vibe | `OnboardingScreen` step 4 | Set spend + social preference |
| 5 | Explorer | `ExplorerScreen` | Discover and filter quests |
| 6 | Quest detail | `QuestDetailScreen` | Review a recommendation before accepting |
| 7 | Active quest | `ActiveQuestScreen` | Execute the quest step by step |
| 8 | Exit flow | `ExitFlowScreen` | Pause/save/abandon without losing context |
| 9 | Rating | `RatingScreen` | Capture feedback for future recommendations |
| 10 | Group quest | `GroupQuestScreen` | Coordinate a shared quest |
| 11 | Profile | `ProfileScreen` | Preferences, progress, badges |
| 12 | Contextual recommendation | `NotificationBanner` over Explorer | Surface a time/location-style smart suggestion |

## UI system carried over from the mockup

- Primary: Explorer Indigo `#5B4BDB`
- Highlight: Quest Amber `#FFB347`
- Discovery / success: `#2AB7A9`
- Rounded cards/chips with strong hierarchy and generous spacing.
- Light/dark surfaces preserved.
- The Figma web prototype specifies Outfit for headings and DM Sans for body. The Android draft currently uses the platform sans-serif fallback so no font binaries are committed. The team can add the licensed font resources itself if exact typography is required.
