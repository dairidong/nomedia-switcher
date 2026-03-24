# NoMedia Switcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android 11+ app that lists switchable photo albums, toggles `.nomedia` per album with strong progress feedback, and preserves previously hidden albums through local state.

**Architecture:** Use a single Android app module with Kotlin + Compose, a small service-locator style app container, Room for album state, DataStore for settings, MediaStore for album discovery, and WorkManager for long-running toggle work. Album discovery remains read-only; write access is granted per directory through `ACTION_OPEN_DOCUMENT_TREE` and persisted URI permissions, then `.nomedia` is created or deleted with `DocumentsContract`/`DocumentFile` and a media refresh pass.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX Activity/ViewModel/Navigation, Room, DataStore, WorkManager, DocumentFile, MediaStore, JUnit4, Turbine, MockK, Robolectric, Compose UI Test, AndroidX Test

---

## Planned File Structure

### Root Build Files

- `settings.gradle.kts`: declare plugin management, repositories, and `:app`
- `build.gradle.kts`: root plugin versions and shared repositories
- `gradle.properties`: JVM, AndroidX, Compose, Kotlin flags
- `gradle/libs.versions.toml`: central dependency versions
- `app/build.gradle.kts`: app plugins, Android config, dependencies, test setup, WorkManager manifest placeholders
- `app/proguard-rules.pro`: keep rules for release builds
- `app/src/main/AndroidManifest.xml`: permissions, activity, notification and worker setup

### Application Shell

- `app/src/main/java/com/nomedia/switcher/NoMediaApplication.kt`: create app container and WorkManager configuration
- `app/src/main/java/com/nomedia/switcher/MainActivity.kt`: Compose entry point and permission bootstrap
- `app/src/main/java/com/nomedia/switcher/AppContainer.kt`: dependency graph shared across app and worker factory
- `app/src/main/java/com/nomedia/switcher/ui/AppRoot.kt`: top-level navigation and state host
- `app/src/main/java/com/nomedia/switcher/ui/theme/*`: theme, color, typography

### Domain and Persistence

- `app/src/main/java/com/nomedia/switcher/domain/model/AlbumId.kt`
- `app/src/main/java/com/nomedia/switcher/domain/model/AlbumState.kt`
- `app/src/main/java/com/nomedia/switcher/domain/model/AlbumEntry.kt`
- `app/src/main/java/com/nomedia/switcher/domain/model/ToggleAction.kt`
- `app/src/main/java/com/nomedia/switcher/domain/model/ToggleResult.kt`
- `app/src/main/java/com/nomedia/switcher/data/local/AppDatabase.kt`
- `app/src/main/java/com/nomedia/switcher/data/local/album/AlbumRecordEntity.kt`
- `app/src/main/java/com/nomedia/switcher/data/local/album/AlbumRecordDao.kt`
- `app/src/main/java/com/nomedia/switcher/data/local/settings/UserSettingsSerializer.kt`
- `app/src/main/java/com/nomedia/switcher/data/local/settings/UserSettingsRepository.kt`

### Discovery, Merge, and Access

- `app/src/main/java/com/nomedia/switcher/data/media/MediaStoreAlbumRow.kt`: raw MediaStore projection model
- `app/src/main/java/com/nomedia/switcher/data/media/MediaStoreAlbumScanner.kt`: query and group eligible albums
- `app/src/main/java/com/nomedia/switcher/domain/usecase/ObserveAlbumsUseCase.kt`: merge local records with live scan results
- `app/src/main/java/com/nomedia/switcher/domain/usecase/RefreshAlbumsUseCase.kt`
- `app/src/main/java/com/nomedia/switcher/domain/usecase/SetHiddenAlbumsPinnedUseCase.kt`
- `app/src/main/java/com/nomedia/switcher/data/access/DirectoryGrantEntity.kt`
- `app/src/main/java/com/nomedia/switcher/data/access/DirectoryGrantDao.kt`
- `app/src/main/java/com/nomedia/switcher/data/access/DirectoryGrantRepository.kt`
- `app/src/main/java/com/nomedia/switcher/ui/access/DirectoryGrantLauncher.kt`

### Toggle Execution

