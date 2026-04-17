# 2026-04-17 Adaptive Icon Foreground Background Design

## Goal

Replace the failed single-image icon approach with a standard Android adaptive icon design flow that separates the icon into a foreground layer and a background layer.

## Scope

This task includes:

- deriving a new adaptive icon foreground from the approved icon concept
- keeping both the photo and the lock in the foreground layer
- replacing the old large rounded-square base plate with a lightweight background layer
- preparing reviewable foreground, background, and merged preview assets before Android integration

This task does not include:

- direct Android resource integration in this round
- manifest changes
- launcher mipmap generation in this round

## Design Decisions

### Foreground Layer

The foreground contains:

- the photo card
- the lock

The foreground must use a transparent background.

The foreground is responsible for the icon identity, so the lock must remain visible and legible. The photo remains the dominant element, and the lock stays attached to it as part of the same protected-media concept.

The foreground composition must be corrected for Android adaptive icon safe-area use. This means the spacing is judged from the visible subject only, not from the previous full-canvas composition.

### Background Layer

The background does not keep the old large rounded-square plate from the original mockup.

The background becomes a clean supporting layer:

- plain light color, or
- very soft light gradient

The background must stay visually quiet so it does not compete with the photo and lock.

### Merged Preview

A merged preview is still needed for review, but only as a validation artifact. It is not the source of truth for Android integration.

The merged preview combines:

- the corrected foreground
- the simplified background

This preview exists to validate:

- subject balance
- outer whitespace
- lock readability
- desktop-style icon feel

## Rationale

The previous merged-image workflow kept the original large base plate and tried to solve spacing problems by moving a full illustration around inside a 512x512 canvas. That approach does not match how Android adaptive icons are normally built, and it made whitespace correction unreliable.

Separating foreground and background fixes the problem at the correct layer boundary:

- the foreground can be optically centered inside the safe area
- the background becomes stable and non-problematic
- later Android resource generation becomes straightforward

## Deliverables

The review round should produce:

- `foreground.svg`
- `foreground.png`
- `background.svg`
- `background.png`
- one merged preview image for review

These files should live under `assets/app-icon/`.

## Validation

The result is acceptable when all of the following are true:

- the lock is still present and readable
- the photo remains the main subject
- the foreground outer spacing feels balanced
- the simplified background does not compete with the foreground
- the merged preview looks suitable for later launcher integration

## Risks

- Removing the large original base plate changes the visual impression, so the new background must stay calm enough not to feel like a redesign.
- If the lock is scaled too small inside the foreground safe area, the icon may lose its protected-media meaning.
- If the background gradient is too strong, it will recreate the same visual noise problem in a different form.

## Next Step

After the user approves the review assets, a later task can convert the approved foreground and background into Android launcher resources.
