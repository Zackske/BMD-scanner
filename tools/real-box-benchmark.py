"""Diagnostic comparison only. No OCR, inferred lots, or expected-count filling."""
from pathlib import Path
import json, time, re
from PIL import Image, ImageOps, ImageDraw
import zxingcpp as zx

ROOT=Path('test-data/real-box'); OUT=Path('diagnostics'); OUT.mkdir(exist_ok=True)
reports=[]
for path in sorted(ROOT.glob('*.jpg')):
    im=Image.open(path).convert('RGB'); w,h=im.size; found=[]; timings=[]
    def read(image,ox=0,oy=0,scale=1,binarizer=zx.Binarizer.LocalAverage):
        for code in zx.read_barcodes(image,formats=zx.BarcodeFormat.DataMatrix|zx.BarcodeFormat.QRCode,try_rotate=True,try_downscale=False,text_mode=zx.TextMode.HRI,binarizer=binarizer,return_errors=False):
            pos=code.position; points=[pos.top_left,pos.top_right,pos.bottom_right,pos.bottom_left]
            box=[min(p.x for p in points)/scale+ox,min(p.y for p in points)/scale+oy,max(p.x for p in points)/scale+ox,max(p.y for p in points)/scale+oy]
            if any(r['raw']==code.text and abs((r['box'][0]+r['box'][2]-box[0]-box[2])/2)<max(2,min(r['box'][2]-r['box'][0],box[2]-box[0])*.3) and abs((r['box'][1]+r['box'][3]-box[1]-box[3])/2)<max(2,min(r['box'][3]-r['box'][1],box[3]-box[1])*.3) for r in found):continue
            match=re.search(r'\(10\)([^\x1d(]{1,20})',code.text)
            found.append({'raw':code.text,'format':str(code.format),'box':box,'lot':match.group(1) if match else None})
    start=time.monotonic();read(im)
    timings.append({'pass':'full-frame baseline','codes':len(found),'ms':round((time.monotonic()-start)*1000)})
    for scale in (1,2,3):
        start=time.monotonic()
        for y in range(0,h,320):
            for x in range(0,w,320):
                crop=im.crop((max(0,x-64),max(0,y-64),min(w,x+384),min(h,y+384)))
                if scale>1:crop=crop.resize((crop.width*scale,crop.height*scale),Image.Resampling.NEAREST)
                read(crop,max(0,x-64),max(0,y-64),scale)
        timings.append({'pass':f'overlapping tiles x{scale}','codes':len(found),'ms':round((time.monotonic()-start)*1000)})
    start=time.monotonic(); gray=ImageOps.autocontrast(im.convert('L'),cutoff=.5)
    for y in range(0,h,320):
        for x in range(0,w,320):
            crop=gray.crop((max(0,x-64),max(0,y-64),min(w,x+384),min(h,y+384)))
            crop=crop.resize((crop.width*3,crop.height*3),Image.Resampling.NEAREST)
            read(crop,max(0,x-64),max(0,y-64),3,zx.Binarizer.GlobalHistogram)
    timings.append({'pass':'contrast + global histogram tiles x3','codes':len(found),'ms':round((time.monotonic()-start)*1000)})
    found.sort(key=lambda r:(r['box'][1],r['box'][0]));overlay=im.copy();draw=ImageDraw.Draw(overlay)
    palette=['#16dd92','#a77aff','#13c7ee','#f29b48','#ee6cae']; lotcolors={}
    for i,r in enumerate(found):
        color=lotcolors.setdefault(r['lot'],palette[len(lotcolors)%len(palette)]) if r['lot'] else '#ffffff'
        draw.rectangle(r['box'],outline=color,width=3);draw.text((r['box'][0],max(0,r['box'][1]-14)),str(i+1),fill=color,stroke_width=1,stroke_fill='#001624')
    overlay.save(OUT/(path.stem+'-decoded.jpg'),quality=92)
    report={'image':path.name,'size':[w,h],'passes':timings,'decoded_symbols':len(found),'decoded_lots':{lot:sum(r['lot']==lot for r in found) for lot in sorted({r['lot'] for r in found if r['lot']})},'results':found,'limits':'Symbols are not validated physical products. No cross-photo deduplication. Expected 95 is not used by the decoder.'}
    reports.append(report); print(json.dumps({k:v for k,v in report.items() if k!='results'}),flush=True)
(OUT/'report.json').write_text(json.dumps({'decoder':'zxing-cpp 2.3.0 Python diagnostic, not Android camera validation','photos':reports},indent=2),encoding='utf8')

