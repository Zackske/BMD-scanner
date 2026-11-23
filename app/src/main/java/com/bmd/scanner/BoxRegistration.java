package com.bmd.scanner;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import java.util.*;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.calib3d.Calib3d;
import org.opencv.imgproc.Imgproc;

/** Proposes geometry only; the operator must verify every placement on the overview. */
final class BoxRegistration {
    static final class Result {final double[] map;final boolean markers;Result(double[] m,boolean k){map=m;markers=k;}}
    static Result markers(List<ScanTracker.Detection> from,List<ScanTracker.Detection> to){float[] src=new float[8],dst=new float[8];int n=0;for(char id='A';id<='D';id++){String key="BMD-BOX-"+id;List<ScanTracker.Detection> a=new ArrayList<>(),b=new ArrayList<>();for(ScanTracker.Detection d:from)if(d.value.equals(key))a.add(d);for(ScanTracker.Detection d:to)if(d.value.equals(key))b.add(d);if(a.size()!=1||b.size()!=1)return null;src[n]=a.get(0).x();dst[n++]=b.get(0).x();src[n]=a.get(0).y();dst[n++]=b.get(0).y();}Matrix m=new Matrix();if(!m.setPolyToPoly(src,0,dst,0,4))return null;float[] values=new float[9];m.getValues(values);double[] out=new double[9];for(int i=0;i<9;i++)out[i]=values[i];return valid(out)?new Result(out,true):null;}
    static boolean valid(double[] m){try{float[][] p={BoxSession.point(m,0,0),BoxSession.point(m,1,0),BoxSession.point(m,1,1),BoxSession.point(m,0,1)};double area=0;int sign=0;for(int i=0;i<4;i++){float[] a=p[i],b=p[(i+1)%4],c=p[(i+2)%4];if(a[0]<-.15||a[1]<-.15||a[0]>1.15||a[1]>1.15)return false;double cross=(b[0]-a[0])*(c[1]-b[1])-(b[1]-a[1])*(c[0]-b[0]);int s=cross>0?1:-1;if(Math.abs(cross)<.003||(sign!=0&&sign!=s))return false;sign=s;area+=a[0]*b[1]-b[0]*a[1];}return area>.06&&area<2.5;}catch(Exception e){return false;}}
    static Result align(Bitmap detail,Bitmap overview,List<ScanTracker.Detection> current,List<ScanTracker.Detection> reference){Result marked=markers(current,reference);if(marked!=null)return marked;if(!OpenCVLoader.initLocal())return null;Result fast=features(detail,overview,false);return fast!=null?fast:features(detail,overview,true);}
    private static Result features(Bitmap detail,Bitmap overview,boolean precise){
        Mat a=new Mat(),b=new Mat(),da=new Mat(),db=new Mat(),mask=new Mat(),empty=new Mat(),h=null;MatOfKeyPoint ka=new MatOfKeyPoint(),kb=new MatOfKeyPoint();List<MatOfDMatch> pairs=new ArrayList<>();Feature2D orb=precise?SIFT.create(3000):ORB.create(3500);DescriptorMatcher matcher=DescriptorMatcher.create(precise?DescriptorMatcher.BRUTEFORCE:DescriptorMatcher.BRUTEFORCE_HAMMING);
        Bitmap sa=small(detail),sb=small(overview);MatOfPoint2f src=new MatOfPoint2f(),dst=new MatOfPoint2f();
        try{Utils.bitmapToMat(sa,a);Utils.bitmapToMat(sb,b);Imgproc.cvtColor(a,a,Imgproc.COLOR_RGBA2GRAY);Imgproc.cvtColor(b,b,Imgproc.COLOR_RGBA2GRAY);orb.detectAndCompute(a,empty,ka,da);orb.detectAndCompute(b,empty,kb,db);if(da.empty()||db.empty())return null;matcher.knnMatch(da,db,pairs,2);KeyPoint[] aa=ka.toArray(),bb=kb.toArray();List<Point> ap=new ArrayList<>(),bp=new ArrayList<>();Set<Integer> taken=new HashSet<>();for(MatOfDMatch pair:pairs){DMatch[] m=pair.toArray();if(m.length==2&&m[0].distance<(precise?180:45)&&m[0].distance<m[1].distance*.62&&taken.add(m[0].trainIdx)){ap.add(aa[m[0].queryIdx].pt);bp.add(bb[m[0].trainIdx].pt);}}android.util.Log.i("BMD-box","alignment "+(precise?"SIFT":"ORB")+" keypoints="+aa.length+"/"+bb.length+" matches="+ap.size());if(ap.size()<24)return null;src.fromList(ap);dst.fromList(bp);h=Calib3d.findHomography(src,dst,Calib3d.RANSAC,2.5,mask,3000,.999);if(h.empty())return null;int inliers=Core.countNonZero(mask);android.util.Log.i("BMD-box","alignment inliers="+inliers+"/"+ap.size());if(inliers<24||inliers<ap.size()*.8)return null;
            double minx=1,miny=1,maxx=0,maxy=0;for(int i=0;i<ap.size();i++)if(mask.get(i,0)[0]!=0){Point p=ap.get(i);minx=Math.min(minx,p.x/a.cols());maxx=Math.max(maxx,p.x/a.cols());miny=Math.min(miny,p.y/a.rows());maxy=Math.max(maxy,p.y/a.rows());}android.util.Log.i("BMD-box","alignment spread="+(maxx-minx)+","+(maxy-miny));if(maxx-minx<.35||maxy-miny<.35)return null;
            double[] m=new double[9];h.get(0,0,m);m[0]*=(double)a.cols()/b.cols();m[1]*=(double)a.rows()/b.cols();m[2]/=b.cols();m[3]*=(double)a.cols()/b.rows();m[4]*=(double)a.rows()/b.rows();m[5]/=b.rows();m[6]*=a.cols();m[7]*=a.rows();android.util.Log.i("BMD-box","alignment geometry="+Arrays.toString(m)+" valid="+valid(m));return valid(m)?new Result(m,false):null;
        }finally{for(MatOfDMatch p:pairs)p.release();a.release();b.release();da.release();db.release();mask.release();empty.release();ka.release();kb.release();src.release();dst.release();if(h!=null)h.release();orb.clear();matcher.clear();if(sa!=detail)sa.recycle();if(sb!=overview)sb.recycle();}
    }
    private static Bitmap small(Bitmap b){float s=Math.min(1,1200f/Math.max(b.getWidth(),b.getHeight()));return s==1?b:Bitmap.createScaledBitmap(b,Math.max(1,Math.round(b.getWidth()*s)),Math.max(1,Math.round(b.getHeight()*s)),true);}
}
