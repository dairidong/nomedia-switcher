# Real-Device Follow-Up Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the real-device issues around `Download` child album authorization, unreliable video covers, heavy main-screen header, missing in-app failure feedback, and close with a code-level scroll-performance review.

**Architecture:** Keep the existing media-store-first and directory-first architecture. Narrow the reserved-directory and grant-validation rules instead of redesigning album discovery, tighten the album cover rendering path rather than building a thumbnail pipeline, and layer transient foreground failure messaging on top of the existing durable state model. Finish by reviewing likely scroll hot spots and applying only low-risk optimizations.

**Tech Stack:** Kotlin, Jetpack Compose, Coil, AndroidX ViewModel, Room, WorkManager, JUnit4, Robolectric, Compose UI Test, Gradle

---

## Planned File Structure

### Directory Eligibility And Grants

- Modify: `app/src/main/java/com/nomedia/switcher/data/media/SystemReservedDirectoryPolicy.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/data/access/DirectoryGrantRepository.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/media/SystemReservedDirectoryPolicyTest.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/access/DirectoryGrantRepositoryTest.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/media/MediaStoreAlbumLoaderTest.kt`

These files control which directories are shown and which ones can request tree access. They need to distinguish `Download` root from `Download/<child>`.

### Cover Rendering

- Create: `app/src/main/java/com/nomedia/switcher/ui/common/AlbumCoverRequestSpec.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/common/AlbumCover.kt`
- Test: `app/src/test/java/com/nomedia/switcher/ui/common/AlbumCoverRequestSpecTest.kt`
- Modify: `app/src/androidTest/java/com/nomedia/switcher/ui/albums/AlbumListScreenTest.kt`

These files should make the video cover path explicit, small-list-sized, and resilient to decode failures while keeping the UI cheap enough for scrolling.

### Main Screen Header

- Modify: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/androidTest/java/com/nomedia/switcher/ui/albums/AlbumListScreenTest.kt`

These files define the top bar structure and the strings needed for an icon-only settings action.

### Foreground Failure Messaging

- Modify: `app/src/main/java/com/nomedia/switcher/ui/UiMessage.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListViewModel.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/AppRoot.kt`
- Test: `app/src/test/java/com/nomedia/switcher/ui/albums/AlbumListViewModelTest.kt`
- Create: `app/src/test/java/com/nomedia/switcher/ui/UiMessageResolverTest.kt`

These files should expose one-shot foreground failure messages without replacing the existing persisted state and notification behavior.

### Final Performance Pass

- Inspect: `app/src/main/java/com/nomedia/switcher/ui/common/AlbumCover.kt`
- Inspect: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListScreen.kt`
- Inspect: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListViewModel.kt`
- Optional modify: whichever of the above needs a low-risk optimization
- Test: whichever focused tests prove the optimization

This final pass should target likely low-cost scroll issues, not speculative refactors.

## Task 1: Allow `Download` Child Albums

**Files:**
- Modify: `app/src/main/java/com/nomedia/switcher/data/media/SystemReservedDirectoryPolicy.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/data/access/DirectoryGrantRepository.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/media/SystemReservedDirectoryPolicyTest.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/access/DirectoryGrantRepositoryTest.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/media/MediaStoreAlbumLoaderTest.kt`

- [ ] **Step 1: Write the failing tests for `Download` root vs child behavior**

```kotlin
@Test
fun isReserved_returns_false_for_download_children() {
    assertFalse(policy.isReserved("Download/Telegram"))
    assertFalse(policy.isReserved("Download/MyApp/Exports"))
}

@Test
fun validateGrantRequest_allows_download_child_directory() = runTest {
    assertNull(repository.validateGrantRequest("Download/Telegram"))
}