- `app/src/main/java/com/nomedia/switcher/data/toggle/NomediaDocumentGateway.kt`
- `app/src/main/java/com/nomedia/switcher/data/toggle/MediaRefreshCoordinator.kt`
- `app/src/main/java/com/nomedia/switcher/domain/usecase/EnqueueToggleAlbumUseCase.kt`
- `app/src/main/java/com/nomedia/switcher/worker/ToggleAlbumWorker.kt`
- `app/src/main/java/com/nomedia/switcher/worker/ToggleWorkerFactory.kt`
- `app/src/main/java/com/nomedia/switcher/notifications/ToggleNotificationFactory.kt`

### UI

- `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListViewModel.kt`
- `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListScreen.kt`
- `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumRowState.kt`
- `app/src/main/java/com/nomedia/switcher/ui/progress/ToggleProgressSheet.kt`
- `app/src/main/java/com/nomedia/switcher/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/nomedia/switcher/ui/common/StatusChip.kt`

### Tests

- `app/src/test/java/com/nomedia/switcher/data/local/AlbumRecordDaoTest.kt`
- `app/src/test/java/com/nomedia/switcher/data/media/MediaStoreAlbumScannerTest.kt`
- `app/src/test/java/com/nomedia/switcher/domain/usecase/ObserveAlbumsUseCaseTest.kt`
- `app/src/test/java/com/nomedia/switcher/data/access/DirectoryGrantRepositoryTest.kt`
- `app/src/test/java/com/nomedia/switcher/data/toggle/NomediaDocumentGatewayTest.kt`
- `app/src/test/java/com/nomedia/switcher/worker/ToggleAlbumWorkerTest.kt`
- `app/src/test/java/com/nomedia/switcher/ui/albums/AlbumListViewModelTest.kt`
- `app/src/androidTest/java/com/nomedia/switcher/AppLaunchTest.kt`
- `app/src/androidTest/java/com/nomedia/switcher/ui/albums/AlbumListScreenTest.kt`
- `app/src/androidTest/java/com/nomedia/switcher/ui/access/DirectoryGrantFlowTest.kt`

### Implementation Decisions To Lock In

- Read media using `READ_EXTERNAL_STORAGE` on Android 11-12 and `READ_MEDIA_IMAGES` on Android 13+.
- Discover candidate albums from `MediaStore.Images.Media` using `BUCKET_ID`, `BUCKET_DISPLAY_NAME`, and `RELATIVE_PATH`, then reject any bucket whose sampled rows resolve to more than one normalized directory.
- Request write access only when the user first toggles an album, using `ACTION_OPEN_DOCUMENT_TREE` for that album directory and persisting the URI permission.
- Exclude albums under SAF-restricted roots the user cannot grant, and treat missing or stale grants as actionable failures that prompt re-grant.
- Run toggle work through a long-running `CoroutineWorker` with foreground info so the task survives UI dismissal while remaining user-visible.
- Declare `POST_NOTIFICATIONS` (API 33+), `FOREGROUND_SERVICE`, and the matching foreground service type permissions needed by the long-running worker in the manifest.
- Refresh media by scanning the affected `.nomedia` path and then re-querying the affected bucket/state; do not promise immediate third-party gallery refresh beyond the filesystem operation.
- Deep-link failure notifications back into the app with the album highlighted; treat inline notification retry as optional follow-up, not v1-critical.

### Task 1: Bootstrap The Android Project And Test Harness

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/nomedia/switcher/NoMediaApplication.kt`
- Create: `app/src/main/java/com/nomedia/switcher/MainActivity.kt`
- Create: `app/src/main/java/com/nomedia/switcher/ui/AppRoot.kt`
- Create: `app/src/main/java/com/nomedia/switcher/ui/theme/Color.kt`
- Create: `app/src/main/java/com/nomedia/switcher/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/nomedia/switcher/ui/theme/Type.kt`
- Test: `app/src/androidTest/java/com/nomedia/switcher/AppLaunchTest.kt`

- [ ] **Step 1: Create the Gradle and Android app skeleton**

```kotlin
android {
    namespace = "com.nomedia.switcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nomedia.switcher"
        minSdk = 30
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }
}
```

- [ ] **Step 2: Write the failing app launch test**

```kotlin
@Test
fun launch_showsAlbumScreenTitle() {
    composeTestRule.onNodeWithText("Albums").assertIsDisplayed()
}
```

- [ ] **Step 3: Run the instrumentation test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nomedia.switcher.AppLaunchTest`
Expected: FAIL because `Albums` is not rendered yet.

