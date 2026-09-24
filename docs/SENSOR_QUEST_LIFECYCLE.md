# Sensor and quest-lifecycle integration

This branch implements the Kotlin-owned Sprint block on top of
`feature/quest-progress-persistence`.

## Photo-proof flow

1. A photo step launches the device camera with `ActivityResultContracts.TakePicture`.
2. `CameraPhotoProofService` creates a full-resolution JPEG target inside the app's
   private files directory and shares only a temporary `FileProvider` URI with the camera.
3. The active quest keeps evidence per step, so one photo cannot satisfy multiple
   photo-required steps.
4. The local photo lets the user continue if the network is unavailable.
5. When Supabase is configured, `PhotoProofRepository` uploads the JPEG to the private
   Storage bucket and exposes retry state if the upload fails.
6. Analytics records `photo_proof_captured`, `photo_proof_uploaded`, or
   `photo_proof_upload_failed`. Events contain technical evidence only; they do not put
   image bytes or a public URL into `analytics_events`.

The default bucket is `quest-proofs`. It can be changed with the Gradle property or
environment variable `SUPABASE_QUEST_PROOFS_BUCKET`.

### Required Storage policy

The backend must create a private bucket and allow an authenticated user to insert only
under a folder named with their own user ID. The app writes objects as:

```text
<user-id>/<attempt-id>/<quest-id>/step-<number>-<uuid>.jpg
```

Example Supabase SQL for the backend repository:

```sql
insert into storage.buckets (id, name, public)
values ('quest-proofs', 'quest-proofs', false)
on conflict (id) do update set public = false;

create policy "Users upload their own quest proofs"
on storage.objects
for insert
to authenticated
with check (
  bucket_id = 'quest-proofs'
  and (storage.foldername(name))[1] = (select auth.uid()::text)
);
```

Reading or deleting evidence should receive separate least-privilege policies only when
the product flow needs those operations.

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
including `quest_duration_minutes` and `estimated_cost`. Its `metadata` adds:

- `schema_version`
- `reason_code` and `reason_label`
- `quest_distance_label`, optional `quest_distance_meters`, and `quest_distance_band`
- `attempt_id`
- `current_step_index`, `completed_step_count`, `total_step_count`, and
  `progress_percent`
- `had_photo_proof` and `uploaded_photo_proof_count`

This makes the BQ6 dimensions available without requiring an immediate
`analytics_events` schema migration. A pipeline can select BQ6 source rows with:

```sql
select
  metadata->>'reason_code' as reason_code,
  quest_duration_minutes,
  estimated_cost,
  metadata->>'quest_distance_band' as distance_band,
  metadata->>'quest_distance_meters' as distance_meters
from public.analytics_events
where event_type = 'quest_abandoned';
```

Saving progress is not abandonment. `quest_progress_saved` leaves the attempt active and
does not write an abandonment reason. Abandoning clears the active attempt locally, and
accepting the quest again creates a new attempt ID.

## Validation

- `:app:testDebugUnitTest` covers BQ6 distance normalization and evidence generation.
- `:app:assembleDebug` validates the complete Android build.
- Runtime validation still requires a camera-capable device or emulator plus a configured
  Supabase project with the Storage bucket and policy above.
