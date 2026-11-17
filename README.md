# BMD ScanTovenaar

Native Android scanner with local QR/barcode recognition. Android 8.0+ (API 26); package `com.bmd.scanner`.

[Download the latest tested APK](https://github.com/Zackske/BMD-scanner/releases/latest/download/BMD-ScanTovenaar.apk)

## Home and settings

Four raised, animated home tiles: Scannen, Foto’s uploaden, Resultaten and Instellingen. The original supplied Blue Medical logo is preserved, with a subtle dark raised rim. The title has a gentle smoke animation. Android's disabled-animation preference is respected.

General settings contain Language, Scanner, Document type and About. About opens an animated “Made by Jack with ♥” canvas.

The ten interface languages are Nederlands, English, Українська, Türkçe, Italiano, Deutsch, Français, Español, Português and हिन्दी. Language names have flags; code contents are never translated.

## Live scanner

- Continuous live detection starts with the camera. There is no Video/Foto mode or Start/Stop control.
- The small live-detection gear selects QR + barcodes, QR only or barcodes only without discarding existing results.
- Numbered cyan markers follow physical label positions. Separate visible labels with identical decoded text count separately. A tracked label is counted once per session, including after edits or deletion.
- The counter shows total physical instances and separate QR/barcode totals. The list opens for individual corrections and deletion.
- Vertical zoom is on the right; the lamp above it shows off/on with matching icon, color and animation. Controls respect actual camera capabilities.
- The round shutter captures only when pressed. The captured still image remains frozen, including after activity recreation. After capture, choose local saving of the annotated photo plus document, native Android sharing, or scan again.
- Reset clears the current session, not previously saved documents/photos.
- The camera gear offers thirteen requested resolutions with native fallback, low/normal/high confirmation sensitivity (3/2/1 processed frames), and one queued haptic pulse per newly accepted instance. Android's system haptic preference is respected.

Keep the batch in view until capture. Physical identity cannot be guaranteed after indistinguishable labels leave the frame entirely, swap positions or are completely occluded. Review the editable list before saving. Hardware focus, lighting, blur, label size and quiet zones affect recognition.

Requested screen-like resolution dimensions are camera preferences, not a promise of unsupported sensor modes. The camera settings show the actual selected analysis size.

## Photos and exports

Photo import uses Android's multi-select document picker and processes images locally. No account or server upload is required. Importing separate photos adds their detections as separate rows.

Results export as UTF-8 CSV, real Excel XLSX, Word DOCX or PDF. Duplicate product rows, leading zeros, Unicode and individual corrections are preserved. CSV spreadsheet-formula prefixes are escaped. The file extension follows the selected document type.

Android's document/folder picker lets the user choose where files are saved. Annotated photos are downsampled for reliable memory use; the selected camera input size and exported photo size can differ. Android sharing grants temporary read access to the selected receiving app; email/social delivery is completed by the user in that app.

## Build and verification

GitHub Actions pins Temurin Java 17 and Gradle 8.9. It builds the APK, runs Android lint and JVM tests, then installs the app on an Android 35 emulator with real code images supplied to the virtual camera. A release download is published only after these checks pass.

Tests cover identical code instances and rotation, tracker stability and corrections, a regression image captured from the real emulator camera, confirmation on slow processed frames, real photo decoding, actual camera binding for all thirteen preferences, zoom/capture behavior, format filters, settings and ten languages, four export formats, Android file pickers and share URI grants. Emulator screenshots and a screen recording are retained as workflow artifacts; screenshots are also saved in `docs/screenshots`.

Physical Samsung camera quality, actual lamp brightness, the felt vibration and delivery through installed email/social apps still require testing on a phone. The downloadable APK is a debug-signed installable test build; CI caches its signing key to permit subsequent updates.

Locally, with the Android SDK, Java 17 and Gradle 8.9 installed:

```sh
gradle --no-daemon :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
gradle --no-daemon :app:connectedDebugAndroidTest
```

The recognition engine uses [ZXing](https://github.com/zxing/zxing), licensed under Apache 2.0.