- [ ] **Step 4: Implement the minimal app shell**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NoMediaTheme { AppRoot() } }
    }
}
```

- [ ] **Step 5: Run the test and assemble the debug app**

Run: `./gradlew :app:connectedDebugAndroidTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit the scaffold**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml app
git commit -m "chore: scaffold Android Compose app"
```

### Task 2: Add Local Persistence For Album State And Settings

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/nomedia/switcher/domain/model/AlbumId.kt`
- Create: `app/src/main/java/com/nomedia/switcher/domain/model/AlbumState.kt`
- Create: `app/src/main/java/com/nomedia/switcher/domain/model/ToggleAction.kt`
- Create: `app/src/main/java/com/nomedia/switcher/domain/model/AlbumEntry.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/local/AppDatabase.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/local/album/AlbumRecordEntity.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/local/album/AlbumRecordDao.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/local/settings/UserSettings.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/local/settings/UserSettingsSerializer.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/local/settings/UserSettingsRepository.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/local/AlbumRecordDaoTest.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/local/settings/UserSettingsRepositoryTest.kt`

- [ ] **Step 1: Write failing persistence tests**

```kotlin
@Test
fun hidden_album_record_roundTrips() = runTest {
    dao.upsert(AlbumRecordEntity(directoryKey = "DCIM/Camera", state = Hidden))
    assertThat(dao.observeAll().first().single().state).isEqualTo(Hidden)
}

@Test
fun settings_default_pinsHiddenAlbums() = runTest {
    assertThat(repository.settings.first().pinHiddenAlbumsToTop).isTrue()
}
```

- [ ] **Step 2: Run the unit tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*AlbumRecordDaoTest' --tests '*UserSettingsRepositoryTest'`
Expected: FAIL because Room entities/DAO and settings repository do not exist yet.

- [ ] **Step 3: Implement the models, Room schema, and DataStore settings**

```kotlin
@Entity(tableName = "album_records")
data class AlbumRecordEntity(
    @PrimaryKey val directoryKey: String,
    val displayName: String,
    val state: String,
    val treeUri: String?,
    val lastFailure: String?,
    val seenInLastScan: Boolean,
    val updatedAtEpochMs: Long,
)
```

- [ ] **Step 4: Run the persistence tests**

Run: `./gradlew :app:testDebugUnitTest --tests '*AlbumRecordDaoTest' --tests '*UserSettingsRepositoryTest'`
Expected: PASS

- [ ] **Step 5: Run a broader regression pass for local data code**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.nomedia.switcher.data.local.*'`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit the persistence layer**

```bash
git add app/build.gradle.kts app/src/main/java/com/nomedia/switcher/domain app/src/main/java/com/nomedia/switcher/data/local app/src/test/java/com/nomedia/switcher/data/local
git commit -m "feat: add album state persistence"
```

### Task 3: Implement MediaStore Album Discovery And Eligibility Rules

**Files:**
- Create: `app/src/main/java/com/nomedia/switcher/data/media/MediaStoreAlbumRow.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/media/MediaStoreAlbumScanner.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/media/DirectoryNormalizer.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/media/AlbumCandidate.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/media/MediaStoreAlbumScannerTest.kt`

- [ ] **Step 1: Write failing scanner tests for grouping and rejection rules**

```kotlin
@Test
fun bucket_with_single_directory_isEligible() {
    val rows = listOf(
        row(bucketId = "1", bucketName = "Camera", relativePath = "DCIM/Camera/"),
        row(bucketId = "1", bucketName = "Camera", relativePath = "DCIM/Camera/")
    )
    assertThat(scanner.fromRows(rows)).containsExactly(candidate("DCIM/Camera"))
}

@Test
fun bucket_with_multiple_directories_isRejected() {
    val rows = listOf(
        row(bucketId = "2", bucketName = "Travel", relativePath = "Pictures/TripA/"),
        row(bucketId = "2", bucketName = "Travel", relativePath = "Pictures/TripB/")
    )
    assertThat(scanner.fromRows(rows)).isEmpty()
}
```