@Test
fun load_keeps_download_child_albums_switchable() = runTest {
    val album = loader.load(contentResolver).single { it.directoryKey == "Download/Telegram" }
    assertEquals("video", album.coverMediaKind)
}
```

- [ ] **Step 2: Run the targeted unit tests to verify they fail**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*SystemReservedDirectoryPolicyTest' --tests '*DirectoryGrantRepositoryTest' --tests '*MediaStoreAlbumLoaderTest'`

Expected: FAIL because the current code blocks `Download` descendants during grant validation. `SystemReservedDirectoryPolicy` may already allow some `Download/<child>` paths today, so keep the implementation focused on the actual failing boundary.

- [ ] **Step 3: Implement the narrow root-only restriction**

```kotlin
fun validateGrantRequest(directoryKey: String): GrantError? {
    val segments = directoryKey
        .split('/')
        .map(String::trim)
        .filter(String::isNotEmpty)
    return if (segments.size == 1 && segments[0].equals("Download", ignoreCase = true)) {
        GrantError.RestrictedRoot
    } else {
        null
    }
}
```

Apply the same root-only interpretation to the reserved-directory policy so `Download` root remains excluded while `Download/<child>` remains visible and grantable.

- [ ] **Step 4: Re-run the targeted unit tests**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*SystemReservedDirectoryPolicyTest' --tests '*DirectoryGrantRepositoryTest' --tests '*MediaStoreAlbumLoaderTest'`

Expected: PASS

- [ ] **Step 5: Commit the `Download` child fix**

```bash
git add app/src/main/java/com/nomedia/switcher/data/media/SystemReservedDirectoryPolicy.kt app/src/main/java/com/nomedia/switcher/data/access/DirectoryGrantRepository.kt app/src/test/java/com/nomedia/switcher/data/media/SystemReservedDirectoryPolicyTest.kt app/src/test/java/com/nomedia/switcher/data/access/DirectoryGrantRepositoryTest.kt app/src/test/java/com/nomedia/switcher/data/media/MediaStoreAlbumLoaderTest.kt
git commit -m "fix: allow download child albums"
```

## Task 2: Stabilize Video Album Covers

**Files:**
- Create: `app/src/main/java/com/nomedia/switcher/ui/common/AlbumCoverRequestSpec.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/common/AlbumCover.kt`
- Create: `app/src/test/java/com/nomedia/switcher/ui/common/AlbumCoverRequestSpecTest.kt`
- Modify: `app/src/androidTest/java/com/nomedia/switcher/ui/albums/AlbumListScreenTest.kt`

- [ ] **Step 1: Write the failing tests for the more reliable video-cover request path**

```kotlin
@Test
fun video_cover_request_uses_non_zero_video_frame_and_small_size() {
    val spec = buildAlbumCoverRequestSpec(
        coverUri = "content://media/external/video/media/5",
        coverMediaKind = "video",
        targetSizePx = 128,
    )

    assertEquals("content://media/external/video/media/5", spec.data)
    assertEquals(true, spec.useVideoFrame)
    assertEquals(128, spec.targetSizePx)
    assertEquals(1_000L, spec.videoFrameMillis)
}

@Test
fun image_cover_request_skips_video_frame_mode() {
    val spec = buildAlbumCoverRequestSpec(
        coverUri = "content://media/external/images/media/9",
        coverMediaKind = "image",
        targetSizePx = 128,
    )

    assertEquals(false, spec.useVideoFrame)
}
```

Also update the existing UI test for a video-album row so it still expects a non-null video cover URI to render the cover node rather than the placeholder node.

Add one more failing UI test for the error fallback:

