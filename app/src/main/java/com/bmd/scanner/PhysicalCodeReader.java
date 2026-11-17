package com.bmd.scanner;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import java.util.*;

/** Finds physical symbols; equal text on separate labels remains separate. */
final class PhysicalCodeReader {
    private final List<ScanTracker.Detection> found=new ArrayList<>();
    private byte[] pixels;private int width,height,attempts;private String mode;
    static List<ScanTracker.Detection> decode(byte[] pixels,int width,int height,String mode){PhysicalCodeReader r=new PhysicalCodeReader();r.pixels=pixels;r.width=width;r.height=height;r.mode=mode;r.read();return r.found;}
    private BinaryBitmap bitmap(int x,int y,int w,int h){return new BinaryBitmap(new HybridBinarizer(new PlanarYUVLuminanceSource(pixels,width,height,x,y,w,h,false)));}
    private void read(){
        Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);hints.put(DecodeHintType.TRY_HARDER,Boolean.TRUE);
        if(!mode.equals("bar"))try{for(Result result:new QRCodeMultiReader().decodeMultiple(bitmap(0,0,width,height),hints))add(result,0,0,false);}catch(NotFoundException ignored){}
        if(!mode.equals("qr")){
            hints.put(DecodeHintType.POSSIBLE_FORMATS,Arrays.asList(BarcodeFormat.CODE_128,BarcodeFormat.CODE_39,BarcodeFormat.CODE_93,BarcodeFormat.CODABAR,BarcodeFormat.EAN_13,BarcodeFormat.EAN_8,BarcodeFormat.ITF,BarcodeFormat.UPC_A,BarcodeFormat.UPC_E,BarcodeFormat.RSS_14,BarcodeFormat.RSS_EXPANDED,BarcodeFormat.DATA_MATRIX,BarcodeFormat.AZTEC,BarcodeFormat.PDF_417));
            MultiFormatReader reader=new MultiFormatReader();reader.setHints(hints);region(reader,0,0,width,height,0);
        }
        found.sort(Comparator.comparingDouble((ScanTracker.Detection d)->d.top).thenComparingDouble(d->d.left));
    }
    private void region(MultiFormatReader reader,int x,int y,int w,int h,int depth){
        if(w<32||h<24||depth>9||attempts++>80)return;
        try{Result result=reader.decodeWithState(bitmap(x,y,w,h));ScanTracker.Detection d=add(result,x,y,true);if(d==null)return;
            int l=Math.max(x,(int)(d.left*width)-4),t=Math.max(y,(int)(d.top*height)-4),r=Math.min(x+w,(int)Math.ceil(d.right*width)+4),b=Math.min(y+h,(int)Math.ceil(d.bottom*height)+4);
            region(reader,x,y,w,t-y,depth+1);region(reader,x,b,w,y+h-b,depth+1);region(reader,x,t,l-x,b-t,depth+1);region(reader,r,t,x+w-r,b-t,depth+1);
        }catch(ReaderException ignored){}finally{reader.reset();}
    }
    private ScanTracker.Detection add(Result result,int ox,int oy,boolean linear){
        ResultPoint[] points=result.getResultPoints();if(points==null||points.length<2||result.getText()==null)return null;
        float l=width,t=height,r=0,b=0;for(ResultPoint p:points)if(p!=null){l=Math.min(l,p.getX()+ox);r=Math.max(r,p.getX()+ox);t=Math.min(t,p.getY()+oy);b=Math.max(b,p.getY()+oy);}
        if(r<=l)return null;
        boolean qr=result.getBarcodeFormat()==BarcodeFormat.QR_CODE;
        if(!linear||b-t>8){float px=(r-l)*.18f,py=(b-t)*.18f;l-=px;r+=px;t-=py;b+=py;}
        else { // Find the height of a linear symbol from its repeated row pattern.
            int row=Math.max(0,Math.min(height-1,Math.round((t+b)/2))),left=Math.max(0,(int)l),right=Math.min(width-1,(int)r);
            int top=row,bottom=row;while(top>0&&similarRow(row,top-1,left,right))top--;while(bottom<height-1&&similarRow(row,bottom+1,left,right))bottom++;
            t=top;b=bottom+1;l-=3;r+=3;
        }
        ScanTracker.Detection d=new ScanTracker.Detection(result.getText(),qr?"QR":"Barcode",Math.max(0,l/width),Math.max(0,t/height),Math.min(1,r/width),Math.min(1,b/height));
        for(ScanTracker.Detection old:found)if(old.value.equals(d.value)&&old.kind.equals(d.kind)&&Math.abs(old.x()-d.x())<Math.max(.012f,(old.right-old.left)*.3f)&&Math.abs(old.y()-d.y())<Math.max(.012f,(old.bottom-old.top)*.3f))return d;
        found.add(d);return d;
    }
    private boolean similarRow(int a,int b,int l,int r){int same=0,total=0;for(int x=l;x<=r;x+=2){if(((pixels[a*width+x]&255)<128)==((pixels[b*width+x]&255)<128))same++;total++;}return same>total*.88;}
}