- [ ] **Step 2: Run the scanner tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*MediaStoreAlbumScannerTest'`
Expected: FAIL because the scanner and normalizer are not implemented.

- [ ] **Step 3: Implement the MediaStore projection and eligibility heuristic**

```kotlin
private val projection = arrayOf(
    MediaStore.Images.Media._ID,
    MediaStore.Images.Media.BUCKET_ID,
    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
    MediaStore.Images.Media.RELATIVE_PATH,
    MediaStore.MediaColumns.VOLUME_NAME,
)
```

- [ ] **Step 4: Run the scanner tests**

Run: `./gradlew :app:testDebugUnitTest --tests '*MediaStoreAlbumScannerTest'`
Expected: PASS

- [ ] **Step 5: Add a regression case for missing or null paths**

```kotlin
@Test
fun row_without_resolvable_directory_isIgnored() {
    assertThat(scanner.fromRows(listOf(row(relativePath = null, dataPath = null)))).isEmpty()
}
```

Run: `./gradlew :app:testDebugUnitTest --tests '*MediaStoreAlbumScannerTest'`
Expected: PASS

- [ ] **Step 6: Commit the scanner logic**

```bash
git add app/src/main/java/com/nomedia/switcher/data/media app/src/test/java/com/nomedia/switcher/data/media
git commit -m "feat: scan switchable albums from MediaStore"
```

### Task 4: Implement Startup Merge, Sorting, And List State Assembly

**Files:**
- Create: `app/src/main/java/com/nomedia/switcher/domain/usecase/ObserveAlbumsUseCase.kt`
- Create: `app/src/main/java/com/nomedia/switcher/domain/usecase/RefreshAlbumsUseCase.kt`
- Create: `app/src/main/java/com/nomedia/switcher/domain/usecase/SetHiddenAlbumsPinnedUseCase.kt`
- Create: `app/src/main/java/com/nomedia/switcher/domain/repository/AlbumRepository.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/AlbumRepositoryImpl.kt`
- Test: `app/src/test/java/com/nomedia/switcher/domain/usecase/ObserveAlbumsUseCaseTest.kt`

- [ ] **Step 1: Write failing merge and sorting tests**

```kotlin
@Test
fun local_hidden_album_is_visible_before_scan_returns() = runTest {
    val records = listOf(localAlbum(state = HiddenMissingFromScan, displayName = "Secret"))
    val scan = emptyList<AlbumCandidate>()
    assertThat(useCase.merge(records, scan, pinHidden = true).first().displayName).isEqualTo("Secret")
}

@Test
fun processing_album_is_sorted_first() = runTest {
    val merged = useCase.merge(records = listOf(localAlbum(state = Processing), localAlbum(state = Shown)), scan = emptyList(), pinHidden = true)
    assertThat(merged.map { it.state }).startsWith(Processing)
}
```

- [ ] **Step 2: Run the use case tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*ObserveAlbumsUseCaseTest'`
Expected: FAIL because the merge and sorting logic does not exist.

- [ ] **Step 3: Implement repository merge behavior and sorting policy**

```kotlin
private fun stateRank(state: AlbumState, pinHidden: Boolean): Int = when (state) {
    Processing -> 0
    Hidden, HiddenMissingFromScan -> if (pinHidden) 1 else 3
    Failed -> 2
    Shown -> 3
}
```

- [ ] **Step 4: Run the use case tests**

Run: `./gradlew :app:testDebugUnitTest --tests '*ObserveAlbumsUseCaseTest'`
Expected: PASS

- [ ] **Step 5: Add a regression test for hidden-pin disabled**

```kotlin
@Test
fun hidden_albums_do_not_jump_when_pin_setting_disabled() = runTest {
    val merged = useCase.merge(records, scan, pinHidden = false)
    assertThat(merged.first().state).isNotEqualTo(Hidden)
}
```

Run: `./gradlew :app:testDebugUnitTest --tests '*ObserveAlbumsUseCaseTest'`
Expected: PASS

