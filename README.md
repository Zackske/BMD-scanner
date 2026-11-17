# BMD ScanTovenaar

Native Android scanner with local QR/barcode recognition. Android 8.0+ (API 26); package `com.bmd.scanner`.

[Download the latest tested APK](https://github.com/Zackske/BMD-scanner/releases/latest/download/BMD-ScanTovenaar.apk)

## Home and settings

The supplied Dutch homepage artwork is preserved exactly, with four native tile actions: Scannen, Foto’s uploaden, Resultaten and Instellingen. The original title lettering, glossy 3D tile borders, scanner illustration, smoke artwork and Blue Medical plaque are retained. Other languages keep translated labels and accessibility names. See [homepage verification](docs/home-reference-2.1.md).

General settings contain Language, Scanner, Document type and About. About opens an animated “Made by Jack with ♥” canvas.

The ten interface languages are Nederlands, English, Українська, Türkçe, Italiano, Deutsch, Français, Español, Português and हिन्दी. Language names have flags; code contents are never translated.

## Live scanner

- Continuous live detection starts with the camera. There is no Video/Foto mode or Start/Stop control.
- The small live-detection gear selects QR + barcodes, QR only or barcodes only without discarding existing results.
- Numbered cyan markers follow physical label positions. Separate visible labels with identical decoded text count separately. A tracked label is counted once per session, including after edits or deletion.
- The counter shows total physical instances and separate QR/barcode totals. The list opens for individual corrections and deletion.
- Compact vertical zoom is on the right with 1× / 2× / 3× stops, clamped to actual camera capabilities. The top toolbar contains Flash, camera switching and scanner settings. Flash shows off/on status and a short activation glow.
- The centered Scan control captures only when pressed. The left list and right reset remain utility controls; History and Create tabs are removed. The floating gallery button imports photos and resumes the camera when cancelled. The captured still image remains frozen, including after activity recreation. After capture, choose local saving of the annotated photo plus document, native Android sharing, or scan again.
- Reset clears the current session, not previously saved documents/photos.
- The camera gear offers thirteen requested resolutions with native fallback, low/normal/high confirmation sensitivity (3/2/1 processed frames), and one queued haptic pulse per newly accepted instance. Android's system haptic preference is respected.

Keep the batch in view until capture. Physical identity cannot be guaranteed after indistinguishable labels leave the frame entirely, swap positions or are completely occluded. Review the editable list before saving. Hardware focus, lighting, blur, label size and quiet zones affect recognition.

Requested screen-like resolution dimensions are camera preferences, not a promise of unsupported sensor modes. The camera settings show the actual selected analysis size.

## Photos and exports

Photo import uses Android's multi-select document picker and processes images locally. No account or server upload is required. Importing separate photos adds their detections as separate rows.

Results export as UTF-8 CSV, real Excel XLSX, Word DOCX or PDF. Duplicate product rows, leading zeros, Unicode and individual corrections are preserved. CSV spreadsheet-formula prefixes are escaped. The file extension follows the selected document type.

Android's document/folder picker lets the user choose where files are saved. Annotated photos are downsampled for reliable memory use; the selected camera input size and exported photo size can differ. Android sharing grants temporary read access to the selected receiving app; email/social delivery is completed by the user in that app.

## Build and verification

GitHub Actions builds the APK with Java 17 and Gradle 8.9, runs Android lint and JVM tests, and verifies the APK signature and package. For version 2.2 the owner tests the scanner layout and camera behavior on the phone; no emulator/device-test result is claimed for this release. The separately recorded 2.1 homepage pixel comparison remains documented. See [scanner 2.2](docs/scanner-layout-2.2.md).

Recognition accepts QR codes and linear barcodes only. Data Matrix, Aztec and PDF417 are excluded. Tests cover accepted codes, separate identical labels, format filters and rejection of excluded matrix formats.

The 2.1 homepage follows the latest supplied example: title above, blue Scan / green Photo upload tiles, purple Results / orange Settings tiles, and the Blue Medical plaque below. CSV and email actions are inside Results.

The APK is a debug-signed test build. The signing key is cached for subsequent updates.
