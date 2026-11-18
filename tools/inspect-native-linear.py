from pathlib import Path
from PIL import Image
import zxingcpp as zx
files=list(Path('test-data/real-box').glob('*.jpg'))+[Path('app/src/androidTest/assets/single-package-example.png')]
for f in files:
    found=zx.read_barcodes(Image.open(f),formats=zx.BarcodeFormat.LinearCodes,try_rotate=True,try_downscale=False,text_mode=zx.TextMode.Plain)
    print('NATIVE-LINEAR',f.name,[(str(c.format),c.text) for c in found],flush=True)

