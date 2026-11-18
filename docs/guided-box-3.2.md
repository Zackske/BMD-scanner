# Guided box capture 3.2 — experimental

Home > Scan > Guided box scan. Asks the expected count before starting. Captures an overview and four overlapping details, with extra photos supported. Shares a printable QR marker sheet (A top-left, B top-right, C bottom-left, D bottom-right). Markers and products must remain stationary.

Every detail proposes a placement using four unique markers when all four are visible, otherwise conservative ORB/SIFT with RANSAC feature matching. The operator must verify the outline and photo before adding it. Failed or conflicting placement is not added. This matching is experimental on repetitive cartons and is not validated on a real 95-product multi-view capture. Four overview markers alone cannot locate details that show none of them unless image features also match.

Aligned detections use overview positions and contents to associate overlap. Equal contents at different positions remain separate. Several labels on one box require explicit product grouping. Grouping, split, removal and manually labelled lot corrections are available. Unknown or conflicting lots prevent the reviewed-complete state. An equal count is never shipment approval.

Lot-coloured borders surround decoded codes in live, frozen and single-scan annotated photos. OpenCV can propose unread QR locations as white dashed question-mark boxes; these never become product records. This does not locate every unread Data Matrix or fully missed code. All recognition remains actual checksum-accepted decoder output; printed lot text and expected counts never fill missing detections.

Local atomic session storage supports resuming the most recent box and retains expected-count changes. The ZIP saved/shared package contains session coordinates, raw payloads, expected/history, original photos and a CSV/XLSX/DOCX/PDF document using the selected document type. It always says shipmentApproved=false. A package may be explicitly incomplete.

Validation includes synthetic overlap identity, separate identical products, marker exclusion, invalid transform rollback, count integrity, synthetic native registration, missing/duplicate marker rejection and real-photo non-fabrication. Real phone camera quality, all 95 physical products, repetitive-pattern alignment and reliable physical-product deduplication still require real-world validation. The provided full-carton images previously decoded zero symbols in the separate Python diagnostic. Do not use this as the sole final distribution check.

## 3.2.1 barcode integrity correction

A full-carton photograph produced the spurious linear value `14446597` from stacked printed patterns. A single checksum hit is insufficient. Linear detections now require matching values, formats and positions on at least two additional parallel scanlines. Verification is restricted to the symbol's horizontal region so adjacent genuine barcodes remain separate. Rejected candidates do not prevent searching nearby regions. A separate Java diagnostic rejects linear reads in all six owner photographs, and its three genuine-label regression tests pass. The Android suite additionally rejects linear reads in the supplied carton reference. This is a targeted false-positive regression, not proof that all false positives are impossible.

Manual exclusions survive additional overlapping photos. Selected 2D/barcode filters apply to guided photo processing and resume correctly. Camera controls were checked in native screenshots and fitted to two readable lines. A count match is never shipment approval.
