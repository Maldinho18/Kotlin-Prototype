# Sprint 2 extension backlog

This file separates **implemented prototype behavior** from **future Sprint 2 work** so the team does not confuse a UI demonstration with an end-to-end feature.

## Implemented now

- Native Compose views and navigation.
- Local preference state.
- Local recommendation scoring.
- Quest progress and rating state.
- Native camera preview flow for photo-proof UX.
- Simulated contextual notification.
- Repository boundary and observable UI state.

## Still to implement for the later Sprint 2 product

### Authentication
Replace the local demo identity with the authentication mechanism selected by the full team.

### External service / backend
Connect at least one non-authentication functionality to the backend and replace the in-memory quest source with the agreed remote/local data strategy.

### Real context awareness
Replace the four-second demo banner with a real context signal chosen for product value (for example location + available time).

### Sensor functionality
Select a phone sensor whose data changes an actual user experience and keep Android sensor APIs behind a small service boundary.

### Business question + analytics pipeline
The recommendation logic in this repository is intentionally local and deterministic. Sprint 2 analytics/business-question work should feed results to the app through a defined interface.

### Persistence / eventual connectivity
Add local persistence and a synchronization policy when the sprint/module requires it rather than keeping all state only in memory.
