# Simplified Chinese Localization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Simplified Chinese localization for all user-visible Android app text while preserving English as the default fallback and keeping the app name unchanged.

**Architecture:** Keep English as the default resource set in `res/values/strings.xml`, add a parallel `res/values-zh-rCN/strings.xml`, and remove hardcoded user-facing text from Kotlin sources. Where view models or use cases currently emit display strings, introduce stable presentation message keys or resource IDs so the UI resolves localized text near the presentation boundary instead of the domain layer embedding language-specific copy.

**Tech Stack:** Kotlin, Android resources, Jetpack Compose, AndroidX ViewModel, WorkManager notifications, JUnit4, Robolectric, Compose UI Test, Gradle

---

## Planned File Structure

### Resource Files

- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values-zh-rCN/strings.xml`

These files hold default English and Simplified Chinese user-facing copy. `app_name` remains `NoMedia Switcher`.

### UI Screens

- Modify: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListScreen.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/progress/ToggleProgressSheet.kt`

These files should consume string resources instead of hardcoded literals for screen titles, actions, helper copy, and empty states.

### Presentation-State Mapping

- Modify: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListViewModel.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/progress/ToggleProgressSheet.kt`
- Optional create: `app/src/main/java/com/nomedia/switcher/ui/UiMessage.kt`

This layer should stop treating English display text as state. Replace visible status strings with stable keys or resource IDs that Compose can resolve with `stringResource(...)`.

### Permission And Failure Flows

- Modify: `app/src/main/java/com/nomedia/switcher/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/domain/usecase/ResolveToggleRequestUseCase.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/domain/usecase/RecoverInterruptedAlbumTogglesUseCase.kt`

These files currently originate user-visible failure text. Normalize them into message keys or constants that map to localized resources before UI display or persisted failure presentation.

### Notifications

- Modify: `app/src/main/java/com/nomedia/switcher/notifications/ToggleNotificationFactory.kt`

Notification titles and shared progress labels should stay resource-backed and align with the localized UI text.

### Tests

- Modify: `app/src/test/java/com/nomedia/switcher/ui/albums/AlbumListViewModelTest.kt`
- Modify: `app/src/test/java/com/nomedia/switcher/domain/usecase/ResolveToggleRequestUseCaseTest.kt`
- Modify: `app/src/test/java/com/nomedia/switcher/domain/usecase/RecoverInterruptedAlbumTogglesUseCaseTest.kt`
- Modify: `app/src/test/java/com/nomedia/switcher/notifications/ToggleNotificationFactoryTest.kt`
- Modify: `app/src/androidTest/java/com/nomedia/switcher/ui/albums/AlbumListScreenTest.kt`
- Modify: `app/src/androidTest/java/com/nomedia/switcher/ui/progress/ToggleProgressSheetTest.kt`

These tests should prove message-key mapping, localized resource usage, and that the existing English fallback behavior still compiles cleanly.

## Task 1: Resourceize Existing UI Text

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListScreen.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/progress/ToggleProgressSheet.kt`
- Test: `app/src/androidTest/java/com/nomedia/switcher/ui/albums/AlbumListScreenTest.kt`
- Test: `app/src/androidTest/java/com/nomedia/switcher/ui/progress/ToggleProgressSheetTest.kt`

- [ ] **Step 1: Write failing UI tests for resource-backed labels**

```kotlin
@Test
fun album_list_uses_resource_text_for_static_labels() {
    composeTestRule.setContent {
        AlbumListScreen(
            state = AlbumListUiState(),
            onToggleClick = {},
            onOpenSettings = {},
            highlightedAlbumId = null,
        )
    }

    composeTestRule.onNodeWithText(
        composeTestRule.activity.getString(R.string.album_list_title),
    ).assertIsDisplayed()
}

@Test
fun progress_sheet_uses_resource_text_for_helper_copy() {
    composeTestRule.setContent {
        ToggleProgressSheet(
            state = ToggleProgressSheetState(
                albumId = AlbumId("Pictures/Travel"),
                albumName = "Travel",
                action = ToggleAction.Hide,
                message = ProgressMessage.HideFromLibrary,
            ),
            onHide = {},
        )
    }

    composeTestRule.onNodeWithText(
        composeTestRule.activity.getString(R.string.toggle_progress_helper),
    ).assertIsDisplayed()
}
```