```kotlin
@Test
fun album_with_broken_video_cover_falls_back_to_placeholder() {
    composeTestRule.setContent {
        NoMediaTheme {
            AlbumListScreen(
                state = AlbumListUiState(
                    albums = listOf(
                        AlbumRowState(
                            id = AlbumId("Movies/Broken"),
                            displayName = "Broken",
                            directorySummary = "Movies/Broken",
                            state = AlbumState.Shown,
                            coverUri = "content://media/external/video/media/does_not_exist",
                            coverMediaKind = "video",
                            isChecked = false,
                            isToggleEnabled = true,
                            nextAction = ToggleAction.Hide,
                        ),
                    ),
                ),
                onToggleClick = {},
                onOpenSettings = {},
                highlightedAlbumId = null,
            )
        }
    }

    composeTestRule
        .onNodeWithTag("album-cover-placeholder-Movies/Broken")
        .assertIsDisplayed()
}
```

- [ ] **Step 2: Run the targeted tests to verify they fail**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*AlbumCoverRequestSpecTest'`

Expected: FAIL because the request-spec helper does not exist yet and the cover component does not yet guarantee placeholder fallback on broken video-cover requests.

- [ ] **Step 3: Implement the explicit video-cover reliability tactic**

```kotlin
internal data class AlbumCoverRequestSpec(
    val data: String,
    val useVideoFrame: Boolean,
    val targetSizePx: Int,
    val videoFrameMillis: Long? = null,
)

internal fun buildAlbumCoverRequestSpec(
    coverUri: String,
    coverMediaKind: String?,
    targetSizePx: Int,
): AlbumCoverRequestSpec {
    return AlbumCoverRequestSpec(
        data = coverUri,
        useVideoFrame = coverMediaKind == "video",
        targetSizePx = targetSizePx,
        videoFrameMillis = if (coverMediaKind == "video") 1_000L else null,
    )
}
```

Then update `AlbumCover.kt` to:

- build requests through the helper
- constrain requests to a list-sized target
- use a small non-zero `videoFrameMillis` for video items instead of frame `0`, to avoid fragile first-frame extraction on real devices
- render the placeholder UI when the image request errors instead of leaving the row visually blank
- keep image behavior unchanged

Recommended implementation shape:

- switch from a fire-and-forget image composable to a state-aware image composable
- when the painter reports an error, render the same tagged placeholder path the null-cover case uses

- [ ] **Step 4: Re-run the targeted tests**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*AlbumCoverRequestSpecTest'`

Expected: PASS

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nomedia.switcher.ui.albums.AlbumListScreenTest`

Expected: PASS for both the normal video-cover node case and the broken-video-cover placeholder fallback case

Then run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:compileDebugAndroidTestKotlin`

Expected: PASS

Manual verification note:

- install the new debug build on a physical device
- confirm at least one known video album now shows a recognizable cover where the previous build showed a placeholder

- [ ] **Step 5: Commit the video-cover stabilization**

```bash
git add app/src/main/java/com/nomedia/switcher/ui/common/AlbumCoverRequestSpec.kt app/src/main/java/com/nomedia/switcher/ui/common/AlbumCover.kt app/src/test/java/com/nomedia/switcher/ui/common/AlbumCoverRequestSpecTest.kt app/src/androidTest/java/com/nomedia/switcher/ui/albums/AlbumListScreenTest.kt
git commit -m "perf: stabilize video album covers"
```

## Task 3: Simplify the Main Screen Header

**Files:**
- Modify: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/androidTest/java/com/nomedia/switcher/ui/albums/AlbumListScreenTest.kt`

- [ ] **Step 1: Update the failing UI tests for a titleless top bar**

```kotlin
@Test
fun album_list_hides_visible_page_title_and_shows_settings_action() {
    composeTestRule.setContent {
        WithZhCnLocale {
            AlbumListScreen(
                state = AlbumListUiState(),
                onToggleClick = {},
                onOpenSettings = {},
                highlightedAlbumId = null,
            )
        }
    }

    composeTestRule.onNodeWithText("图集").assertDoesNotExist()
    composeTestRule
        .onNodeWithContentDescription(context.getString(R.string.album_list_open_settings))
        .assertIsDisplayed()
}
```

Also update or replace the existing `album_list_uses_resource_text_for_static_labels` test so it no longer expects the visible title text and instead asserts:

- the settings action remains visible
- the empty-state text still comes from resources

- [ ] **Step 2: Run the targeted UI test to verify it fails**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nomedia.switcher.ui.albums.AlbumListScreenTest`

