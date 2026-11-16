# BMD ScanTovenaar

Android QR batch scanner — version 1.3.0.

## App flow
- Home has exactly four actions: Scan, Upload photos, Results, Settings.
- Scan opens an idle preview. In Video, Start begins continuous QR decoding and Stop pauses decoding while keeping the preview. In Foto, a single round shutter captures one still image and decodes its QR values. No image-upload or results buttons appear on the scanner.
- The scanner gear contains Video/Foto mode, HD / Full HD / highest-supported capture/analysis resolution, High/Normal/Low video confirmation (1/2/3 consecutive processed frames), and vibration after a successful code. Settings persist. CameraX falls back to supported device resolutions.
- Flash and zoom remain directly accessible on the preview. Unsupported flash/zoom are disabled; zoom is clamped to hardware limits. Automatic focus and tap-to-focus are retained.
- Every accepted QR value requests one short haptic pulse, including multiple codes in a photo. Pulses are spaced to avoid overwriting each other; blank/failed images and unconfirmed/cooldown-filtered live detections do not trigger them. The app toggle and Android's device haptic settings are respected. Haptics stop when the app goes into the background.
- Upload photos opens Android's document picker with multi-selection. Photos are decoded locally, never uploaded to a server. All QR values from each selected image are added to Results; unreadable images are reported.
- Results contains CSV creation and clearing. CSV is UTF-8 with BOM, quoted values and CRLF, one scan per row. Repeated scan contents are preserved.
- After the CSV is saved, Send opens Android's share chooser for that saved file; choose Outlook or another installed app. Sending requires a user action in that app.
- Settings offers Dutch, English, Turkish and Ukrainian, repeat delay, and CSV filename.
- The native vector artwork recreates the approved navy/cyan wave style and four colored home tiles. The Blue Medical header is a text recreation from the supplied visual, not an official logo asset.
- Results survive activity recreation/rotation. They are not a permanent scan archive; export before ending the session.

## Build and download
Run **Actions → Build BMD ScanTovenaar APK** or push to main. Download **BMD-ScanTovenaar-APK** from a successful run. The ZIP contains a debug-signed APK suitable for installation/testing, not a Play Store release.

CI pins Gradle 8.9, Java 17, Android SDK 35, Build Tools 34.0.0 and Android Gradle Plugin 8.6.1. No wrapper is committed; local builds require an installed Gradle 8.9 and ANDROID_HOME or local.properties.

```text
gradle --no-daemon :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
gradle --no-daemon :app:connectedDebugAndroidTest
```

CI also runs Android 35 emulator tests: four home actions and navigation, multiple QR codes across photos, blank/broken images, results through recreation, CSV file contents. Device screenshots and reports are uploaded as artifacts. Live physical-camera scanning, autofocus/torch hardware, the system photo picker, and Outlook delivery still require testing on a real phone.

The CI debug signing key is cached for subsequent builds. If Android rejects an update from an older APK signed with a different debug key, export existing results, uninstall that older app and install this APK. A stable release signing key is needed for production distribution.

## Name, icon and requested portrait sizes
The supplied launcher artwork is bundled as an adaptive Android icon, with density-specific legacy assets. The original is preserved in [docs/branding/icon-source.jpg](docs/branding/icon-source.jpg).

Localized names: Nederlands **BMD ScanTovenaar**; English **BMD ScanMagician**; Українська **BMD СканЧарівник**; Türkçe **BMD Tarama Büyücüsü**. The in-app name follows the chosen app language; Android launcher labels use the Android resource language.

The resolution menu additionally offers the requested portrait targets: 720×1600, 720×1612, 1080×1920, 1080×2340, 1080×2400, 1080×2424, 1220×2712, 1280×2856, 1440×3120 and 1440×3200. These are desired sizes, not a promise of sensor support. CameraX selects the nearest available native output if an exact size is absent; the gear panel reports the active native resolution. Images are never artificially upscaled to pretend that the sensor supports a screen resolution. Existing HD, Full HD and highest-supported selections remain compatible with saved settings.
