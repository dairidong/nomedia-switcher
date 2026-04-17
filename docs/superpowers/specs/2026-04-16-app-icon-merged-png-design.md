# 2026-04-16 App Icon Merged PNG Design

## Goal

Produce a corrected app icon master from `icon_concept.svg` that preserves the intended visual design and fixes the current whitespace/composition problem. This round does not integrate the icon into the Android app yet. It only produces reviewable design assets.

## Scope

This task includes:

- using `icon_concept.svg` as the only design input
- correcting the icon composition so the outer whitespace is visually balanced
- preserving the original design language: background plate, photo card, lock, gradients, and shadows
- exporting a merged SVG master and a high-resolution PNG

This task does not include:

- generating Android launcher resources
- wiring the icon back into `AndroidManifest.xml`
- creating adaptive icon foreground/background assets

## Deliverables

Files will be written under `assets/app-icon/` at the repository root:

- `icon_merged.svg`
- `icon_full.png`

`icon_merged.svg` is the editable master after composition correction.

`icon_full.png` is the review artifact and future input for Android launcher integration.

## Design Rules

### Fidelity

The output must remain faithful to the approved design concept. This means:

- no redesign of the lock, photo, or base plate
- no stylistic simplification
- no removal of gradients or shadow treatment unless technically required for export

### Composition

The current issue is uneven whitespace around the icon contents. The corrected master must:

- keep the overall 512x512 artboard
- rebalance the visual center of the main composition
- make the four-side whitespace feel intentionally consistent
- preserve the original front/back relationship between photo and lock

This is a composition correction, not a redesign.

### Output Quality

The PNG export must:

- be high resolution
- preserve soft shadows and gradients
- remain visually close to the corrected merged SVG

## Approach Options

### Option 1: Direct master correction and export

Edit the source composition in SVG, then export a corrected merged PNG.

Pros:

- shortest path to a usable review artifact
- keeps the source of truth in vector form
- easiest to validate against the original concept

Cons:

- still requires careful manual judgment on optical centering

Recommended.

### Option 2: Split into layers first, then recombine

Create separate SVG layers for background, photo, and lock before correcting layout and exporting.

Pros:

- more flexible for later adaptive icon work

Cons:

- adds work before solving the immediate problem
- increases chances of new alignment drift during recomposition

Not recommended for this round.

## Data Flow

1. Read `icon_concept.svg`.
2. Create a corrected merged SVG master with balanced whitespace.
3. Export the corrected master to `icon_full.png`.
4. User reviews the merged SVG and PNG.
5. Only after approval does a later task generate Android launcher assets.

## Validation

The output is acceptable when all of the following are true:

- the lock remains present and clearly readable
- the photo card remains the dominant element
- the base plate still matches the intended soft rounded-square design
- the outer whitespace looks balanced enough that it no longer draws attention as a defect
- the PNG matches the corrected SVG visually

## Risks

- Optical centering is subjective, so a mathematically centered layout may still feel off.
- Shadows can enlarge the perceived bounds of the icon and make whitespace feel uneven even when geometry is balanced.
- Export tooling may slightly differ from SVG rendering, especially around blur and shadow softness.

## Testing

Testing for this round is visual verification:

- inspect the corrected merged SVG
- inspect the exported PNG
- compare both against the original concept for structure and spacing

If visual review passes, the next task can convert the approved PNG into Android launcher assets.