- [ ] **Step 2: Run the targeted instrumentation tests to verify they fail**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nomedia.switcher.ui.albums.AlbumListScreenTest,com.nomedia.switcher.ui.progress.ToggleProgressSheetTest`

Expected: FAIL because these screens still contain hardcoded text and the new resource keys do not exist.

- [ ] **Step 3: Add English and Simplified Chinese string resources plus minimal UI wiring**

```xml
<string name="album_list_title">Albums</string>
<string name="settings_title">Settings</string>
<string name="toggle_progress_helper">Large albums can take a while. You can hide this panel and let the task continue.</string>
```

```xml
<string name="album_list_title">图集</string>
<string name="settings_title">设置</string>
<string name="toggle_progress_helper">大图集可能需要一些时间。你可以隐藏此面板，让任务继续在后台运行。</string>
```

```kotlin
SmallTopAppBar(
    title = { Text(stringResource(R.string.album_list_title)) },
)
```

- [ ] **Step 4: Re-run the targeted instrumentation tests**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nomedia.switcher.ui.albums.AlbumListScreenTest,com.nomedia.switcher.ui.progress.ToggleProgressSheetTest`

Expected: PASS

- [ ] **Step 5: Commit the resourceized static UI text**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListScreen.kt app/src/main/java/com/nomedia/switcher/ui/settings/SettingsScreen.kt app/src/main/java/com/nomedia/switcher/ui/progress/ToggleProgressSheet.kt app/src/androidTest/java/com/nomedia/switcher/ui/albums/AlbumListScreenTest.kt app/src/androidTest/java/com/nomedia/switcher/ui/progress/ToggleProgressSheetTest.kt
git commit -m "feat: localize static album ui strings"
```

## Task 2: Replace ViewModel Display Strings With Message Keys

**Files:**
- Modify: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListViewModel.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumRowState.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/progress/ToggleProgressSheet.kt`
- Optional create: `app/src/main/java/com/nomedia/switcher/ui/UiMessage.kt`
- Test: `app/src/test/java/com/nomedia/switcher/ui/albums/AlbumListViewModelTest.kt`

- [ ] **Step 1: Write failing unit tests for message-key state**

```kotlin
@Test
fun hidden_album_exposes_hidden_status_key_instead_of_raw_text() = runTest {
    val row = viewModel.uiState.value.albums.single()
    assertEquals(UiMessage.AlbumHidden, row.statusMessage)
    assertNull(row.statusText)
}

@Test
fun toggle_click_opens_progress_sheet_with_hide_message_key() = runTest {
    viewModel.onToggleClick(row)
    advanceUntilIdle()

    assertEquals(
        ProgressMessage.HideFromLibrary,
        viewModel.uiState.value.progressSheet?.message,
    )
}
```

- [ ] **Step 2: Run the targeted unit test to verify it fails**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*AlbumListViewModelTest'`

Expected: FAIL because row state and progress sheet still carry raw `String` display text.

- [ ] **Step 3: Implement minimal presentation message types and UI resolution**

```kotlin
sealed interface UiMessage {
    data object AlbumHidden : UiMessage
    data object LastActionFailed : UiMessage
    data object HideInProgress : UiMessage
    data object ShowInProgress : UiMessage
    data class Raw(val value: String) : UiMessage
}
```

```kotlin
data class ToggleProgressSheetState(
    val albumId: AlbumId,
    val albumName: String,
    val action: ToggleAction,
    val message: ProgressMessage,
)
```

```kotlin
Text(text = state.message.resolve())
```

- [ ] **Step 4: Re-run the targeted unit test**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*AlbumListViewModelTest'`

Expected: PASS

- [ ] **Step 5: Commit the presentation-message refactor**

```bash
git add app/src/main/java/com/nomedia/switcher/ui app/src/test/java/com/nomedia/switcher/ui/albums/AlbumListViewModelTest.kt
git commit -m "refactor: localize album presentation messages"
```

## Task 3: Localize Failure And Recovery Flows

**Files:**
- Modify: `app/src/main/java/com/nomedia/switcher/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/domain/usecase/ResolveToggleRequestUseCase.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/domain/usecase/RecoverInterruptedAlbumTogglesUseCase.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Test: `app/src/test/java/com/nomedia/switcher/domain/usecase/ResolveToggleRequestUseCaseTest.kt`
- Test: `app/src/test/java/com/nomedia/switcher/domain/usecase/RecoverInterruptedAlbumTogglesUseCaseTest.kt`
- Modify: `app/src/test/java/com/nomedia/switcher/ui/albums/AlbumListViewModelTest.kt`

- [ ] **Step 1: Write failing tests for localized failure reasons**

```kotlin
@Test
fun restricted_root_returns_blocked_reason_key() = runTest {
    val resolution = useCase("Pictures", "Pictures", ToggleAction.Hide)
    assertEquals(ToggleFailureReason.RestrictedRoot, (resolution as ToggleRequestResolution.Blocked).reason)
}

