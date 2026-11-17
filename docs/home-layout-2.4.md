# Choice 2 homepage, version 2.4

Uses the approved latest preview (868 x 1812): larger tiles, lowered grid, clean text without letter shadows, darker purple Tovenaar lettering and slightly raised Blue Medical logo. The reference contains no fake system bars and is used in full. Real click targets and translated label areas follow the new artwork. Localized labels no longer have drop shadows.

Foreground sections remain uniformly scaled. Only three internal background gaps flex to available height, including compression on shorter portrait screens. At the approved reference size every vertical coordinate remains unchanged. The layout covers the usable native viewport without top/bottom bands; Android cutout and system-bar safe insets remain honored.

JVM geometry tests cover six portrait sizes, foreground aspect ratios, exact reference-size reproduction, no top/bottom gaps and short-screen containment. Build, lint, unit tests and signature validation run in Actions. No new emulator or physical phone appearance test is claimed.
