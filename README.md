# NoMedia Switcher

NoMedia Switcher is an Android app for toggling `.nomedia` on album directories that can be mapped to real folders.

## What It Does

- Lists switchable albums from MediaStore and locally known hidden folders
- Toggles album visibility with a per-album switch
- Shows progress while large albums are being updated
- Lets background toggle work continue after the progress panel is hidden
- Keeps locally hidden albums easy to reach
- Supports sorting by album name or latest media time
- Shows album cover thumbnails, including cached video covers
- Includes Simplified Chinese UI strings

## Scope And Limits

- The app only manages albums that can be mapped to actual directories
- Some Android-protected or system-reserved directories cannot be granted to apps
- Visibility changes rely on `.nomedia` and Android media indexing behavior

## Build

```bash
GRADLE_USER_HOME=.gradle ./gradlew :app:assembleDebug
```

## License

This project is released under the MIT License. See [LICENSE](LICENSE).

## AI Note

This project was produced with substantial AI assistance under human direction and review.
