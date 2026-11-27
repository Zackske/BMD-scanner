package com.bmd.scanner;

import android.content.Context;
import android.graphics.*;
import android.util.LruCache;
import android.view.View;
import java.util.HashMap;

/** Approved ring and gold sparkle for four Home tiles and two scan-choice tiles only. */
final class OrbitalArtwork {
    private static final HashMap<Integer,Rect> crops=new HashMap<>();
    private static final LruCache<Integer,Bitmap> rings=new LruCache<Integer,Bitmap>(8*1024){
        @Override protected int sizeOf(Integer key,Bitmap value){return Math.max(1,value.getAllocationByteCount()/1024);}
    };
    private static int core(int resource){
        if(resource==R.drawable.ref_scan_final_check)return R.drawable.core_scan_final_check;
        if(resource==R.drawable.ref_home_customers)return R.drawable.core_home_customers;
        if(resource==R.drawable.ref_home_instructions)return R.drawable.core_home_instructions;
        if(resource==R.drawable.ref_home_settings)return R.drawable.core_home_settings;
        if(resource==R.drawable.ref_scan_count_green)return R.drawable.core_scan_count;
        if(resource==R.drawable.ref_document_capture_purple)return R.drawable.core_document_capture;
        throw new IllegalArgumentException("Orbital artwork is reserved for the six approved tiles");
    }
    static int surfaceInk(int requested){
        ThemeSkin skin=ThemeSkin.active;
        return skin==ThemeSkin.LIME?0xff9bdf35:skin==ThemeSkin.VIOLET?0xffaa65e8:skin==ThemeSkin.DARK?0xffb3c2cd:requested;
    }
    private static synchronized Rect crop(Context context,int resource){
        Rect found=crops.get(resource);if(found!=null)return found;
        Bitmap image=ReferenceArt.bitmap(context,resource);int w=image.getWidth(),h=image.getHeight(),left=w,top=h,right=0,bottom=0;
        int[] pixels=new int[w*h];image.getPixels(pixels,0,w,0,0,w,h);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(Color.alpha(pixels[y*w+x])>8){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);}
        found=right>left?new Rect(left,top,right,bottom):new Rect(0,0,w,h);crops.put(resource,found);return found;
    }
    private static synchronized Bitmap ring(Context context,int colour){
        Bitmap found=rings.get(colour);if(found!=null)return found;
        Bitmap source=ReferenceArt.bitmap(context,R.drawable.orbit_ring);Rect visible=crop(context,R.drawable.orbit_ring);int w=visible.width(),h=visible.height();
        int[] pixels=new int[w*h];source.getPixels(pixels,0,w,visible.left,visible.top,w,h);float[] target=new float[3],hsv=new float[3];Color.colorToHSV(colour,target);
        for(int i=0;i<pixels.length;i++){int alpha=Color.alpha(pixels[i]);if(alpha==0)continue;Color.colorToHSV(pixels[i],hsv);if(hsv[1]>.08f){hsv[0]=target[0];hsv[1]*=Math.min(1,target[1]/.8f);pixels[i]=Color.HSVToColor(alpha,hsv);}}
        found=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);rings.put(colour,found);return found;
    }
    private static void orbit(Canvas canvas,Context context,RectF bounds,int colour,boolean front){
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        float w=bounds.width(),h=bounds.height();RectF dest=new RectF(bounds.left+w*.035f,bounds.top+h*.53f,bounds.right-w*.035f,bounds.top+h*.82f);
        canvas.save();canvas.rotate(-18,bounds.centerX(),bounds.top+h*.65f);
        canvas.clipRect(dest.left-w*.03f,front?dest.centerY():dest.top-h*.05f,dest.right+w*.03f,front?dest.bottom+h*.05f:dest.centerY());
        canvas.drawBitmap(ring(context,colour),null,dest,paint);canvas.restore();
    }
    private static void star(Canvas canvas,Context context,RectF bounds){
        canvas.drawBitmap(ReferenceArt.bitmap(context,R.drawable.orbit_star),crop(context,R.drawable.orbit_star),bounds,new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
    }
    static void draw(Canvas canvas,Context context,int resource,RectF bounds,int colour){
        int subject=core(resource);boolean wide=subject==R.drawable.core_scan_count;
        float aspect=wide?1.7f:1,scale=Math.min(bounds.width()/(100*aspect),bounds.height()/100),w=100*aspect,h=100;
        canvas.save();canvas.translate(bounds.centerX()-w*scale/2,bounds.centerY()-h*scale/2);canvas.scale(scale,scale);
        RectF frame=new RectF(0,0,w,h);orbit(canvas,context,frame,colour,false);
        Rect source=crop(context,subject);float limitW=w*.75f,limitH=72,fit=Math.min(limitW/source.width(),limitH/source.height()),sw=source.width()*fit,sh=source.height()*fit;
        RectF dest=new RectF((w-sw)/2,81-sh,(w+sw)/2,81);
        canvas.drawBitmap(ReferenceArt.bitmap(context,subject),source,dest,new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
        orbit(canvas,context,frame,colour,true);star(canvas,context,new RectF(w*.84f-9,6,w*.84f+9,24));canvas.restore();
    }
    static final class Artwork extends View {
        private final int resource,colour;private final RectF bounds=new RectF();
        Artwork(Context context,int resource,int colour){super(context);this.resource=resource;this.colour=colour;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){bounds.set(0,0,getWidth(),getHeight());OrbitalArtwork.draw(canvas,getContext(),resource,bounds,colour);}
    }
}
