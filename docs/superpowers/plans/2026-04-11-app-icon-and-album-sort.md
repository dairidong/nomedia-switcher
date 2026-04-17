# App Icon And Album Sort Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the launcher icon with assets derived from `icon_concept.svg` and add a persisted album sort mode that can switch between name order and latest-media-time order without breaking hidden-album pinning.

**Architecture:** Treat `icon_concept.svg` as the design source for Android adaptive launcher icon resources. Extend the existing settings and media scan pipeline so sort mode is persisted in DataStore, latest media time is captured during scan and persisted for hidden albums, and album merge ordering applies sorting only inside the existing state-priority groups.

**Tech Stack:** Android resources, AndroidManifest launcher icon config, Kotlin, Compose, DataStore, Room, MediaStore

---

### Task 1: Add Sort Mode To User Settings

**Files:**
- Modify: `app/src/main/java/com/nomedia/switcher/data/local/settings/UserSettings.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/data/local/settings/UserSettingsSerializer.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/data/local/settings/UserSettingsRepository.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/local/settings/UserSettingsRepositoryTest.kt`

- [ ] **Step 1: Write the failing settings tests**

Add tests covering:
- default sort mode is `ByName`
- updating sort mode persists the new value
- serializer round-trips both `pinHiddenAlbumsToTop` and sort mode

- [ ] **Step 2: Run the settings tests to verify failure**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*UserSettingsRepositoryTest'
```
Expected: FAIL because sort mode does not exist yet

- [ ] **Step 3: Add the sort mode model**

Implement:
- an enum such as `AlbumSortMode`
- `UserSettings.albumSortMode`
- repository setter for sort mode

- [ ] **Step 4: Update serializer compatibility**

Extend serializer format so it can read and write both:
- existing pinned-hidden setting
- new sort mode

Keep backward compatibility with existing single-setting payloads where practical.

- [ ] **Step 5: Re-run the settings tests**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*UserSettingsRepositoryTest'
```
Expected: PASS

### Task 2: Capture And Persist Latest Media Time

**Files:**
- Modify: `app/src/main/java/com/nomedia/switcher/data/media/AlbumCandidate.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/data/media/MediaStoreAlbumLoader.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/data/media/MediaStoreAlbumScanner.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/data/media/MediaStoreAlbumRow.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/data/local/album/AlbumRecordEntity.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/domain/usecase/PersistScannedAlbumCoverReferencesUseCase.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/media/MediaStoreAlbumScannerTest.kt`
- Test: `app/src/test/java/com/nomedia/switcher/domain/usecase/PersistScannedAlbumCoverReferencesUseCaseTest.kt`

- [ ] **Step 1: Write failing scan/persist tests**

Cover:
- scan output includes latest media time for each album
- persisted album record keeps last-known media time
- hidden/missing albums retain this fallback time after scan persistence

- [ ] **Step 2: Run the targeted tests to verify failure**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*MediaStoreAlbumScannerTest' --tests '*PersistScannedAlbumCoverReferencesUseCaseTest'
```
Expected: FAIL because latest media time is not modeled yet

- [ ] **Step 3: Add scan-time timestamp capture**

Extend the scan pipeline so the newest media row per album also carries:
- latest media timestamp in epoch milliseconds or a comparable sortable long

Do not add filesystem directory timestamp reads.

- [ ] **Step 4: Persist last-known album media time**

Add local database columns and persistence logic so hidden albums can keep participating in time sort after disappearing from the live scan.

- [ ] **Step 5: Re-run the targeted tests**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*MediaStoreAlbumScannerTest' --tests '*PersistScannedAlbumCoverReferencesUseCaseTest'
```
Expected: PASS

### Task 3: Apply Group-Preserving Sort Logic

**Files:**
- Modify: `app/src/main/java/com/nomedia/switcher/domain/model/AlbumEntry.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/data/AlbumRepositoryImpl.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/domain/usecase/ObserveAlbumsUseCase.kt`
- Test: `app/src/test/java/com/nomedia/switcher/data/AlbumRepositoryImplTest.kt`
- Test: `app/src/test/java/com/nomedia/switcher/domain/usecase/ObserveAlbumsUseCaseTest.kt`

