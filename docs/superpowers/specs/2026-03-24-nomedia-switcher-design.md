# NoMedia Switcher Android App Design

## Overview

`NoMedia Switcher` is an Android-only utility app for toggling `.nomedia` on photo album directories so selected albums can be hidden from gallery apps and later restored.

The user-facing model is "album-first," but the implementation model is "directory-first." The app presents only albums that can be stably mapped to a real filesystem directory, then creates or removes `.nomedia` in that directory.

The first release is optimized for a specific UX problem in existing `.nomedia` apps: slow toggles provide poor feedback. This app must make long-running hide/show operations understandable through immediate status changes, a dismissible progress surface, and completion notifications.

## Goals

- Show a list of switchable albums using an album-oriented UI rather than a raw folder manager.
- Let the user toggle each album independently.
- Provide clear feedback for slow hide/show operations.
- Keep operations running after the progress UI is dismissed.
- Surface final results through both in-app state and system notifications.
- Preserve user-managed hidden albums in the list even when they disappear from the media scan after `.nomedia` is applied.

## Non-Goals

- Batch toggle operations.
- Time-based, scene-based, or app-based automatic switching.
- Support for logical albums that cannot be mapped to a single directory.
- File encryption, vault import/export, disguised icon mode, or other privacy-vault features.
- Root-only, Shizuku-only, or OEM-private integrations.
- A guarantee that every third-party gallery app will refresh immediately after a toggle.

## Target Platform

- Android only
- `minSdk = 30` (Android 11)
- `targetSdk = 35` (Android 15)

## Primary User Flow

1. User launches the app.
2. The app immediately loads locally known album records and renders high-priority items first.
3. The app starts a background media scan to discover switchable albums.
4. The scan results are merged with local records.
5. The user toggles an album.
6. The album row immediately enters a processing state.
7. A bottom progress sheet appears and shows the active task.
8. The user may dismiss the sheet while the task keeps running in the background.
9. On success or failure, the app updates album state, updates the list, and posts a system notification.

## Album Eligibility

The app only exposes albums that can be stably mapped to a real directory suitable for `.nomedia` management.

### In Scope

- Albums whose underlying media items resolve to a single real directory
- Albums derived from local shared storage media that the app can inspect and manage

### Out of Scope

- Favorites
- Trash / Recently deleted
- Cloud-only albums
- Timeline / Recent / All photos aggregate views
- Person, place, search-result, memory, or AI-generated albums
- Any gallery-specific logical grouping that spans multiple directories or cannot be mapped reliably

If an album cannot be mapped to one directory with confidence, it is not shown.

## Product Surfaces

### Main Screen

The main screen shows a list of switchable albums. Each list item includes:

- Album name
- Optional directory summary or path hint
- Current switch state
- Current task/result badge when applicable
- A single toggle control

The list is sourced from two inputs:

- Local album state records
- Current media scan results

The local state source is rendered first so previously hidden albums remain visible even before scanning completes.

### Progress Bottom Sheet

The progress UI appears when a hide/show operation starts. It shows:

- Album name
- Target action: hide or show
- Current task state
- Brief explanatory text for slow operations
- Dismiss control

Dismissal hides the sheet only. It does not cancel the task.

### Notifications

When a task completes after the user has dismissed the progress sheet, the app posts a notification. Notifications must cover:

- Successful hide
- Successful restore/show
- Failure, including a retry affordance if feasible

### Settings

The first release includes a setting:

- `Pin hidden albums to top`

Default: enabled

## State Model

Each album item has one effective presentation state:

- `Shown`
- `Hidden`
- `Processing`
- `Failed`
- `HiddenMissingFromScan`

### State Meaning

- `Shown`: album is visible to the media library and no active task is running.
- `Hidden`: album is managed by the app and currently hidden.
- `Processing`: a hide or show task is currently running.
- `Failed`: the most recent requested operation failed.
- `HiddenMissingFromScan`: the album was previously hidden and is still known locally, but the current media scan does not surface it. This preserves recoverability.

The app should not claim to know which gallery app or scanner excluded a hidden album. It only reports what it can verify: the album is locally known, hidden, and absent from the latest scan.

## Startup and Merge Strategy

Startup prioritizes continuity over completeness:

1. Read persisted album records and settings.
2. Render locally known high-priority albums immediately.
3. Launch media scan asynchronously.
4. Convert scan results into switchable albums.
5. Merge scanned albums with local records by normalized directory identity.
6. Refresh visible ordering and item details.

This ensures hidden albums, failed items, and in-progress items remain visible even if the media scan is delayed or no longer returns those albums.

## Sorting Rules

Baseline ordering:

