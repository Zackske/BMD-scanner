package com.bmd.scanner;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;

/** Small vector artwork: sharp at every screen density, with no network assets. */
final class UiArt {
    static final int CYAN=0xff00dcff;
    static Drawable card(int top,int bottom,int edge,float radius){
        GradientDrawable base=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{top,bottom,0xff001329});
        base.setCornerRadius(radius);base.setStroke(2,edge);
        GradientDrawable sheen=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x38ffffff,0x00ffffff,0x00102030});
        sheen.setCornerRadius(radius);
        return new LayerDrawable(new Drawable[]{base,sheen});
    }
    static class Backdrop extends View {
        private final Paint p=new Paint(3);
        Backdrop(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff001127,0xff03284c,0xff000918},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);p.setShader(null);
            Path wave=new Path();float y=h-40*getResources().getDisplayMetrics().density;
            wave.moveTo(-20,y+12);wave.cubicTo(w*.27f,y-88,w*.56f,y+75,w+20,y-30);
            p.setStyle(Paint.Style.STROKE);
            for(int i=14;i>0;i--){p.setStrokeWidth(i*3);p.setColor(Color.argb(8,0,130,255));c.drawPath(wave,p);}
            p.setStrokeWidth(3);p.setColor(CYAN);c.drawPath(wave,p);
            p.setStrokeWidth(1);p.setColor(Color.WHITE);c.drawPath(wave,p);p.setStyle(Paint.Style.FILL);
        }
    }
    // Only the two approved illustration regions are decoded; controls and translated
    // app names remain native views. No screenshot UI is used as an interactive page.
    private static Bitmap scannerArt,brandArt;
    private static synchronized Bitmap artwork(Context context,boolean scanner){
        Bitmap cached=scanner?scannerArt:brandArt;if(cached!=null)return cached;
        try(java.io.InputStream in=context.getAssets().open("approved-home-artwork.png")){
            BitmapRegionDecoder decoder=BitmapRegionDecoder.newInstance(in,false);
            float sx=decoder.getWidth()/941f,sy=decoder.getHeight()/1670f;
            Rect region=scanner?new Rect((int)(130*sx),(int)(138*sy),(int)(312*sx),(int)(316*sy)):new Rect((int)(174*sx),(int)(1270*sy),(int)(767*sx),(int)(1442*sy));
            Bitmap bitmap=decoder.decodeRegion(region,new BitmapFactory.Options());decoder.recycle();
            if(scanner)scannerArt=bitmap;else brandArt=bitmap;return bitmap;
        }catch(java.io.IOException e){throw new IllegalStateException("Bundled brand artwork missing",e);}
    }
    static class ScannerMark extends View {
        private final Paint p=new Paint(3);private final Bitmap art;
        ScannerMark(Context c){super(c);art=artwork(c,true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            float size=Math.min(getWidth(),getHeight()),x=(getWidth()-size)/2,y=(getHeight()-size)/2;
            int layer=c.saveLayer(x,y,x+size,y+size,null);
            c.drawBitmap(art,null,new RectF(x,y,x+size,y+size),p);
            p.setShader(new RadialGradient(x+size/2,y+size/2,size*.7f,new int[]{Color.WHITE,Color.WHITE,Color.TRANSPARENT},new float[]{0,.65f,1},Shader.TileMode.CLAMP));p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));c.drawRect(x,y,x+size,y+size,p);p.setShader(null);p.setXfermode(null);c.restoreToCount(layer);
        }
    }
    static class NameText extends androidx.appcompat.widget.AppCompatTextView {
        NameText(Context c){super(c);setTextColor(CYAN);setPadding(0,0,(int)(14*getResources().getDisplayMetrics().density),0);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float d=getResources().getDisplayMetrics().density,x=getWidth()-7*d,y=getHeight()*.35f,r=4*d;Paint star=new Paint(3);star.setColor(0xffffd34d);Path path=new Path();path.moveTo(x,y-r);path.quadTo(x+1*d,y-1*d,x+r,y);path.quadTo(x+1*d,y+1*d,x,y+r);path.quadTo(x-1*d,y+1*d,x-r,y);path.quadTo(x-1*d,y-1*d,x,y-r);canvas.drawPath(path,star);}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);getPaint().setShader(new LinearGradient(0,0,Math.max(1,w),0,new int[]{0xff73f7fa,0xffb8c8ff,0xffe5a1ee},null,Shader.TileMode.CLAMP));}
    }
    static class Wave extends View {
        private final Paint p=new Paint(3);
        Wave(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();Path wave=new Path();wave.moveTo(-10,h*.6f);wave.cubicTo(w*.27f,-h*.2f,w*.56f,h*1.3f,w+10,h*.3f);p.setStyle(Paint.Style.STROKE);for(int i=7;i>0;i--){p.setStrokeWidth(i*2);p.setColor(Color.argb(12,0,140,255));c.drawPath(wave,p);}p.setStrokeWidth(2);p.setColor(CYAN);c.drawPath(wave,p);p.setStrokeWidth(.7f);p.setColor(Color.WHITE);c.drawPath(wave,p);}
    }
    static class Brand extends View {
        private final Paint p=new Paint(3);private final Bitmap art;
        Brand(Context c){super(c);art=artwork(c,false);setContentDescription("Blue Medical, A Translumina Group Company");}
        @Override protected void onDraw(Canvas c){float scale=Math.min((float)getWidth()/art.getWidth(),(float)getHeight()/art.getHeight());float w=art.getWidth()*scale,h=art.getHeight()*scale;c.drawBitmap(art,null,new RectF((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2),p);}
    }
    static class Icon extends View {
        private final String kind;private final int color;private final Paint p=new Paint(3);
        Icon(Context c,String kind){this(c,kind,Color.WHITE);}
        Icon(Context c,String kind,int color){super(c);this.kind=kind;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){
            float size=Math.min(getWidth(),getHeight());canvas.save();canvas.translate((getWidth()-size)/2,(getHeight()-size)/2);canvas.scale(size/64,size/64);
            p.setColor(color);p.setStrokeWidth(3.5f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
            switch(kind){
                case "qr":
                    cbox(canvas,8,8);cbox(canvas,38,8);cbox(canvas,8,38);
                    canvas.drawLine(39,39,48,39,p);canvas.drawLine(39,39,39,48,p);canvas.drawLine(48,48,57,48,p);canvas.drawLine(48,48,48,57,p);canvas.drawLine(39,57,42,57,p);canvas.drawLine(57,38,57,41,p);break;
                case "photo":
                    canvas.drawRoundRect(7,10,57,54,5,5,p);canvas.drawCircle(43,23,4,p);
                    Path mountain=new Path();mountain.moveTo(9,48);mountain.lineTo(26,29);mountain.lineTo(38,43);mountain.lineTo(46,36);mountain.lineTo(56,48);canvas.drawPath(mountain,p);break;
                case "list":
                    for(int y=16;y<=48;y+=16){canvas.drawCircle(10,y,2,p);canvas.drawLine(23,y,55,y,p);}break;
                case "settings":
                    Path gear=new Path();for(int i=0;i<32;i++){double angle=i*Math.PI/16-Math.PI/2;float radius=i%4<2?26:21;float x=32+(float)Math.cos(angle)*radius,y=32+(float)Math.sin(angle)*radius;if(i==0)gear.moveTo(x,y);else gear.lineTo(x,y);}gear.close();canvas.drawPath(gear,p);canvas.drawCircle(32,32,9,p);break;
                case "csv":
                    Path page=new Path();page.moveTo(14,6);page.lineTo(39,6);page.lineTo(53,20);page.lineTo(53,58);page.lineTo(14,58);page.close();canvas.drawPath(page,p);
                    canvas.drawLine(38,7,38,21,p);canvas.drawLine(38,21,52,21,p);p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(15);canvas.drawText("CSV",18,44,p);break;
                case "back":canvas.drawLine(49,32,14,32,p);canvas.drawLine(14,32,29,17,p);canvas.drawLine(14,32,29,47,p);break;
                case "camera":canvas.drawRoundRect(7,18,57,53,6,6,p);canvas.drawCircle(32,35,11,p);canvas.drawLine(21,18,24,10,p);canvas.drawLine(24,10,40,10,p);canvas.drawLine(40,10,43,18,p);break;
                case "play":Path play=new Path();play.moveTo(20,10);play.lineTo(52,32);play.lineTo(20,54);play.close();p.setStyle(Paint.Style.FILL);canvas.drawPath(play,p);break;
                case "minus":canvas.drawLine(15,32,49,32,p);break;
                case "plus":canvas.drawLine(15,32,49,32,p);canvas.drawLine(32,15,32,49,p);break;
                case "stop":p.setStyle(Paint.Style.FILL);canvas.drawRoundRect(18,16,46,46,4,4,p);break;
                case "flash":Path bolt=new Path();bolt.moveTo(38,5);bolt.lineTo(15,36);bolt.lineTo(31,36);bolt.lineTo(25,59);bolt.lineTo(50,27);bolt.lineTo(34,27);bolt.close();canvas.drawPath(bolt,p);break;
                case "check":canvas.drawLine(13,33,26,46,p);canvas.drawLine(26,46,52,18,p);break;
                case "language":canvas.drawCircle(32,32,25,p);canvas.drawOval(21,7,43,57,p);canvas.drawLine(8,32,56,32,p);canvas.drawLine(13,19,51,19,p);canvas.drawLine(13,45,51,45,p);break;
                default:canvas.drawCircle(32,32,23,p);canvas.drawLine(32,29,32,46,p);canvas.drawCircle(32,18,1,p);
            }
            canvas.restore();
        }
        private void cbox(Canvas c,float x,float y){c.drawRoundRect(x,y,x+18,y+18,1,1,p);p.setStyle(Paint.Style.FILL);c.drawRect(x+6,y+6,x+12,y+12,p);p.setStyle(Paint.Style.STROKE);}
    }
}
