# Real-Device Follow-Up Design

## Overview

This follow-up iteration addresses issues found after installing `NoMedia Switcher` on a physical Android device.

The current app is functionally close to the intended v1 behavior, but several real-device gaps remain:

- `Download` subdirectories are blocked from normal authorization and toggling
- video-based albums frequently show placeholders instead of recognizable covers
- the main screen header feels heavier than necessary
- failed toggle operations do not provide an immediate in-app transient message
- scrolling feels less smooth than expected, so the task must end with a code-level performance review

This iteration should stay focused. It is not a redesign of the whole app. The goal is to fix real-device usability issues while keeping the existing architecture stable.

## Goals

- Allow switchable subdirectories under `Download`
- Improve the reliability of video album covers without adding an expensive thumbnail pipeline
- Simplify the main screen top area by removing the visible title text and keeping a settings entry in the header
- Show a short in-app transient failure message when a foreground toggle fails
- Review the final code for low-cost performance improvements that could improve scrolling smoothness

## Non-Goals

- Supporting the `Download` root directory itself as a managed album
- Building a dedicated sidebar or drawer navigation system
- Adding a media thumbnail cache database or pre-generated cover pipeline
- Adding runtime profiling instrumentation in this iteration
- Rewriting the list screen into a materially different layout system

## Real-Device Issues

### 1. `Download` Subdirectories Cannot Be Toggled

The current app treats `Download` too broadly as a restricted area. That is acceptable for the root directory itself, but not for child directories that should still be managed individually.

Desired behavior:

- `Download` root remains non-switchable
- `Download/<child>` directories are allowed if they otherwise map cleanly to one directory
- authorization and persisted tree validation should work for those child directories the same way they do for other normal folders

### 2. Video Albums Often Show No Cover

The current implementation stores a media URI for video albums, then relies on the UI image pipeline to decode a frame directly from the video item at render time.

That approach is lightweight enough in principle, but it is not reliable enough on real devices. Some devices or providers fail to decode the first frame consistently, leaving the user with placeholders even when the album is valid.

Desired behavior:

- continue to prefer lightweight runtime resolution over a heavy thumbnail-generation pipeline
- improve the success rate of visible video covers
- keep fallback behavior cheap and predictable when video cover resolution fails

### 3. Header Feels Too Heavy

The current list page still resembles a standard titled app bar. For this app, the visible `Albums` / `图集` title is not carrying enough value.

Desired behavior:

- remove the visible title text from the main screen header
- keep a lightweight top area
- keep a settings entry in the top-right area
- avoid introducing a drawer in this iteration

### 4. Failures Need an Immediate In-App Message

The app already persists failure state and can notify in the background, but foreground interactions still feel too quiet when a toggle fails.

Desired behavior:

- when the user is in the app and a toggle finishes with failure, show a short transient message
- do not replace existing row state, progress state, or notifications
- reuse existing localized failure messaging where possible

### 5. Scrolling Feels Slightly Janky

The user reports that scrolling does not feel as smooth as expected. This could be caused by one or more of:

- image/video cover decode cost during list movement
- repeated fallback cover resolution work
- avoidable recomposition or remapping cost in list state production

This iteration should include a code review pass focused on likely low-cost improvements, but not a full profiling exercise.

## Recommended Approach

Use a focused incremental approach:

1. Narrow the reserved-directory restriction so it only blocks the `Download` root, not every descendant path.
2. Keep the existing media-store-first cover model, but strengthen the video cover display path and add a cheap fallback path instead of relying on raw frame extraction success alone.
3. Simplify the top app bar into a titleless action bar with only a settings affordance.
4. Add a foreground-only transient message event for toggle failures.
5. End the task with a dedicated code review pass for scroll performance risks and apply only small, defensible optimizations.

This is the best tradeoff because it fixes the practical product issues without overcorrecting into a large architectural refactor.

## Alternatives Considered

### Option 1: Full Thumbnail Pipeline

Generate and persist dedicated image thumbnails for video albums.

Pros:

- most control over final cover appearance
- likely highest steady-state display reliability

Cons:

- adds background work, cache invalidation, and storage concerns
- too much complexity for this iteration

