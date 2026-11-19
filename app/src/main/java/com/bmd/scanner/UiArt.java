package com.bmd.scanner;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;

/** Small vector artwork: sharp at every screen density, with no network assets. */
final class UiArt {
    static final int CYAN=0xff00dcff;
    static Drawable card(int top,int bottom,int edge,float radius){
        ThemeSkin skin=ThemeSkin.active;if(skin!=ThemeSkin.BMD){top=skin.top;bottom=skin.bottom;edge=ThemeSkin.edge(edge);}
        GradientDrawable base=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{top,bottom,ThemeSkin.active.bottom});
        base.setCornerRadius(radius);base.setStroke(2,edge);
        GradientDrawable sheen=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x38ffffff,0x00ffffff,0x00102030});
        sheen.setCornerRadius(radius);
        return new LayerDrawable(new Drawable[]{base,sheen});
    }
    static Drawable raised(int requestedTop,int requestedBottom,int requestedEdge,float radius){ThemeSkin skin=ThemeSkin.active;final int top=skin==ThemeSkin.BMD?requestedTop:skin.top,bottom=skin==ThemeSkin.BMD?requestedBottom:skin.bottom,edge=skin==ThemeSkin.BMD?requestedEdge:ThemeSkin.edge(requestedEdge);return new Drawable(){
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        @Override public void draw(Canvas c){Rect b=getBounds();float w=b.width(),h=b.height(),d=c.getDensity()/160f,depth=Math.min(6*d,h*.07f),rr=Math.min(radius,Math.min(w,h)*.5f);c.save();c.translate(b.left,b.top);p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(skin==ThemeSkin.LIGHT?0xffbfcfdc:0xff000a16);c.drawRoundRect(0,depth,w,h,rr,rr,p);
            int dark=skin==ThemeSkin.LIGHT?bottom:Color.rgb(Math.round(Color.red(bottom)*.32f),Math.round(Color.green(bottom)*.32f),Math.round(Color.blue(bottom)*.32f));RectF face=new RectF(1,1,w-1,h-depth);p.setShader(new LinearGradient(0,0,w,h,new int[]{top,bottom,dark},new float[]{0,.65f,1},Shader.TileMode.CLAMP));c.drawRoundRect(face,rr,rr,p);p.setShader(null);
            int clip=c.save();Path mask=new Path();mask.addRoundRect(face,rr,rr,Path.Direction.CW);c.clipPath(mask);
            p.setShader(new LinearGradient(0,0,0,h*.48f,new int[]{0xb0ffffff,0x22ffffff,0x00ffffff},new float[]{0,.36f,1},Shader.TileMode.CLAMP));Path gloss=new Path();gloss.moveTo(0,0);gloss.lineTo(w,0);gloss.lineTo(w,h*.09f);gloss.cubicTo(w*.65f,h*.15f,w*.20f,h*.12f,0,h*.35f);gloss.close();c.drawPath(gloss,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.min(6*d,w*.017f));p.setShader(new LinearGradient(0,0,w,h,new int[]{0xdfffffff,edge,0x50000000},new float[]{0,.35f,1},Shader.TileMode.CLAMP));RectF inner=new RectF(4*d,4*d,w-4*d,h-depth-4*d);c.drawRoundRect(inner,Math.max(1,rr-3*d),Math.max(1,rr-3*d),p);p.setShader(null);c.restoreToCount(clip);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,d));p.setShader(new LinearGradient(0,0,w,h,new int[]{Color.WHITE,edge,0xff243c48},new float[]{0,.45f,1},Shader.TileMode.CLAMP));c.drawRoundRect(face,rr,rr,p);p.setShader(null);
            p.setColor(0xccffffff);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(Math.min(2*d,w*.01f));Path shine=new Path();shine.moveTo(5*d,Math.min(rr,h*.32f));shine.quadTo(5*d,5*d,Math.min(rr,w*.3f),5*d);shine.lineTo(w*.66f,5*d);c.drawPath(shine,p);p.setStyle(Paint.Style.FILL);c.restore();}
        @Override public void setAlpha(int a){p.setAlpha(a);}@Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}@Override public void getOutline(Outline o){o.setRoundRect(getBounds(),radius);}
    };}
    static Drawable tile(int top,int bottom,int edge,float radius){ThemeSkin original=ThemeSkin.active;if(original==ThemeSkin.TRANSLUMINA){ThemeSkin.active=ThemeSkin.BMD;try{return raised(top,bottom,edge,radius);}finally{ThemeSkin.active=original;}}return raised(top,bottom,edge,radius);}
    static class Backdrop extends View {
        private final Paint p=new Paint(3);
        Backdrop(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h,new int[]{ThemeSkin.active.background,ThemeSkin.active.halo,ThemeSkin.active.background},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);p.setShader(null);
            Path wave=new Path();float y=h-40*getResources().getDisplayMetrics().density;
            wave.moveTo(-20,y+12);wave.cubicTo(w*.27f,y-88,w*.56f,y+75,w+20,y-30);
            p.setStyle(Paint.Style.STROKE);
            for(int i=14;i>0;i--){p.setStrokeWidth(i*3);p.setColor(Color.argb(8,0,130,255));c.drawPath(wave,p);}
            p.setStrokeWidth(3);p.setColor(ThemeSkin.active.accent);c.drawPath(wave,p);
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
    static Drawable mist(){return new Drawable(){private final Paint p=new Paint(3);private final Runnable tick=this::invalidateSelf;@Override public void draw(Canvas canvas){Rect r=getBounds();float w=r.width(),h=r.height();canvas.save();canvas.translate(r.left,r.top+(float)Math.sin(android.os.SystemClock.uptimeMillis()/3200.0)*h*.025f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setMaskFilter(new BlurMaskFilter(h*.05f,BlurMaskFilter.Blur.NORMAL));for(int i=0;i<3;i++){Path wave=new Path();float y=h*(.35f+i*.17f);wave.moveTo(w*.08f,y);wave.cubicTo(w*.3f,y-h*.3f,w*.43f,y+h*.3f,w*.65f,y-h*.02f);wave.cubicTo(w*.8f,y-h*.2f,w*.85f,y-h*.15f,w*.95f,y-h*.35f);p.setStrokeWidth(h*.027f);p.setShader(new LinearGradient(0,y,w,y-h*.3f,new int[]{0x0000baff,0x2700cfff,0x1908baff,0x0000bbff},null,Shader.TileMode.CLAMP));canvas.drawPath(wave,p);}p.setShader(null);p.setMaskFilter(null);canvas.restore();unscheduleSelf(tick);if(android.animation.ValueAnimator.areAnimatorsEnabled()&&isVisible())scheduleSelf(tick,android.os.SystemClock.uptimeMillis()+80);}@Override public void setAlpha(int alpha){p.setAlpha(alpha);}@Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}};}
    static class ScannerMark extends View {
        private final Paint p=new Paint(3);private final Bitmap art;private final Rect bounds;
        ScannerMark(Context c){super(c);art=BitmapFactory.decodeResource(c.getResources(),R.drawable.launcher_foreground);int w=art.getWidth(),h=art.getHeight(),left=w,top=h,right=0,bottom=0;int[] pixels=new int[w*h];art.getPixels(pixels,0,w,0,0,w,h);for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(Color.alpha(pixels[y*w+x])>2){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);}bounds=new Rect(left,top,right,bottom);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){float scale=Math.min(getWidth()/(float)bounds.width(),getHeight()/(float)bounds.height()),w=bounds.width()*scale,h=bounds.height()*scale,x=(getWidth()-w)/2,y=(getHeight()-h)/2;c.drawBitmap(art,bounds,new RectF(x,y,x+w,y+h),p);}
    }
    static class NameText extends androidx.appcompat.widget.AppCompatTextView {
        NameText(Context c){super(c);setTextColor(CYAN);setPadding(0,0,(int)(14*getResources().getDisplayMetrics().density),0);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float d=getResources().getDisplayMetrics().density,x=Math.min(getWidth()-7*d,getPaddingLeft()+(getLayout()==null?getPaint().measureText(getText().toString()):getLayout().getLineRight(0))+7*d),y=getHeight()*.35f,r=4*d;Paint star=new Paint(3);star.setColor(0xffffd34d);Path path=new Path();path.moveTo(x,y-r);path.quadTo(x+1*d,y-1*d,x+r,y);path.quadTo(x+1*d,y+1*d,x,y+r);path.quadTo(x-1*d,y+1*d,x-r,y);path.quadTo(x-1*d,y-1*d,x,y-r);canvas.drawPath(path,star);}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);getPaint().setShader(new LinearGradient(0,0,Math.max(1,w),0,new int[]{0xff73f7fa,0xffb8c8ff,0xffe5a1ee},null,Shader.TileMode.CLAMP));}
    }
    static class Wave extends View {
        private final Paint p=new Paint(3);
        Wave(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();Path wave=new Path();wave.moveTo(-10,h*.6f);wave.cubicTo(w*.27f,-h*.2f,w*.56f,h*1.3f,w+10,h*.3f);p.setStyle(Paint.Style.STROKE);for(int i=7;i>0;i--){p.setStrokeWidth(i*2);p.setColor(Color.argb(12,0,140,255));c.drawPath(wave,p);}p.setStrokeWidth(2);p.setColor(ThemeSkin.active.accent);c.drawPath(wave,p);p.setStrokeWidth(.7f);p.setColor(Color.WHITE);c.drawPath(wave,p);}
    }
    static class Brand extends androidx.appcompat.widget.AppCompatImageView {
        Brand(Context c){super(c);ThemeSkin skin=ThemeSkin.load(c);setImageResource(skin==ThemeSkin.TRANSLUMINA?R.drawable.translumina_logo:R.drawable.blue_medical_logo);setColorFilter(skin.brandFilter());setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);setBackground(null);setPadding(0,0,0,0);setElevation(0);setContentDescription(skin==ThemeSkin.TRANSLUMINA?"Translumina · Limitless Possibilities":"Blue Medical · A Translumina Group Company");}
    }
    static class Magic extends View {
        final Icon icon;final int tint;final Paint p=new Paint(3);
        Magic(Context c,String name,int tint){super(c);this.tint=ThemeSkin.active==ThemeSkin.LIGHT&&ThemeSkin.contrast(tint,ThemeSkin.active.bottom)<3?mix(tint,ThemeSkin.LIGHT.text,.6f):tint;icon=new Icon(c,name,this.tint);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),size=Math.min(w,h),x=(w-size)/2,y=(h-size)/2,r=size*.23f;ThemeSkin s=ThemeSkin.active;c.save();c.translate(x,y);p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,0,size,size,new int[]{mix(tint,s.top,.65f),s.bottom},null,Shader.TileMode.CLAMP));c.drawRoundRect(1,1,size-1,size-1,r,r,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(size*.018f);p.setColor(tint);c.drawRoundRect(1,1,size-1,size-1,r,r,p);c.save();c.translate(size*.17f,size*.17f);int z=Math.round(size*.66f);icon.layout(0,0,z,z);icon.draw(c);c.restore();p.setColor(tint);p.setStrokeWidth(size*.025f);c.save();c.rotate(-23,size/2,size/2);c.drawArc(size*.07f,size*.43f,size*.92f,size*.79f,5,165,false,p);c.restore();p.setStyle(Paint.Style.FILL);float cx=size*.82f,cy=size*.18f,rr=size*.08f;Path star=new Path();star.moveTo(cx,cy-rr);star.quadTo(cx+rr*.2f,cy-rr*.2f,cx+rr,cy);star.quadTo(cx+rr*.2f,cy+rr*.2f,cx,cy+rr);star.quadTo(cx-rr*.2f,cy+rr*.2f,cx-rr,cy);star.quadTo(cx-rr*.2f,cy-rr*.2f,cx,cy-rr);p.setColor(s==ThemeSkin.LIGHT?tint:Color.WHITE);c.drawPath(star,p);c.restore();}
    }
    static int mix(int a,int b,float amount){return Color.rgb(Math.round(Color.red(a)*(1-amount)+Color.red(b)*amount),Math.round(Color.green(a)*(1-amount)+Color.green(b)*amount),Math.round(Color.blue(a)*(1-amount)+Color.blue(b)*amount));}

    static class Icon extends View {
        private final String kind;private final int color;private final Paint p=new Paint(3);
        Icon(Context c,String kind){this(c,kind,Color.WHITE);}
        Icon(Context c,String kind,int color){super(c);this.kind=kind;this.color=ThemeSkin.icon(color);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){
            float size=Math.min(getWidth(),getHeight());canvas.save();canvas.translate((getWidth()-size)/2,(getHeight()-size)/2);canvas.scale(size/64,size/64);
            p.setShader(new LinearGradient(0,0,64,64,new int[]{ThemeSkin.active==ThemeSkin.LIGHT?color:Color.WHITE,color,mix(color,ThemeSkin.active.bottom,.28f)},new float[]{0,.43f,1},Shader.TileMode.CLAMP));p.setColor(color);p.setStrokeWidth(3.5f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
            switch(kind){
                case "qr":
                    cbox(canvas,8,8);cbox(canvas,38,8);cbox(canvas,8,38);
                    canvas.drawLine(39,39,48,39,p);canvas.drawLine(39,39,39,48,p);canvas.drawLine(48,48,57,48,p);canvas.drawLine(48,48,48,57,p);canvas.drawLine(39,57,42,57,p);canvas.drawLine(57,38,57,41,p);break;
                case "barcode":p.setStyle(Paint.Style.FILL);for(int x=7;x<58;x+=6)canvas.drawRect(x,12,x+(x%3==0?3:2),52,p);break;
                case "both":
                    canvas.save();canvas.scale(.56f,.56f);cbox(canvas,5,8);cbox(canvas,35,8);cbox(canvas,5,38);canvas.drawLine(36,40,50,40,p);canvas.drawLine(40,50,54,50,p);canvas.restore();p.setStyle(Paint.Style.FILL);for(int x=39;x<61;x+=4)canvas.drawRect(x,12,x+2,52,p);break;
                case "photo":
                    canvas.drawRoundRect(7,10,57,54,5,5,p);canvas.drawCircle(43,23,4,p);
                    Path mountain=new Path();mountain.moveTo(9,48);mountain.lineTo(26,29);mountain.lineTo(38,43);mountain.lineTo(46,36);mountain.lineTo(56,48);canvas.drawPath(mountain,p);break;
                case "users":canvas.drawCircle(19,22,8,p);canvas.drawCircle(30,14,7,p);canvas.drawRoundRect(6,34,32,54,9,9,p);for(int y=19;y<50;y+=13)canvas.drawLine(43,y,59,y,p);break;
                case "book":Path book=new Path();book.moveTo(4,12);book.quadTo(18,4,32,15);book.quadTo(46,4,60,12);book.lineTo(60,55);book.quadTo(46,47,32,58);book.quadTo(18,47,4,55);book.close();canvas.drawPath(book,p);canvas.drawLine(32,15,32,58,p);for(int y=22;y<48;y+=12){canvas.drawLine(11,y,14,y+3,p);canvas.drawLine(14,y+3,21,y-4,p);}canvas.drawLine(40,23,53,39,p);canvas.drawLine(53,23,40,39,p);break;
                case "mail":canvas.drawRoundRect(5,13,59,51,4,4,p);canvas.drawLine(7,16,32,36,p);canvas.drawLine(32,36,57,16,p);break;
                case "pallet":for(int i=0;i<3;i++)canvas.drawRoundRect(8+i*17,30,24+i*17,47,2,2,p);canvas.drawRoundRect(24,10,41,28,2,2,p);canvas.drawLine(6,53,59,53,p);canvas.drawLine(13,53,13,59,p);canvas.drawLine(51,53,51,59,p);break;
                case "save":canvas.drawRoundRect(8,6,49,54,5,5,p);canvas.drawRect(19,6,38,23,p);canvas.drawRect(19,36,38,54,p);canvas.drawCircle(47,47,13,p);canvas.drawLine(40,47,45,52,p);canvas.drawLine(45,52,54,41,p);break;
                case "list":
                    for(int y=16;y<=48;y+=16){canvas.drawCircle(10,y,2,p);canvas.drawLine(23,y,55,y,p);}break;
                case "settings":
                    Path gear=new Path();for(int i=0;i<32;i++){double angle=i*Math.PI/16-Math.PI/2;float radius=i%4<2?26:21;float x=32+(float)Math.cos(angle)*radius,y=32+(float)Math.sin(angle)*radius;if(i==0)gear.moveTo(x,y);else gear.lineTo(x,y);}gear.close();canvas.drawPath(gear,p);canvas.drawCircle(32,32,9,p);break;
                case "close":canvas.drawLine(17,17,47,47,p);canvas.drawLine(47,17,17,47,p);break;
                case "reset":canvas.drawArc(9,9,55,55,-50,290,false,p);canvas.drawLine(48,8,53,23,p);canvas.drawLine(53,23,38,21,p);break;
                case "delete":canvas.drawLine(12,16,52,16,p);canvas.drawRoundRect(18,17,46,55,3,3,p);canvas.drawLine(25,8,39,8,p);canvas.drawLine(27,26,27,46,p);canvas.drawLine(37,26,37,46,p);break;
                case "document":canvas.drawRoundRect(15,6,49,58,4,4,p);canvas.drawLine(23,23,41,23,p);canvas.drawLine(23,34,41,34,p);canvas.drawLine(23,45,36,45,p);break;
                case "csv":
                    Path page=new Path();page.moveTo(14,6);page.lineTo(39,6);page.lineTo(53,20);page.lineTo(53,58);page.lineTo(14,58);page.close();canvas.drawPath(page,p);
                    canvas.drawLine(38,7,38,21,p);canvas.drawLine(38,21,52,21,p);p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(15);canvas.drawText("CSV",18,44,p);break;
                case "back":canvas.drawLine(49,32,14,32,p);canvas.drawLine(14,32,29,17,p);canvas.drawLine(14,32,29,47,p);break;
                case "camera":canvas.drawRoundRect(7,18,57,53,6,6,p);canvas.drawCircle(32,35,11,p);canvas.drawLine(21,18,24,10,p);canvas.drawLine(24,10,40,10,p);canvas.drawLine(40,10,43,18,p);break;
                case "play":Path play=new Path();play.moveTo(20,10);play.lineTo(52,32);play.lineTo(20,54);play.close();p.setStyle(Paint.Style.FILL);canvas.drawPath(play,p);break;
                case "minus":canvas.drawLine(15,32,49,32,p);break;
                case "plus":canvas.drawLine(15,32,49,32,p);canvas.drawLine(32,15,32,49,p);break;
                case "stop":p.setStyle(Paint.Style.FILL);canvas.drawRoundRect(18,16,46,46,4,4,p);break;
                case "flashoff":case "flash":Path bolt=new Path();bolt.moveTo(38,5);bolt.lineTo(15,36);bolt.lineTo(31,36);bolt.lineTo(25,59);bolt.lineTo(50,27);bolt.lineTo(34,27);bolt.close();canvas.drawPath(bolt,p);if(kind.equals("flashoff")){p.setColor(0xffa3b6c8);canvas.drawLine(8,7,55,57,p);}break;
                case "check":canvas.drawLine(13,33,26,46,p);canvas.drawLine(26,46,52,18,p);break;
                case "palette":canvas.drawOval(6,8,58,54,p);canvas.drawCircle(20,23,3,p);canvas.drawCircle(34,18,3,p);canvas.drawCircle(46,28,3,p);canvas.drawCircle(22,40,3,p);break;
                case "language":canvas.drawCircle(32,32,25,p);canvas.drawOval(21,7,43,57,p);canvas.drawLine(8,32,56,32,p);canvas.drawLine(13,19,51,19,p);canvas.drawLine(13,45,51,45,p);break;
                default:canvas.drawCircle(32,32,23,p);canvas.drawLine(32,29,32,46,p);canvas.drawCircle(32,18,1,p);
            }
            canvas.restore();
        }
        private void cbox(Canvas c,float x,float y){c.drawRoundRect(x,y,x+18,y+18,1,1,p);p.setStyle(Paint.Style.FILL);c.drawRect(x+6,y+6,x+12,y+12,p);p.setStyle(Paint.Style.STROKE);}
    }
}

