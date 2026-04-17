# Release Signing And CI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a shared release-signing pipeline that works both locally and in GitHub Actions without committing the keystore into the repository.

**Architecture:** Release signing is configured in Gradle and reads signing inputs from environment variables so local builds and CI builds use the same interface. GitHub Actions restores the keystore from secrets, exports the same variables, builds the signed release APK, and uploads the artifact or publishes it for tag builds.

**Tech Stack:** Android Gradle Plugin, Kotlin DSL, GitHub Actions, Java keystore, environment-variable based signing

---

### Task 1: Wire Gradle Release Signing

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `.gitignore`
- Test: local Gradle release task output

- [ ] **Step 1: Add failing release-signing expectation**

Document the expected behavior directly in the build logic task:
- `assembleRelease` must fail clearly when required release-signing variables are missing.
- `assembleDebug` must remain unaffected.

- [ ] **Step 2: Implement release signing config**

Add environment-variable backed release signing inputs:
- `NOMEDIA_RELEASE_STORE_FILE`
- `NOMEDIA_RELEASE_STORE_PASSWORD`
- `NOMEDIA_RELEASE_KEY_ALIAS`
- `NOMEDIA_RELEASE_KEY_PASSWORD`

Configure `signingConfigs.release` and bind it to `buildTypes.release`.

- [ ] **Step 3: Add repository safety guards**

Ignore local signing artifacts:
- `keystore/`
- `*.jks`
- `*.keystore`
- local env helper files if created

- [ ] **Step 4: Verify Gradle behavior**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:assembleDebug
```
Expected: PASS

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:assembleRelease
```
Expected: FAIL with a clear missing-signing-config message when env vars are absent

### Task 2: Add GitHub Actions Release Workflow

**Files:**
- Create: `.github/workflows/release.yml`
- Test: workflow syntax and environment contract

- [ ] **Step 1: Define workflow triggers**

Add:
- `workflow_dispatch`
- `push` tags matching `v*`

- [ ] **Step 2: Restore keystore from secrets**

Use these secrets:
- `NOMEDIA_RELEASE_KEYSTORE_BASE64`
- `NOMEDIA_RELEASE_STORE_PASSWORD`
- `NOMEDIA_RELEASE_KEY_ALIAS`
- `NOMEDIA_RELEASE_KEY_PASSWORD`

Decode the keystore into a temporary file inside the runner workspace.

- [ ] **Step 3: Export shared signing env vars**

Map secrets into the same environment variables Gradle expects locally.

- [ ] **Step 4: Build and publish artifacts**

Run:
```bash
./gradlew :app:assembleRelease
```

Upload the signed APK as an artifact.
If the trigger is a tag, create or update a GitHub Release and attach the APK.

- [ ] **Step 5: Verify workflow structure**

Check for:
- checkout
- Java setup
- Android/Gradle cache
- keystore restore
- signed release build
- artifact upload
- tag-only release upload

### Task 3: Document Local Release Process

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add release section**

Document:
- how to generate a keystore locally
- where to store it without committing it
- how to export signing variables
- how to run a signed local release build

- [ ] **Step 2: Add GitHub Actions secret setup**

Document:
- required secret names
- how to Base64-encode the keystore
- how tag/manual triggers differ

- [ ] **Step 3: Keep secrets out of docs**

Do not include:
- real passwords
- real aliases beyond placeholders
- real keystore paths from your machine

### Task 4: Verify End-To-End Local Release Build

**Files:**
- Verify: `app/build/outputs/apk/release/`

- [ ] **Step 1: Generate or use a local release keystore**

Use a non-debug keystore for formal release verification.

- [ ] **Step 2: Export release-signing env vars**

Example shell contract:
```bash
export NOMEDIA_RELEASE_STORE_FILE=/abs/path/to/release.keystore
export NOMEDIA_RELEASE_STORE_PASSWORD=...
export NOMEDIA_RELEASE_KEY_ALIAS=...
export NOMEDIA_RELEASE_KEY_PASSWORD=...
```

- [ ] **Step 3: Build signed release APK**

Run:
```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:assembleRelease
```
Expected: PASS and produce a signed release APK

- [ ] **Step 4: Validate result**

Verify:
- APK exists under `app/build/outputs/apk/release/`
- package is installable on emulator/device
- debug builds still work
