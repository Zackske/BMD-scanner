# BMD Scanner

Android batch QR scanner with CSV export and Outlook/Hotmail sharing.

## Build

Run GitHub Actions > Build BMD Scanner APK > Run workflow. Download `BMD-Scanner-APK` from the successful run's Artifacts section.

The Android application lives in `app/`. The root build declares Android Gradle Plugin 8.6.1; CI installs Gradle 8.9, Temurin Java 17, Android SDK 35 and Build Tools 34.0.0 explicitly. CI runs `gradle --no-daemon --stacktrace :app:assembleDebug :app:lintDebug`.

For a local build, install the same toolchain and set `ANDROID_HOME` (or `sdk.dir` in an untracked `local.properties`). No Gradle wrapper is committed; use an installed Gradle 8.9. The artifact contains a debug-signed APK, suitable for testing, not a Play Store release.

## Notes

- Each detected QR code is stored separately. Repeated contents are allowed; scanning is rate-limited to avoid dozens of copies from a stationary camera.
- CSV has no header, just one correctly escaped result per row (UTF-8 with BOM for Excel).
- Sharing uses Android's chooser; select Outlook if installed and signed in.
- No corporate logo is bundled because the official image asset was not provided in this project.
