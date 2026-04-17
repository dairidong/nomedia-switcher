# App Icon And Album Sort Design

## Overview

This iteration adds two user-facing improvements:

- replace the current launcher icon with the provided `icon_concept.svg` design asset
- add a user-selectable album sorting mode that can switch between name order and latest-media-time order, with the selected mode persisted as the default

The existing hidden-album pinning behavior must remain the primary grouping rule. Sorting is only allowed to change order inside the existing priority groups; it must not override pinned hidden albums.

## Goals

- use the provided SVG as the source for the app launcher icon
- configure the app to ship with Android launcher resources derived from that design
- let the user choose album sorting by:
  - name
  - latest media time
- persist the selected sorting mode in user settings
- preserve the existing hidden-albums-to-top behavior exactly as a higher-priority rule

## Non-Goals

- redesigning the overall settings screen layout
- adding a manual drag-and-drop custom album ordering system
- sorting by filesystem directory metadata such as folder `mtime`
- reading per-directory timestamps from the filesystem or SAF layer

## Problem Framing

### 1. App Icon

The project currently lacks the intended custom launcher icon. The supplied design asset is `icon_concept.svg` at the repository root.

Android launcher icons work best when they are packaged as adaptive icon resources rather than treated as a raw SVG runtime asset. The SVG should therefore be treated as a design source and converted into launcher resources that Android can use consistently across devices and launchers.

### 2. Album Sorting

The current album list is effectively name-sorted inside the existing state-priority ordering. The user wants sorting to be switchable and remembered.

Sorting by folder metadata is not recommended because:

- directory timestamps are not stable across storage implementations
- toggling `.nomedia` can itself perturb folder timestamps
- filesystem-based folder metadata is less portable and less trustworthy than media-library metadata

The stable definition for “time sort” should therefore be:

- sort by the newest media timestamp inside the album, descending

This better matches user expectations for “recently active albums” and can be derived from existing media scan data with low overhead.

## Recommended Approach

Use a focused implementation with two parts:

1. Convert `icon_concept.svg` into Android launcher icon resources and update the manifest/icon references.
2. Add a persisted sort mode to user settings, expose it in the settings UI, and update album merge ordering so that:
   - processing state still ranks first
   - hidden albums still honor the existing pin-to-top setting
   - sorting mode applies only within those groups
   - latest-media sorting uses media scan timestamps, not directory timestamps

This is the best tradeoff because it improves user-facing polish and list control without introducing filesystem fragility or large performance cost.

## Alternatives Considered

### Option 1: Sort By Directory Timestamp

Pros:

- superficially simple concept

Cons:

- unstable across Android storage implementations
- polluted by `.nomedia` creation/removal
- less aligned with what users mean by “recent”

Rejected.

### Option 2: Sort By Persisted Album Record Update Time

Pros:

- easy to implement from existing local state

Cons:

- polluted by app actions such as hide/show
- not actually a media recency signal

Rejected.

### Option 3: Sort By Latest Media Timestamp

Pros:

- matches user expectation best
- derived from data already available during media scan
- avoids filesystem directory metadata pitfalls

Cons:

- hidden albums missing from scan need a persisted fallback timestamp

Recommended.

## Component Design

### Icon Pipeline

Source asset:

- repository root `icon_concept.svg`

Recommended resource strategy:

- generate Android launcher resources from the SVG design
- use adaptive icon resources where possible
- provide a safe background layer if the artwork needs breathing room inside the adaptive mask

Expected implementation shape:

- add foreground/background icon resources under `res`
- update launcher icon XML resources under `mipmap-anydpi-v26`
- ensure manifest icon references point at the updated launcher resources

The SVG itself remains a source asset for future regeneration, not a runtime-loaded app asset.

### Sort Mode Model

Add a persisted sort mode to user settings, for example:

- `ByName`
- `ByLatestMediaTime`

The setting should be stored alongside the existing hidden-albums pinning setting.

### Media Time Source

Each scanned album should expose or retain the latest media timestamp derived from `MediaStore`.

Recommended source:

- the newest timestamp already implied by the scan row order or explicit timestamp field from the latest media item in each album

For albums not currently present in scan results:

- if a last-known media timestamp exists in local album state, use it
- otherwise treat the timestamp as unknown and fall back to stable secondary ordering

### Sort Precedence Rules

Ordering must be applied in layers:

1. Processing albums first
2. Hidden albums grouped ahead of shown albums only when the existing pin-hidden setting is enabled
3. Selected sort mode applied within each resulting group
4. Stable fallback ordering by display name and directory key

This guarantees that the new sort feature does not break the existing hidden pinning behavior.

### Settings UI

The settings screen should gain a second preference section for sort mode.

Requirements:

- the control should be easy to scan on mobile
- current selection must be visible
- user selection must persist immediately

The exact control may be segmented choices, radio-style rows, or another lightweight selection UI, but it should remain consistent with the existing simple settings page.

## Data Model Changes

Expected additions:

- user settings: new persisted album sort mode
- scanned album candidate or merged album state: latest media timestamp
- local album record: optional last-known latest media timestamp for hidden/missing albums

This local persisted timestamp is important so time sorting does not collapse for albums that disappear from the live media scan after hiding.

## Performance Considerations

The recommended time sort should have low overhead if implemented during the existing media scan pass.

Acceptable approach:

- capture latest media time while already reading media rows
- store one timestamp per album
- sort using that cached value during merge

Avoid:

- per-directory filesystem metadata reads
- per-row SAF lookups
- post-processing that walks the filesystem after scan

Expected impact:

- negligible to small compared with the existing scan itself
- much cheaper than any filesystem-based directory timestamp approach

## Testing Strategy

Implementation should cover:

- unit tests for user settings serialization/deserialization with the new sort mode
- unit tests for album merge ordering:
  - hidden pinning still wins over sort mode
  - name sort behaves as before inside groups
  - latest-media sort orders albums by descending media time inside groups
  - missing timestamps fall back to stable ordering
- verification that hidden/missing albums can still participate in time sort via persisted timestamp
- manual verification of launcher icon resources and manifest wiring

Verification should include:

- `:app:testDebugUnitTest`
- `:app:assembleDebug`
- manual launcher icon inspection on emulator/device

## Risks And Mitigations

### Risk: Adaptive Icon Cropping Makes The New Icon Look Wrong

Mitigation:

- add a controlled background layer
- keep artwork within a safe inset
- verify visually on emulator launcher

### Risk: Time Sort Breaks Hidden Album Pinning

Mitigation:

- keep pinning as a higher-priority grouping rule
- add targeted ordering tests for pinned vs non-pinned states

### Risk: Hidden Albums Lose Time Sort Position Once Missing From Scan

Mitigation:

- persist last-known latest media timestamp into local album state
- fall back to name ordering only when no timestamp is available

## Acceptance Criteria

- The app launcher icon uses resources derived from `icon_concept.svg`.
- The settings screen lets the user switch album sorting between name and latest-media-time order.
- The selected sort mode persists across app restarts.
- Latest-media sorting uses album media recency rather than directory filesystem timestamps.
- Existing hidden-albums pinning behavior remains intact and takes precedence over the new sort mode.