- [ ] **Step 6: Commit the merge layer**

```bash
git add app/src/main/java/com/nomedia/switcher/domain/usecase app/src/main/java/com/nomedia/switcher/domain/repository app/src/main/java/com/nomedia/switcher/data/AlbumRepositoryImpl.kt app/src/test/java/com/nomedia/switcher/domain/usecase
git commit -m "feat: merge local and scanned album state"
```

### Task 5: Add Per-Album Directory Grant Capture And Persistence

**Files:**
- Create: `app/src/main/java/com/nomedia/switcher/data/access/DirectoryGrantEntity.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/access/DirectoryGrantDao.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/access/DirectoryGrantRepository.kt`
- Create: `app/src/main/java/com/nomedia/switcher/ui/access/DirectoryGrantLauncher.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/data/local/AppDatabase.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/access/DirectoryGrantRepositoryTest.kt`
- Test: `app/src/androidTest/java/com/nomedia/switcher/ui/access/DirectoryGrantFlowTest.kt`

- [ ] **Step 1: Write failing tests for missing, stale, and persisted grants**

```kotlin
@Test
fun matching_tree_uri_isReturnedForKnownDirectory() = runTest {
    repository.saveGrant(directoryKey = "DCIM/Camera", treeUri = "content://tree/primary%3ADCIM%2FCamera")
    assertThat(repository.findGrant("DCIM/Camera")).isNotNull()
}

@Test
fun missing_grant_requests_user_action() = runTest {
    assertThat(repository.findGrant("Pictures/Secret")).isNull()
}
```

- [ ] **Step 2: Run the grant tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*DirectoryGrantRepositoryTest' :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nomedia.switcher.ui.access.DirectoryGrantFlowTest`
Expected: FAIL because grant persistence and launcher flow do not exist.

- [ ] **Step 3: Implement persisted tree grants and launcher contract**

```kotlin
fun createIntent(initialUri: Uri) = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
    putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
}
```

- [ ] **Step 4: Run the grant tests**

Run: `./gradlew :app:testDebugUnitTest --tests '*DirectoryGrantRepositoryTest' :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nomedia.switcher.ui.access.DirectoryGrantFlowTest`
Expected: PASS

- [ ] **Step 5: Add explicit handling for non-grantable or restricted directories**

```kotlin
@Test
fun restricted_root_returns_ungrantable_error() = runTest {
    assertThat(repository.validateGrantRequest("Download")).isEqualTo(GrantError.RestrictedRoot)
}
```

Run: `./gradlew :app:testDebugUnitTest --tests '*DirectoryGrantRepositoryTest'`
Expected: PASS

- [ ] **Step 6: Commit the grant flow**

```bash
git add app/src/main/java/com/nomedia/switcher/data/access app/src/main/java/com/nomedia/switcher/ui/access app/src/main/java/com/nomedia/switcher/data/local/AppDatabase.kt app/src/test/java/com/nomedia/switcher/data/access app/src/androidTest/java/com/nomedia/switcher/ui/access
git commit -m "feat: persist directory grants for album toggles"
```

### Task 6: Implement `.nomedia` Toggle Execution And Background Work

**Files:**
- Create: `app/src/main/java/com/nomedia/switcher/data/toggle/NomediaDocumentGateway.kt`
- Create: `app/src/main/java/com/nomedia/switcher/data/toggle/MediaRefreshCoordinator.kt`
- Create: `app/src/main/java/com/nomedia/switcher/domain/model/ToggleResult.kt`
- Create: `app/src/main/java/com/nomedia/switcher/domain/usecase/EnqueueToggleAlbumUseCase.kt`
- Create: `app/src/main/java/com/nomedia/switcher/worker/ToggleAlbumWorker.kt`
- Create: `app/src/main/java/com/nomedia/switcher/worker/ToggleWorkerFactory.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/NoMediaApplication.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/AppContainer.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/toggle/NomediaDocumentGatewayTest.kt`
- Test: `app/src/test/java/com/nomedia/switcher/worker/ToggleAlbumWorkerTest.kt`

- [ ] **Step 1: Write failing unit tests for create, delete, and worker status propagation**

