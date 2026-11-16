# BMD Scanner

Android batch QR scanner with CSV export and Outlook/Hotmail sharing.

## Build

Run GitHub Actions > Build BMD Scanner APK > Run workflow. Download `BMD-Scanner-APK` from the successful run's Artifacts section.

## Notes

- Each detected QR code is stored separately. Repeated contents are allowed; scanning is rate-limited to avoid dozens of copies from a stationary camera.
- CSV has no header, just one correctly escaped result per row (UTF-8 with BOM for Excel).
- Sharing uses Android's chooser; select Outlook if installed and signed in.
- No corporate logo is bundled because the official image asset was not provided in this project.
