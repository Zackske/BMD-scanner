# BMD Scanner

Android QR batch scanner — version 1.2.0.

## App flow
- Home has exactly four actions: Scan, Upload photos, Results, Settings.
- Scan uses the bundled offline ML Kit QR detector, CameraX 1280×720 target analysis, tap-to-focus, flashlight (when available), and a configurable repeat interval.
- Upload photos opens Android's document picker with multi-selection. Photos are decoded locally, never uploaded to a server. All QR values from each selected image are added to Results; unreadable images are reported.
- Results contains CSV creation and clearing. CSV is UTF-8 with BOM, quoted values and CRLF, one scan per row. Repeated scan contents are preserved.
- After the CSV is saved, Send opens Android's share chooser for that saved file; choose Outlook or another installed app. Sending requires a user action in that app.
- Settings offers Dutch, English, Turkish and Ukrainian, repeat delay, and CSV filename.
- The native vector artwork recreates the approved navy/cyan wave style and four colored home tiles. The Blue Medical header is a text recreation from the supplied visual, not an official logo asset.
- Results survive activity recreation/rotation. They are not a permanent scan archive; export before ending the session.

## Build and download
Run **Actions → Build BMD Scanner APK** or push to main. Download **BMD-Scanner-APK** from a successful run. The ZIP contains a debug-signed APK suitable for installation/testing, not a Play Store release.

CI pins Gradle 8.9, Java 17, Android SDK 35, Build Tools 34.0.0 and Android Gradle Plugin 8.6.1. No wrapper is committed; local builds require an installed Gradle 8.9 and ANDROID_HOME or local.properties.

```text
gradle --no-daemon :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
gradle --no-daemon :app:connectedDebugAndroidTest
```

CI also runs Android 35 emulator tests: four home actions and navigation, multiple QR codes across photos, blank/broken images, results through recreation, CSV file contents. Device screenshots and reports are uploaded as artifacts. Live physical-camera scanning, autofocus/torch hardware, the system photo picker, and Outlook delivery still require testing on a real phone.

The CI debug signing key is cached for subsequent builds. If Android rejects an update from an older APK signed with a different debug key, export existing results, uninstall that older app and install this APK. A stable release signing key is needed for production distribution.
