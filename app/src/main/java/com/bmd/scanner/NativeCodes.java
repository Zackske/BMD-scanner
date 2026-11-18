package com.bmd.scanner;
import android.graphics.*;
import java.util.*;
import zxingcpp.BarcodeReader;

/** Native multi-symbol reader: matrix labels are detected together, not restricted to screen centre. */
final class NativeCodes {
    static List<ScanTracker.Detection> decode(byte[] gray,int width,int height,String mode){
        if(mode.equals("bar"))return PhysicalCodeReader.decode(gray,width,height,"bar");
        int[] rgb=new int[gray.length];for(int i=0;i<gray.length;i++){int c=gray[i]&255;rgb[i]=0xff000000|(c<<16)|(c<<8)|c;}
        Bitmap bitmap=Bitmap.createBitmap(rgb,width,height,Bitmap.Config.ARGB_8888);try{return decode(bitmap,mode,false);}finally{bitmap.recycle();}
    }
    static List<ScanTracker.Detection> decode(Bitmap bitmap,String mode){return decode(bitmap,mode,true);}
    private static List<ScanTracker.Detection> decode(Bitmap bitmap,String mode,boolean detailed){
        List<ScanTracker.Detection> bars=mode.equals("qr")?new ArrayList<>():linear(bitmap);if(mode.equals("bar"))return bars;
        BarcodeReader.Options options=new BarcodeReader.Options();
        Set<BarcodeReader.Format> formats=new HashSet<>();
        if(!mode.equals("bar")){formats.add(BarcodeReader.Format.QR_CODE);formats.add(BarcodeReader.Format.DATA_MATRIX);}

        options.setFormats(formats);options.setTryHarder(true);options.setTryRotate(true);options.setTryInvert(true);options.setTryDownscale(false);options.setMaxNumberOfSymbols(255);options.setTextMode(BarcodeReader.TextMode.HRI);
        List<ScanTracker.Detection> result=new ArrayList<>(bars);BarcodeReader reader=new BarcodeReader(options);readRegion(reader,bitmap,new Rect(),result);
        // Overlapping native passes avoid a single-pass symbol ceiling on dense scenes.
        if(result.size()>=200||(Math.max(bitmap.getWidth(),bitmap.getHeight())>=2000&&result.size()>=64)){
            int overlapX=Math.max(16,bitmap.getWidth()/20),overlapY=Math.max(16,bitmap.getHeight()/20);
            for(int row=0;row<2;row++)for(int col=0;col<2;col++)readRegion(reader,bitmap,new Rect(Math.max(0,col*bitmap.getWidth()/2-overlapX),Math.max(0,row*bitmap.getHeight()/2-overlapY),Math.min(bitmap.getWidth(),(col+1)*bitmap.getWidth()/2+overlapX),Math.min(bitmap.getHeight(),(row+1)*bitmap.getHeight()/2+overlapY)),result);
        }
        if(detailed&&result.size()<32){regions(reader,bitmap,result);if(Math.max(bitmap.getWidth(),bitmap.getHeight())<1800){Bitmap enlarged=Bitmap.createScaledBitmap(bitmap,bitmap.getWidth()*3,bitmap.getHeight()*3,false);try{readRegion(reader,enlarged,new Rect(),result);regions(reader,enlarged,result);}finally{enlarged.recycle();}}}
        result.sort(Comparator.comparingDouble((ScanTracker.Detection d)->d.top).thenComparingDouble(d->d.left));return result;
    }
    private static List<ScanTracker.Detection> linear(Bitmap bitmap){int w=bitmap.getWidth(),h=bitmap.getHeight();int[] rgb=new int[w*h];bitmap.getPixels(rgb,0,w,0,0,w,h);byte[] gray=new byte[rgb.length];for(int i=0;i<rgb.length;i++){int c=rgb[i];gray[i]=(byte)((((c>>16)&255)*306+((c>>8)&255)*601+(c&255)*117)>>10);}return PhysicalCodeReader.decode(gray,w,h,"bar");}
    private static void regions(BarcodeReader reader,Bitmap image,List<ScanTracker.Detection> result){boolean tall=image.getHeight()>image.getWidth()*1.7f,wide=image.getWidth()>image.getHeight()*1.7f;int nx=wide?8:tall?1:3,ny=tall?8:wide?1:3;for(int y=0;y<ny;y++)for(int x=0;x<nx;x++){int sx=image.getWidth()/nx,sy=image.getHeight()/ny;readRegion(reader,image,new Rect(Math.max(0,x*sx-sx/2),Math.max(0,y*sy-sy/2),Math.min(image.getWidth(),(x+1)*sx+sx/2),Math.min(image.getHeight(),(y+1)*sy+sy/2)),result);}}
    private static void readRegion(BarcodeReader reader,Bitmap bitmap,Rect crop,List<ScanTracker.Detection> result){
        int ox=crop.isEmpty()?0:crop.left,oy=crop.isEmpty()?0:crop.top;
        for(BarcodeReader.Result code:reader.read(bitmap,crop,0)){
            if(code.getError()!=null||code.getText()==null||code.getText().isEmpty())continue;
            BarcodeReader.Position p=code.getPosition();Point[] points={p.getTopLeft(),p.getTopRight(),p.getBottomRight(),p.getBottomLeft()};
            float l=bitmap.getWidth(),t=bitmap.getHeight(),r=0,b=0;for(Point q:points){l=Math.min(l,q.x+ox);t=Math.min(t,q.y+oy);r=Math.max(r,q.x+ox);b=Math.max(b,q.y+oy);}
            float margin=Math.max(2,(b-t)*.10f);if(b-t<4){t-=4;b+=4;}
            String kind=code.getFormat()==BarcodeReader.Format.DATA_MATRIX?"Data Matrix":code.getFormat().name().startsWith("QR_CODE")?"QR":"Barcode";
            ScanTracker.Detection detection=new ScanTracker.Detection(code.getText(),kind,Math.max(0,(l-margin)/bitmap.getWidth()),Math.max(0,(t-margin)/bitmap.getHeight()),Math.min(1,(r+margin)/bitmap.getWidth()),Math.min(1,(b+margin)/bitmap.getHeight()));
            boolean duplicate=false;for(ScanTracker.Detection old:result)if(old.kind.equals(detection.kind)&&old.value.equals(detection.value)&&Math.abs(old.x()-detection.x())<Math.max(2f/bitmap.getWidth(),Math.min(old.right-old.left,detection.right-detection.left)*.3f)&&Math.abs(old.y()-detection.y())<Math.max(2f/bitmap.getHeight(),Math.min(old.bottom-old.top,detection.bottom-detection.top)*.3f)){duplicate=true;break;}if(!duplicate)result.add(detection);
        }
    }
}

