# Home layout 2.3.2

Restores the intact owner-provided picture after the 2.3.1 sliced-layout regression. Uses reference image content rectangle (31,106)-(910,1564), excluding screenshot system bars and baked outer inset. Applies a single uniform scale to preserve all artwork proportions, with real navigation hit targets transformed by the same scale and translation. Portrait phone layouts fit the content width, so the cyan lines reach both screen edges. Spare vertical space contains extensions of the source background edge rows only; the title, tiles, text and logo are never stretched. Short/wide layouts fit the complete image within the viewport instead of cropping it.

Verified reference dimensions and tall-portrait geometry locally. GitHub Actions runs build, lint, JVM unit tests and APK signature verification. Whole-image pixel and navigation device tests are updated but are not executed by this phone-testing workflow. No new native emulator or physical phone test is claimed.