### Option 2: Query System Thumbnails Immediately

Replace the current direct video-item display path with an explicit thumbnail query layer everywhere.

Pros:

- potentially more stable than direct video decode

Cons:

- increases implementation complexity across scanner and cover layers
- still may need fallback handling
- not the first thing to try before tightening the current path

### Option 3: Focused Real-Device Fixes

Keep the current architecture, repair the weak points, and add a code-level performance pass.

Pros:

- smallest risk surface
- fastest route to a noticeably better app
- preserves recent work

Cons:

- may still leave room for a future dedicated thumbnail improvement round

## Component Design

### Reserved Directory Policy

The reserved-directory policy should distinguish between:

- exact reserved root directories
- children of those directories

Rule change:

- exact `Download` remains blocked
- descendants such as `Download/Telegram` or `Download/MyApp` are allowed

Any grant validation that currently assumes all `Download` paths are invalid should be updated to match this narrower rule.

### Video Cover Resolution

The app should continue using media-store album scan output as the primary cover source.

Recommended behavior:

- keep storing the media URI and media kind for the newest visible album item
- treat video albums as a distinct rendering case in the cover UI
- constrain the request for small-list usage rather than display-quality usage
- add a clear error fallback so a failed video decode does not silently degrade the entire cover experience

If the current direct video-item path still proves insufficient after this change, a later iteration can add a dedicated system-thumbnail resolver.

### Header Layout

The main screen should use a lighter top bar:

- no visible page title text
- a single settings action in the trailing area
- spacing and structure should still make the screen feel intentional rather than unfinished

This should preserve the existing navigation model and avoid introducing more navigation state.

### Foreground Failure Messaging

Failure events should be emitted as transient UI events rather than persisted UI state.

Recommended flow:

1. Toggle fails
2. Existing failure state updates continue as they do today
3. If the app is foregrounded on the album list flow, emit one short localized message
4. UI consumes it once and shows a toast or equivalent short transient message

The persisted failure reason remains the source of truth for durable state.

### Performance Review Pass

At the end of implementation, review the code for likely scroll-performance issues, especially:

- unnecessary per-emission fallback cover work in the list view model
- repeated object churn in list row mapping
- expensive image requests during list scroll
- missing low-cost list stability improvements

The goal is not broad speculative optimization. It is to identify and apply only obvious improvements with favorable complexity-to-benefit tradeoffs.

## Testing Strategy

Implementation should cover:

- unit tests for reserved-directory policy around `Download` root vs `Download` children
- unit tests for any new transient failure-message event flow
- cover-related tests for video display path and fallback behavior where practical
- UI tests or compose tests for the simplified top app bar behavior

Verification should include:

- `:app:testDebugUnitTest`
- `:app:compileDebugAndroidTestKotlin`
- `:app:assembleDebug`

Manual real-device verification should confirm:

- a `Download` child directory can be authorized and toggled
- a video album shows a recognizable cover more reliably than before
- the main screen no longer shows the visible title text
- a failed toggle shows a short in-app message
- scrolling feels at least no worse, with any code-level improvements documented

## Risks And Mitigations

### Risk: Relaxing `Download` Handling Reintroduces Disallowed Root Access

Mitigation:

- match only the exact root directory for blocking
- add tests specifically for root vs child behavior

### Risk: Video Cover Fixes Improve Reliability but Still Decode Too Much During Scroll

Mitigation:

- keep requests sized for list usage
- add a cheap error fallback
- review code for avoidable repeated work after implementation

### Risk: Toast-Like Messaging Becomes Noisy

Mitigation:

- only emit on real failure completion
- keep the message short
- do not duplicate with additional foreground banners in this iteration

## Acceptance Criteria

- `Download` root is still excluded, but switchable child directories under `Download` can be authorized and toggled.
- Video albums display recognizable covers with a higher real-device success rate than the current build.
- The main list screen header no longer shows the page title text and still exposes settings from the top-right area.
- Foreground toggle failures show a short in-app transient message without removing the existing durable failure state.
- The implementation closes with a documented code review for likely scroll-performance improvements, and any small justified optimizations are applied.