- [ ] **Step 1: Write failing ordering tests**

Add tests proving:
- hidden pinning still wins over sort mode
- name sort preserves current behavior inside groups
- latest-media sort uses descending media time inside groups
- missing timestamps fall back to stable name/directory ordering

- [ ] **Step 2: Run the ordering tests to verify failure**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*AlbumRepositoryImplTest' --tests '*ObserveAlbumsUseCaseTest'
```
Expected: FAIL because sort mode and media time are not wired into merge ordering yet

- [ ] **Step 3: Extend merged album model**

Expose the data needed for sorting:
- current or persisted latest media time

- [ ] **Step 4: Implement layered sort precedence**

Keep ordering in this sequence:
- processing first
- hidden pinning group if enabled
- selected sort mode within group
- stable fallback by display name then directory key

- [ ] **Step 5: Re-run the ordering tests**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*AlbumRepositoryImplTest' --tests '*ObserveAlbumsUseCaseTest'
```
Expected: PASS

### Task 4: Add Settings UI For Sort Mode

**Files:**
- Modify: `app/src/main/java/com/nomedia/switcher/ui/albums/AlbumListViewModel.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/nomedia/switcher/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Test: `app/src/androidTest/java/com/nomedia/switcher/ui/settings/SettingsScreenTest.kt`
- Test: `app/src/test/java/com/nomedia/switcher/ui/albums/AlbumListViewModelTest.kt`

- [ ] **Step 1: Write failing UI/view-model tests**

Cover:
- settings screen displays current sort mode choices
- user selection callback is exposed
- view model updates persisted sort mode

- [ ] **Step 2: Run targeted tests to verify failure**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*AlbumListViewModelTest' :app:compileDebugAndroidTestKotlin
```
Expected: FAIL or compile failure because sort mode UI and state plumbing do not exist yet

- [ ] **Step 3: Add sort mode state plumbing**

Wire the new setting through:
- `AppRoot`
- `AlbumListViewModel`
- settings callbacks

- [ ] **Step 4: Add the settings UI**

Implement a lightweight mobile-friendly selector showing:
- sort by name
- sort by latest media time

Keep the existing hidden-pin setting intact and visually separate enough to avoid confusion.

- [ ] **Step 5: Re-run targeted tests**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest --tests '*AlbumListViewModelTest' :app:compileDebugAndroidTestKotlin
```
Expected: PASS

### Task 5: Replace Launcher Icon Resources

**Files:**
- Read source: `icon_concept.svg`
- Modify/Create: `app/src/main/res/mipmap-anydpi-v26/*`
- Modify/Create: `app/src/main/res/mipmap-*/*` as needed
- Modify/Create: `app/src/main/res/drawable/*` as needed for foreground/background
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Inspect the SVG and choose adaptive-icon composition**

Decide whether the SVG can be:
- foreground only with a separate flat background
- or needs a simplified foreground/background split

- [ ] **Step 2: Generate launcher icon resources**

Create the Android launcher icon resource set:
- adaptive icon XML
- foreground asset
- background asset
- any legacy mipmap assets required for compatibility

- [ ] **Step 3: Update manifest icon references**

Ensure the launcher icon references point to the new resources.

- [ ] **Step 4: Verify build and manual icon output**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:assembleDebug
```
Expected: PASS

Then manually verify on emulator/device:
- launcher icon appears
- icon is not cropped awkwardly

### Task 6: End-To-End Verification

**Files:**
- Verify: `app/build/outputs/apk/debug/`

- [ ] **Step 1: Run the full relevant unit test set**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest
```
Expected: PASS

- [ ] **Step 2: Build the debug APK**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:assembleDebug
```
Expected: PASS

- [ ] **Step 3: Manually verify behavior**

Check on emulator/device:
- launcher icon uses the new artwork
- settings page shows both sorting choices
- selection persists after app restart
- hidden-album pinning still works
- name sort still behaves as before
- latest-media sort changes only in-group ordering
