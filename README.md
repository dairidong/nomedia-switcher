# NoMedia Switcher

Android app for toggling `.nomedia` on switchable photo album directories.

## Development

- `minSdk = 30`
- `targetSdk = 35`
- Kotlin + Compose + Room + WorkManager

## Useful Commands

```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:testDebugUnitTest
GRADLE_USER_HOME=.gradle ./gradlew :app:assembleDebug
GRADLE_USER_HOME=.gradle ./gradlew :app:connectedDebugAndroidTest
```

## Release

### Local Signed Release

Release builds read signing inputs from environment variables:

- `NOMEDIA_RELEASE_STORE_FILE`
- `NOMEDIA_RELEASE_STORE_PASSWORD`
- `NOMEDIA_RELEASE_KEY_ALIAS`
- `NOMEDIA_RELEASE_KEY_PASSWORD`

Example:

```bash
export NOMEDIA_RELEASE_STORE_FILE=/abs/path/to/release.keystore
export NOMEDIA_RELEASE_STORE_PASSWORD=replace-me
export NOMEDIA_RELEASE_KEY_ALIAS=replace-me
export NOMEDIA_RELEASE_KEY_PASSWORD=replace-me

GRADLE_USER_HOME=.gradle ./gradlew :app:assembleRelease
```

If any release signing variable is missing, `:app:assembleRelease` fails with a clear error.

Keep the keystore out of git. Recommended local location:

- `keystore/nomedia-release.jks`
- `keystore/release.env`

### GitHub Actions Release

The workflow expects these repository secrets:

- `NOMEDIA_RELEASE_KEYSTORE_BASE64`
- `NOMEDIA_RELEASE_STORE_PASSWORD`
- `NOMEDIA_RELEASE_KEY_ALIAS`
- `NOMEDIA_RELEASE_KEY_PASSWORD`

Create `NOMEDIA_RELEASE_KEYSTORE_BASE64` from your local keystore:

```bash
base64 -w 0 /abs/path/to/release.keystore
```

Workflow triggers:

- Manual: `workflow_dispatch`
- Tag push: `v*`

Both triggers build the same signed release APK. Tag builds also publish the APK to a GitHub Release.

## Notes

- The app prioritizes locally known hidden albums before MediaStore scan results arrive.
- Toggle work continues in the background after the progress sheet is dismissed.
- Completion notifications reopen the app with the related album highlighted.
