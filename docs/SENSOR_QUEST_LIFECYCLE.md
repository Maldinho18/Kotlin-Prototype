# Sensor and quest-lifecycle integration

This branch implements the Kotlin-owned Sprint block on top of
`feature/quest-progress-persistence`.

## Photo-proof flow

1. A photo step launches the device camera with `ActivityResultContracts.TakePicture`.
2. `CameraPhotoProofService` asks `JpegPhotoProofFileFactory` for a JPEG target inside
   the app's private files directory and shares only a temporary `FileProvider` URI
   with the camera.
3. The active quest keeps evidence per step, so one photo cannot satisfy multiple
   photo-required steps.
4. The local photo lets the user continue if the network is unavailable.
5. When Supabase is configured, `PhotoProofRepository` uploads the JPEG to the private
   Storage bucket, registers its attempt and step in `quest_photo_proofs`, and exposes
   retry state if either operation fails. A failed registration attempts to remove
   the uploaded object.
6. Analytics records `photo_proof_captured`, `photo_proof_uploaded`, or
   `photo_proof_upload_failed`. Events contain technical evidence only; they do not put
   image bytes or a public URL into `analytics_events`.

The default bucket is `quest-proofs`. It can be changed with the Gradle property or
environment variable `SUPABASE_QUEST_PROOFS_BUCKET`.

### Lex's individual pattern: Factory Method

`PhotoProofFileFactory` is the abstract creator. Its `create(questId, stepIndex)`
prepares the private directory and validates the step, then invokes the protected
factory method `createProof`. `JpegPhotoProofFileFactory` is the concrete creator;
it produces a `JpegPhotoProofFile` through the `PhotoProofFile` product interface.
`CameraPhotoProofService` consumes that interface and handles only the Android URI.

This is part of the actual capture path in `ActiveQuestScreen`.
Every capture gets a new file, so retaking a photo does not overwrite previous
evidence. File creation can be tested without Android or opening the camera.
It is Factory Method rather than Abstract Factory because this flow creates one
kind of product, an evidence file; it does not need multiple product families.
This implementation is separate from Julián's `AppViewModelFactory`.

### Required backend migration

Deploy `supabase/migrations/009_quest_photo_proofs.sql` from the official
`Sidequests-Backend` repository before validating remote photo upload. It creates
the private bucket, proof records, and ownership policies. The app writes objects as:

```text
<user-id>/<attempt-id>/<quest-id>/step-<number>-<uuid>.jpg
```

The path is private; analytics keeps the path only as technical evidence, not as a
public URL. The proof table is the authoritative per-step record. The old
`user_quests.photo_proof_path` field is not used for new uploads.

## BQ6 event contract

Abandonment requires one stable reason code. Display labels may change without breaking
analytics:

- `ran_out_of_time`
- `too_far`
- `too_expensive`
- `place_unavailable`
- `too_difficult`
- `weather_or_mood_changed`
- `other`

`quest_abandoned` keeps the existing structured columns used by the shared backend,
including `quest_duration_minutes`, `estimated_cost`, and `distance_meters` when
the catalogue distance can be normalized. Its `metadata` adds:

- `schema_version`
- `reason_code` and `reason_label`
- `quest_distance_label`, optional `quest_distance_meters`, and `quest_distance_band`
- `attempt_id`
- `current_step_index`, `completed_step_count`, `total_step_count`, and
  `progress_percent`
- `had_photo_proof` and `uploaded_photo_proof_count`

This makes the BQ6 dimensions available without an `analytics_events` schema
migration. A pipeline can select BQ6 source rows with:

```sql
select
  metadata->>'reason_code' as reason_code,
  quest_duration_minutes,
  estimated_cost,
  metadata->>'quest_distance_band' as distance_band,
  distance_meters,
  metadata->>'quest_distance_label' as distance_label
from public.analytics_events
where event_type = 'quest_abandoned';
```

Saving progress is not abandonment. `quest_progress_saved` leaves the attempt active and
does not write an abandonment reason. Abandoning clears the active attempt locally, and
accepting the quest again creates a new attempt ID.

## Validation

- `:app:testDebugUnitTest` covers BQ6 distance normalization and evidence generation.
- `PhotoProofFileFactoryTest` covers unique JPEG targets, path containment, invalid
  steps, and directory creation failures.
- `:app:assembleDebug` validates the complete Android build.
- Runtime validation still requires a camera-capable device or emulator plus a configured
  Supabase project with migration 009 deployed.
