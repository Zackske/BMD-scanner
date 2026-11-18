package com.bmd.scanner;
import android.graphics.Bitmap;
import java.util.*;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.objdetect.QRCodeDetector;

/** QR location hypotheses only. Never imported into product or lot records. */
final class CodeCandidates {
    private static final ThreadLocal<QRCodeDetector> DETECTORS=ThreadLocal.withInitial(QRCodeDetector::new);
    static List<ScanTracker.Detection> find(byte[] gray,int w,int h,List<ScanTracker.Detection> decoded){int[] rgb=new int[gray.length];for(int i=0;i<gray.length;i++){int g=gray[i]&255;rgb[i]=0xff000000|g<<16|g<<8|g;}Bitmap b=Bitmap.createBitmap(rgb,w,h,Bitmap.Config.ARGB_8888);try{return find(b,decoded);}finally{b.recycle();}}
    static List<ScanTracker.Detection> find(Bitmap bitmap,List<ScanTracker.Detection> decoded){List<ScanTracker.Detection> out=new ArrayList<>();if(!OpenCVLoader.initLocal())return out;Mat image=new Mat(),points=new Mat();Bitmap small=null;try{float scale=Math.min(1,1000f/Math.max(bitmap.getWidth(),bitmap.getHeight()));small=scale==1?bitmap:Bitmap.createScaledBitmap(bitmap,Math.max(1,Math.round(bitmap.getWidth()*scale)),Math.max(1,Math.round(bitmap.getHeight()*scale)),true);Utils.bitmapToMat(small,image);QRCodeDetector detector=DETECTORS.get();if(!detector.detectMulti(image,points)||points.empty())return out;float[] xy=new float[(int)(points.total()*points.channels())];points.get(0,0,xy);for(int n=0;n+7<xy.length;n+=8){float l=1,t=1,r=0,b=0;for(int k=0;k<8;k+=2){float x=xy[n+k]/small.getWidth(),y=xy[n+k+1]/small.getHeight();l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);}ScanTracker.Detection d=new ScanTracker.Detection("","Candidate",l,t,r,b);boolean read=false;for(ScanTracker.Detection code:decoded)if(Math.abs(code.x()-d.x())<Math.max(code.right-code.left,r-l)*.6&&Math.abs(code.y()-d.y())<Math.max(code.bottom-code.top,b-t)*.6){read=true;break;}if(!read&&r>l&&b>t)out.add(d);}}catch(Exception e){android.util.Log.w("BMD-candidate","QR location",e);}finally{image.release();points.release();if(small!=null&&small!=bitmap)small.recycle();}return out;}
}