```kotlin
@Test
fun hide_creates_nomedia_file_when_missing() = runTest {
    gateway.hide(treeUri, directoryKey = "DCIM/Camera")
    assertThat(fakeDirectory.createdFiles).contains(".nomedia")
}

@Test
fun worker_marks_failure_when_grant_missing() = runTest {
    val result = worker.doWork()
    assertThat(result).isEqualTo(ListenableWorker.Result.failure())
}
```

- [ ] **Step 2: Run the worker and gateway tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*NomediaDocumentGatewayTest' --tests '*ToggleAlbumWorkerTest'`
Expected: FAIL because the gateway and worker are not implemented.

- [ ] **Step 3: Implement `.nomedia` file operations and foreground worker execution**

```kotlin
override suspend fun doWork(): Result {
    setForeground(notificationFactory.progressInfo(albumName, action))
    return when (toggleExecutor.execute(input)) {
        is ToggleResult.Success -> Result.success()
        is ToggleResult.RetryableFailure -> Result.retry()
        is ToggleResult.PermanentFailure -> Result.failure()
    }
}
```

- [ ] **Step 4: Run the worker and gateway tests**

Run: `./gradlew :app:testDebugUnitTest --tests '*NomediaDocumentGatewayTest' --tests '*ToggleAlbumWorkerTest'`
Expected: PASS

- [ ] **Step 5: Verify the worker registration and foreground notification wiring**

Run: `./gradlew :app:testDebugUnitTest --tests '*ToggleAlbumWorkerTest' :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit the execution layer**

```bash
git add app/src/main/java/com/nomedia/switcher/data/toggle app/src/main/java/com/nomedia/switcher/domain/model/ToggleResult.kt app/src/main/java/com/nomedia/switcher/domain/usecase/EnqueueToggleAlbumUseCase.kt app/src/main/java/com/nomedia/switcher/worker app/src/main/java/com/nomedia/switcher/NoMediaApplication.kt app/src/main/java/com/nomedia/switcher/AppContainer.kt app/src/test/java/com/nomedia/switcher/data/toggle app/src/test/java/com/nomedia/switcher/worker
git commit -m "feat: execute nomedia toggles in background"
```

### Task 7: Build The Album List, Progress Sheet, And Settings UI

**Files:**
- Create: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumRowState.kt`
- Create: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListViewModel.kt`
- Create: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListScreen.kt`
- Create: `app/src/main/java/com/nomedia/switcher/ui/progress/ToggleProgressSheet.kt`
- Create: `app/src/main/java/com/nomedia/switcher/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/nomedia/switcher/ui/common/StatusChip.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/AppRoot.kt`
- Test: `app/src/test/java/com/nomedia/switcher/ui/albums/AlbumListViewModelTest.kt`
- Test: `app/src/androidTest/java/com/nomedia/switcher/ui/albums/AlbumListScreenTest.kt`

- [ ] **Step 1: Write failing ViewModel and Compose UI tests**

```kotlin
@Test
fun toggle_click_sets_processing_and_opens_sheet() = runTest {
    viewModel.onToggleClick(album)
    assertThat(awaitItem().progressSheet?.albumName).isEqualTo(album.displayName)
}

@Test
fun hidden_missing_album_shows_recoverable_status() {
    composeTestRule.onNodeWithText("Hidden, not currently in media library").assertIsDisplayed()
}
```

- [ ] **Step 2: Run the ViewModel and UI tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*AlbumListViewModelTest' :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nomedia.switcher.ui.albums.AlbumListScreenTest`
Expected: FAIL because the UI state and screen components do not exist.

- [ ] **Step 3: Implement the album list, dismissible sheet, and settings screen**

```kotlin
if (state.progressSheet != null) {
    ModalBottomSheet(onDismissRequest = viewModel::dismissProgressSheet) {
        ToggleProgressSheet(state.progressSheet)
    }
}
```

- [ ] **Step 4: Run the ViewModel and UI tests**

Run: `./gradlew :app:testDebugUnitTest --tests '*AlbumListViewModelTest' :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nomedia.switcher.ui.albums.AlbumListScreenTest`
Expected: PASS