@Test
fun interrupted_recovery_uses_interrupted_reason_key() = runTest {
    useCase()
    assertEquals(ToggleFailureReason.Interrupted, capturedUpdate.lastFailure)
}
```

- [ ] **Step 2: Run the targeted unit tests to verify they fail**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*ResolveToggleRequestUseCaseTest' --tests '*RecoverInterruptedAlbumTogglesUseCaseTest' --tests '*AlbumListViewModelTest'`

Expected: FAIL because these flows still persist raw English strings.

- [ ] **Step 3: Introduce stable failure-reason keys and map them at the UI boundary**

```kotlin
enum class ToggleFailureReason {
    RestrictedRoot,
    GrantDenied,
    WrongDirectorySelected,
    PersistPermissionDenied,
    Interrupted,
}
```

```kotlin
data class Blocked(
    val directoryKey: String,
    val albumName: String,
    val action: ToggleAction,
    val reason: ToggleFailureReason,
) : ToggleRequestResolution
```

```kotlin
val localizedFailure = reason.toUiMessage()
recordToggleFailure(..., reason = localizedFailure)
```

- [ ] **Step 4: Re-run the targeted unit tests**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*ResolveToggleRequestUseCaseTest' --tests '*RecoverInterruptedAlbumTogglesUseCaseTest' --tests '*AlbumListViewModelTest'`

Expected: PASS

- [ ] **Step 5: Commit the localized failure-flow changes**

```bash
git add app/src/main/java/com/nomedia/switcher/ui/AppRoot.kt app/src/main/java/com/nomedia/switcher/domain/usecase/ResolveToggleRequestUseCase.kt app/src/main/java/com/nomedia/switcher/domain/usecase/RecoverInterruptedAlbumTogglesUseCase.kt app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/test/java/com/nomedia/switcher/domain/usecase/ResolveToggleRequestUseCaseTest.kt app/src/test/java/com/nomedia/switcher/domain/usecase/RecoverInterruptedAlbumTogglesUseCaseTest.kt app/src/test/java/com/nomedia/switcher/ui/albums/AlbumListViewModelTest.kt
git commit -m "feat: localize toggle failure flows"
```

## Task 4: Verify Notifications And Final Build

**Files:**
- Modify: `app/src/main/java/com/nomedia/switcher/notifications/ToggleNotificationFactory.kt`
- Modify: `app/src/test/java/com/nomedia/switcher/notifications/ToggleNotificationFactoryTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`

- [ ] **Step 1: Write the failing notification test for localized strings**

```kotlin
@Test
fun progress_info_uses_localized_resource_title() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val factory = ToggleNotificationFactory(context)

    val info = factory.buildProgressInfo(albumName = "Camera", action = ToggleAction.Hide)

    assertEquals(
        context.getString(R.string.toggle_hide_progress_title, "Camera"),
        Shadows.shadowOf(info.notification).contentTitle.toString(),
    )
}
```

- [ ] **Step 2: Run the targeted notification unit test to verify it fails if localization wiring is incomplete**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*ToggleNotificationFactoryTest'`

Expected: FAIL if the notification still depends on outdated or incomplete string keys after the refactor.

- [ ] **Step 3: Align notification copy with the new resource set and add any missing shared keys**

```xml
<string name="toggle_hide_progress_title">Hiding %1$s</string>
<string name="toggle_hide_progress_title">正在隐藏 %1$s</string>
```

Keep `ToggleNotificationFactory` resource-backed and ensure no user-visible notification copy stays hardcoded in Kotlin.

- [ ] **Step 4: Run full serial verification**

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest`
Expected: PASS

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:compileDebugAndroidTestKotlin`
Expected: PASS

Run: `env GRADLE_USER_HOME=.gradle ./gradlew :app:assembleDebug`
Expected: PASS

Do not run these Gradle commands in parallel against the same build directory.

Manual check: open both `app/src/main/res/values/strings.xml` and `app/src/main/res/values-zh-rCN/strings.xml` and confirm `app_name` remains `NoMedia Switcher`.

- [ ] **Step 5: Commit the final localization verification pass**

```bash
git add app/src/main/java/com/nomedia/switcher/notifications/ToggleNotificationFactory.kt app/src/test/java/com/nomedia/switcher/notifications/ToggleNotificationFactoryTest.kt app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml
git commit -m "feat: add simplified chinese localization"
```