Expected: FAIL because the current top bar still shows the title and uses a text button for settings.

- [ ] **Step 3: Implement the lighter header**

```kotlin
SmallTopAppBar(
    title = {},
    actions = {
        IconButton(onClick = onOpenSettings) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_preferences),
                contentDescription = stringResource(R.string.album_list_open_settings),
            )
        }
    },
)
```

Keep the rest of the screen structure unchanged.

- [ ] **Step 4: Re-run the targeted UI test**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nomedia.switcher.ui.albums.AlbumListScreenTest`

Expected: PASS

If the environment cannot execute connected tests, at minimum run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:compileDebugAndroidTestKotlin`

- [ ] **Step 5: Commit the header simplification**

```bash
git add app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/androidTest/java/com/nomedia/switcher/ui/albums/AlbumListScreenTest.kt
git commit -m "feat: simplify album list header"
```

## Task 4: Add Foreground Failure Messages

**Files:**
- Modify: `app/src/main/java/com/nomedia/switcher/ui/UiMessage.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListViewModel.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/AppRoot.kt`
- Modify: `app/src/test/java/com/nomedia/switcher/ui/albums/AlbumListViewModelTest.kt`
- Create: `app/src/test/java/com/nomedia/switcher/ui/UiMessageResolverTest.kt`

- [ ] **Step 1: Write the failing tests for one-shot foreground failures**

```kotlin
@Test
fun completed_failed_toggle_emits_transient_failure_message() = runTest {
    val emittedMessages = mutableListOf<UiMessage>()
    backgroundScope.launch {
        viewModel.transientMessages.toList(emittedMessages)
    }

    viewModel.onToggleClick(row)
    advanceUntilIdle()

    albums.value = listOf(
        album(
            directoryKey = "Pictures/Cyberpunk 2077",
            displayName = "Cyberpunk 2077",
            state = AlbumState.Failed,
            lastAction = ToggleAction.Hide,
            lastFailure = ToggleFailureReason.UnableToCreateNomedia.persistedKey,
        ),
    )
    advanceUntilIdle()

    assertEquals(
        listOf(UiMessage.UnableToCreateNomedia),
        emittedMessages,
    )
}

@Test
fun resolve_context_returns_localized_failure_text() {
    assertEquals(
        context.getString(R.string.album_failure_missing_directory_grant),
        UiMessage.DirectoryGrantMissing.resolve(context),
    )
}
```

- [ ] **Step 2: Run the targeted unit tests to verify they fail**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*AlbumListViewModelTest' --tests '*UiMessageResolverTest'`

Expected: FAIL because there is no transient failure stream and `UiMessage` does not yet expose a non-composable resolver for toast usage.

- [ ] **Step 3: Implement shared UI-message resolution and toast event flow**

```kotlin
fun UiMessage.resolve(context: Context): String = when (this) {
    UiMessage.DirectoryGrantMissing -> context.getString(R.string.album_failure_missing_directory_grant)
    UiMessage.AlbumHidden -> context.getString(R.string.album_status_hidden)
    UiMessage.LastActionFailed -> context.getString(R.string.album_status_last_action_failed)
    UiMessage.HideInProgress -> context.getString(R.string.album_status_hide_in_progress)
    UiMessage.ShowInProgress -> context.getString(R.string.album_status_show_in_progress)
    UiMessage.DirectoryCannotBeGranted -> context.getString(R.string.album_failure_restricted_root)
    UiMessage.DirectoryAccessNotGranted -> context.getString(R.string.album_failure_grant_denied)
    UiMessage.WrongFolderSelected -> context.getString(R.string.album_failure_wrong_directory_selected)
    UiMessage.PersistAccessDenied -> context.getString(R.string.album_failure_persist_permission_denied)
    UiMessage.PreviousTaskInterrupted -> context.getString(R.string.album_failure_interrupted)
    UiMessage.UnableToCreateNomedia -> context.getString(R.string.album_failure_unable_to_create_nomedia)
    UiMessage.UnableToRemoveNomedia -> context.getString(R.string.album_failure_unable_to_remove_nomedia)
    is UiMessage.Raw -> value
}
```

