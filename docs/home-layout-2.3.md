# Home layout 2.3.3

Fixes the empty top and bottom bands reported in 2.3.2. The original supplied design is drawn in seven contiguous full-width horizontal rows. Foreground rows (header, complete two-row tile grid, logo and lower wave) retain a uniform scale. Only three background-only internal gaps absorb extra viewport height. The first artwork row starts at the top of the usable content and the last ends at the bottom. Real button targets and translated labels use the same piecewise vertical mapping. Native Android system-bar/cutout safe insets remain honored.

HomeGeometryTest checks six portrait screen sizes for full viewport coverage, unchanged foreground aspect ratios and monotonic row positions, plus short-screen containment. Build, lint, JVM tests and APK signature checks run in Actions. No new physical-phone or native emulator visual test is claimed.
