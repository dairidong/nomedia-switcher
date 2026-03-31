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

## Notes

- The app prioritizes locally known hidden albums before MediaStore scan results arrive.
- Toggle work continues in the background after the progress sheet is dismissed.
- Completion notifications reopen the app with the related album highlighted.