1. `Processing`
2. Hidden states (`Hidden`, `HiddenMissingFromScan`) when the setting is enabled
3. `Failed`
4. Remaining shown albums

Within the same group, sort by stable user-friendly order, such as album name. If implementation uncovers a clearly better stable signal, that can be chosen during planning, but sorting must remain deterministic.

If `Pin hidden albums to top` is disabled, hidden states should no longer receive priority above normal shown albums.

## Background Task Behavior

Each user toggle creates one background job for one album.

Requirements:

- The UI must update immediately to `Processing`.
- The task must continue after the bottom sheet is dismissed.
- The task must report final state back to both list UI and notifications.
- Repeated taps on the same album while processing should be blocked or ignored for that album.
- The first release handles single-album operations only.

The app may serialize tasks globally in v1 for simplicity. Concurrency beyond one active task is not required in the first release.

## Directory Toggle Behavior

### Hide Operation

- Ensure the target directory is resolved and writable via the chosen Android storage access approach.
- Create `.nomedia` in the target directory if it does not already exist.
- Trigger the appropriate media refresh/update path.
- Update local record state to hidden on success.

### Show Operation

- Resolve the previously known target directory.
- Remove `.nomedia` from the target directory if present.
- Trigger the appropriate media refresh/update path.
- Update local record state to shown on success.

If the operation succeeds but media visibility takes time to refresh in third-party apps, the app still reports success for the filesystem operation and should avoid implying immediate visibility everywhere.

## Error Handling

The app must produce explicit user-visible outcomes for:

- Directory access denied
- Directory no longer exists
- File create/delete failure
- Media refresh/update failure or timeout
- Invalid mapping between album and directory

Failure handling requirements:

- Album row moves to `Failed`
- Bottom sheet shows failure summary if visible
- Notification communicates failure if the task finishes in the background
- The user can retry from the album row or the notification when practical

Failure text should be short and concrete, for example "Couldn't write to album directory" rather than a generic "Operation failed."

## Technical Architecture

Recommended stack:

- Kotlin
- Jetpack Compose
- AndroidX architecture components
- Room or equivalent local persistence for album records and settings
- WorkManager or an equivalent persistent background execution approach for toggle jobs

### Logical Components

#### Album Discovery

Responsibilities:

- Read media library entries from the system media store
- Group items into candidate albums
- Determine whether a candidate maps stably to one directory
- Return only switchable albums

#### Album State Store

Responsibilities:

- Persist album directory identity
- Persist user-visible state
- Persist last operation metadata
- Persist settings such as hidden-albums-first

#### Toggle Executor

Responsibilities:

- Perform `.nomedia` create/delete operations
- Emit task progress and final status
- Coordinate media refresh

#### Presentation Layer

Responsibilities:

- Render merged album list
- Render progress sheet
- Render transient and durable result states
- Route retry actions

## Data Model

The precise schema can be finalized during planning, but the app needs at least:

### Album Record

- Stable local id
- Album display name
- Normalized directory identifier
- Directory path or persisted access reference
- Last known visibility state
- Last operation type
- Last operation timestamp
- Last failure summary, if any
- Whether the item was seen in the most recent scan

### Settings Record

- `pinHiddenAlbumsToTop: Boolean`

## UX Requirements

- Tapping a toggle must produce immediate visible feedback.
- Slow operations must never appear idle or frozen.
- The progress surface must be dismissible without cancelling work.
- Completion must be visible even if the user leaves the progress surface.
- Hidden albums must remain manageable after they vanish from the scan.
- The app should feel album-centric, not file-manager-centric.

## Permissions and Access Constraints

The app should use modern Android shared storage patterns compatible with Android 11+.

Constraints:

- No root assumptions
- No OEM-private APIs
- No dependence on unsupported hacks for broad storage writes

Planning must choose the most reliable access model available for `.nomedia` create/delete against user-managed album directories under modern Android storage rules. If a particular directory cannot be reliably managed under those rules, it should not be exposed as switchable.

## Open Planning Questions

These questions do not block the design, but must be resolved during implementation planning:

- Exact directory access strategy on Android 11+ for create/delete of `.nomedia`
- Exact media refresh mechanism after hide/show completes
- Exact album grouping heuristic and normalization key used to merge scan results with local records
- Whether retry from notification is implemented directly or deep-links back into the app

## Release Acceptance Criteria

The v1 design is successful if:

- The app lists switchable albums only.
- A user can hide one album and later restore it.
- Slow operations clearly show `Processing`.
- The progress bottom sheet can be dismissed without cancelling work.
- Completion or failure is visible through notifications and list state.
- Hidden albums remain accessible even when absent from the latest media scan.
- The user can enable or disable hidden-albums-first sorting.