```kotlin
private val transientMessages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
val transientMessages: SharedFlow<UiMessage> = transientMessages
```

Update the view model to emit a transient message when a pending toggle finishes in `Failed`, and update `AppRoot.kt` to collect those events and show `Toast.makeText(...)`. For immediate foreground failures that are still handled directly in `AppRoot.kt`, emit the same localized toast there as well.

Important:

- keep the event contract as collector-first, one-shot delivery
- do not use replayed durable state for these messages
- make the test collect the flow before the failure is emitted

- [ ] **Step 4: Re-run the targeted unit tests**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*AlbumListViewModelTest' --tests '*UiMessageResolverTest'`

Expected: PASS

- [ ] **Step 5: Commit the foreground failure messaging**

```bash
git add app/src/main/java/com/nomedia/switcher/ui/UiMessage.kt app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListViewModel.kt app/src/main/java/com/nomedia/switcher/ui/AppRoot.kt app/src/test/java/com/nomedia/switcher/ui/albums/AlbumListViewModelTest.kt app/src/test/java/com/nomedia/switcher/ui/UiMessageResolverTest.kt
git commit -m "feat: surface foreground toggle failures"
```

## Task 5: Final Verification And Scroll-Performance Review

**Files:**
- Inspect: `app/src/main/java/com/nomedia/switcher/ui/common/AlbumCover.kt`
- Inspect: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListScreen.kt`
- Inspect: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListViewModel.kt`
- Optional modify: whichever file has the clearest low-risk optimization
- Test: whichever focused tests cover the optimization

- [ ] **Step 1: Review the likely scroll hot spots**

Check for:

- oversized or repeated image/video decode work in `AlbumCover.kt`
- avoidable row remapping or redundant IO work in `AlbumListViewModel.kt`
- unnecessary object churn in list rendering code in `AlbumListScreen.kt`

Write down one concrete optimization target only if the benefit is clear and low risk.

- [ ] **Step 2: If a code change is justified, write the failing regression test first**

Examples:

- if fallback cover resolution is repeated unnecessarily, add a view model test proving the resolver is not re-invoked for unchanged live-cover rows
- if cover request sizing is still too loose, add a focused request-spec test for the constrained size

- [ ] **Step 3: Implement the smallest justified optimization**

Examples of acceptable changes:

- avoid re-resolving fallback covers for entries that already have a live cover URI
- remove redundant row copying in list rendering if a stable equivalent is clearer
- tighten request parameters further if tests and code review show an obvious win

Do not bundle speculative refactors into this task.

- [ ] **Step 4: Run full verification**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest`

Expected: PASS

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:compileDebugAndroidTestKotlin`

Expected: PASS

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:assembleDebug`

Expected: PASS

- [ ] **Step 5: Perform the manual real-device verification required by the spec**

Verify on a physical device:

- a `Download` child directory can be authorized and toggled
- at least one known video album now shows a recognizable cover where the previous build showed a placeholder
- the main screen no longer shows the visible title text and still exposes settings from the top-right area
- a failed toggle shows a short in-app transient message
- scrolling feels at least no worse than before, and any low-cost optimization findings are documented

Record the result of each check in the implementation handoff.

- [ ] **Step 6: Commit the optimization only if code changed**

If and only if a real optimization was implemented:

```bash
git add <changed files>
git commit -m "perf: reduce album list scroll overhead"
```

If no code change was justified, skip this commit and report the review findings in the final handoff.