- [ ] **Step 5: Add a regression test for the hidden-pin setting**

```kotlin
@Test
fun settings_toggle_updates_sorting_preference() = runTest {
    viewModel.onPinHiddenChanged(false)
    assertThat(settingsRepository.settings.first().pinHiddenAlbumsToTop).isFalse()
}
```

Run: `./gradlew :app:testDebugUnitTest --tests '*AlbumListViewModelTest'`
Expected: PASS

- [ ] **Step 6: Commit the main UI**

```bash
git add app/src/main/java/com/nomedia/switcher/ui app/src/test/java/com/nomedia/switcher/ui app/src/androidTest/java/com/nomedia/switcher/ui/albums
git commit -m "feat: add album management UI"
```

### Task 8: Add Completion Notifications, Deep Links, And End-To-End Verification

**Files:**
- Create: `app/src/main/java/com/nomedia/switcher/notifications/ToggleNotificationFactory.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/nomedia/switcher/MainActivity.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/AppRoot.kt`
- Test: `app/src/androidTest/java/com/nomedia/switcher/AppLaunchTest.kt`
- Test: `app/src/androidTest/java/com/nomedia/switcher/ui/access/DirectoryGrantFlowTest.kt`
- Test: `app/src/androidTest/java/com/nomedia/switcher/ui/albums/AlbumListScreenTest.kt`
- Create: `README.md`

- [ ] **Step 1: Write failing end-to-end tests for background completion handling**

```kotlin
@Test
fun background_completion_reopens_app_with_album_highlighted() {
    // Launch deep link intent carrying album id and expect highlighted row.
}

@Test
fun denied_notification_permission_does_not_block_inAppStateUpdate() {
    // Simulate no POST_NOTIFICATIONS grant on API 33+.
}
```

- [ ] **Step 2: Run the end-to-end tests to verify they fail**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nomedia.switcher.AppLaunchTest,com.nomedia.switcher.ui.albums.AlbumListScreenTest,com.nomedia.switcher.ui.access.DirectoryGrantFlowTest`
Expected: FAIL because notification deep links and completion routing are not implemented.

- [ ] **Step 3: Implement notification channels, deep links, and README setup notes**

```kotlin
val contentIntent = PendingIntent.getActivity(
    context,
    albumId.raw.hashCode(),
    Intent(context, MainActivity::class.java).apply {
        action = "com.nomedia.switcher.OPEN_ALBUM_RESULT"
        putExtra("album_id", albumId.raw)
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)
```

- [ ] **Step 4: Run the instrumentation suite and unit tests**

Run: `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Run a final release-oriented verification**

Run: `./gradlew :app:assembleRelease :app:lintDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit the notification and verification work**

```bash
git add app/src/main/java/com/nomedia/switcher/notifications app/src/main/AndroidManifest.xml app/src/main/java/com/nomedia/switcher/MainActivity.kt app/src/main/java/com/nomedia/switcher/ui/AppRoot.kt app/src/androidTest/java/com/nomedia/switcher README.md
git commit -m "feat: finish toggle notifications and verification"
```

## Verification Checklist

- Run `./gradlew :app:testDebugUnitTest`
- Run `./gradlew :app:connectedDebugAndroidTest`
- Run `./gradlew :app:assembleRelease :app:lintDebug`
- Manually verify on an Android 13+ device:
  - First launch requests media permission only when needed.
  - A previously hidden album appears before the scan completes.
  - Tapping toggle on an ungranted album prompts directory grant.
  - Dismissing the progress sheet does not cancel work.
  - Success and failure notifications appear after background completion.
  - Turning off `Pin hidden albums to top` reorders the list immediately.

## Notes For Implementation

- Keep v1 single-module; do not introduce Hilt or multi-module decomposition unless real pain appears.
- Prefer fakes over mocks for scanner, grant, and worker tests where possible.
- Normalize directory keys once in one helper and reuse that helper everywhere; do not duplicate path cleanup logic.
- Treat `HiddenMissingFromScan` as a first-class state across DB, use cases, UI, and worker outcomes.
- If directory grants prove too fragile for some roots during implementation, fail closed: remove those albums from the switchable list or mark them as unsupported rather than adding broad-storage hacks.
