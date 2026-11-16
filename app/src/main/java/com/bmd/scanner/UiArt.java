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
    static class Brand extends View {
        private final Paint p=new Paint(3);
        Brand(Context c){super(c);setContentDescription("Blue Medical, A Translumina Group Company");}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();
            p.setShader(new RadialGradient(w*.5f,h*.35f,w*.65f,new int[]{0xffffffff,0xffbbdcff,0xff00386a},null,Shader.TileMode.CLAMP));
            c.drawRoundRect(0,0,w,h-12,16,16,p);p.setShader(null);
            p.setColor(0xff073d91);p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD_ITALIC));p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(w*.083f);c.drawText("BLUE MEDICAL",w/2,h*.49f,p);
            p.setStrokeWidth(2);c.drawLine(w*.08f,h*.18f,w*.92f,h*.18f,p);c.drawLine(w*.06f,h*.59f,w*.94f,h*.59f,p);
            p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(w*.036f);c.drawText("A Translumina Group Company",w/2,h*.79f,p);
            Path wave=new Path();wave.moveTo(0,h-17);wave.cubicTo(w*.3f,h-46,w*.6f,h+15,w,h-29);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(CYAN);c.drawPath(wave,p);p.setStyle(Paint.Style.FILL);
        }
    }
    static class Icon extends View {
        private final String kind;private final Paint p=new Paint(3);
        Icon(Context c,String kind){super(c);this.kind=kind;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){
            float size=Math.min(getWidth(),getHeight());canvas.save();canvas.translate((getWidth()-size)/2,(getHeight()-size)/2);canvas.scale(size/64,size/64);
            p.setColor(Color.WHITE);p.setStrokeWidth(3.5f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
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
                    canvas.drawCircle(32,32,17,p);canvas.drawCircle(32,32,7,p);
                    for(int i=0;i<8;i++){canvas.save();canvas.rotate(i*45,32,32);canvas.drawLine(32,8,32,15,p);canvas.restore();}break;
                case "csv":
                    Path page=new Path();page.moveTo(14,6);page.lineTo(39,6);page.lineTo(53,20);page.lineTo(53,58);page.lineTo(14,58);page.close();canvas.drawPath(page,p);
                    canvas.drawLine(38,7,38,21,p);canvas.drawLine(38,21,52,21,p);p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(15);canvas.drawText("CSV",18,44,p);break;
                case "back":canvas.drawLine(49,32,14,32,p);canvas.drawLine(14,32,29,17,p);canvas.drawLine(14,32,29,47,p);break;
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
