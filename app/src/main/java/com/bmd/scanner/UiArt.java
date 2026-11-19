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
    static Drawable tile(int top,int bottom,int edge,float radius){return ReferenceArt.tile(top,bottom,edge,radius);}
    static class Backdrop extends View {
        Backdrop(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){ReferenceArt.background(c,getWidth(),getHeight());}
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
    static Drawable mist(){return new Drawable(){
        private final Paint p=new Paint(3);private final Runnable tick=this::invalidateSelf;
        @Override public void draw(Canvas canvas){Rect r=getBounds();float w=r.width(),h=r.height();canvas.save();canvas.translate(r.left,r.top+(float)Math.sin(android.os.SystemClock.uptimeMillis()/3200.0)*h*.018f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);int ink=ThemeSkin.active.mistInk()&0xffffff;
            for(int i=0;i<5;i++){Path wave=new Path();float y=h*(.26f+i*.1f);wave.moveTo(w*.01f,y);wave.cubicTo(w*.22f,y-h*.25f,w*.32f,y+h*.38f,w*.55f,y+h*.02f);wave.cubicTo(w*.73f,y-h*.34f,w*.83f,y+h*.12f,w*.98f,y-h*.24f);
                p.setMaskFilter(new BlurMaskFilter(h*.025f,BlurMaskFilter.Blur.NORMAL));p.setStrokeWidth(h*.025f);p.setShader(new LinearGradient(0,y,w,y,new int[]{0,0x29000000|ink,0x22000000|ink,0},new float[]{0,.27f,.7f,1},Shader.TileMode.CLAMP));canvas.drawPath(wave,p);
                p.setMaskFilter(null);p.setStrokeWidth(h*(.003f+i*.0005f));p.setShader(new LinearGradient(0,y,w,y,new int[]{0,0x35000000|ink,0x24000000|ink,0},new float[]{0,.35f,.72f,1},Shader.TileMode.CLAMP));canvas.drawPath(wave,p);
            }p.setShader(null);p.setMaskFilter(null);canvas.restore();unscheduleSelf(tick);if(android.animation.ValueAnimator.areAnimatorsEnabled()&&isVisible())scheduleSelf(tick,android.os.SystemClock.uptimeMillis()+80);
        }
        @Override public void setAlpha(int alpha){p.setAlpha(alpha);}@Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    };}
    static class ScannerMark extends View {
        private final Paint p=new Paint(3);private final Bitmap art;private final Rect bounds;
        ScannerMark(Context c){super(c);art=ReferenceArt.scanner(c);int w=art.getWidth(),h=art.getHeight(),left=w,top=h,right=0,bottom=0;int[] pixels=new int[w*h];art.getPixels(pixels,0,w,0,0,w,h);for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(Color.alpha(pixels[y*w+x])>2){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);}bounds=new Rect(left,top,right,bottom);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
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
        Brand(Context c){this(c,ThemeSkin.load(c));}
        Brand(Context c,ThemeSkin skin){super(c);setImageResource(skin==ThemeSkin.TRANSLUMINA?R.drawable.translumina_logo:R.drawable.blue_medical_logo);setColorFilter(skin.brandFilter());setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);setBackground(null);setPadding(0,0,0,0);setElevation(0);setContentDescription(skin==ThemeSkin.TRANSLUMINA?"Translumina · Limitless Possibilities":"Blue Medical · A Translumina Group Company");}
    }
    /** Bevelled metal pictograms with the approved orbital light and small sparks. */
    static class Magic extends View {
        final int tint;private final String kind;private boolean boxed=true;private final Paint p=new Paint(3);private final Icon fallback;
        Magic(Context c,String name,int tint){super(c);kind=name;this.tint=ThemeSkin.active==ThemeSkin.LIGHT?mix(tint,ThemeSkin.LIGHT.text,.22f):tint;fallback=new Icon(c,name,ThemeSkin.active==ThemeSkin.LIGHT?0xff12365a:0xffe4f3ff);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        Magic unboxed(){boxed=false;return this;}
        private void metal(Canvas c,Path path,int colour){
            c.save();c.translate(0,2.5f);p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(mix(colour,0xff071526,.7f));c.drawPath(path,p);c.restore();
            p.setShader(new LinearGradient(15,10,75,88,new int[]{0xffffffff,mix(colour,Color.WHITE,.65f),mix(colour,0xff1a3045,.48f),0xfff6fcff},new float[]{0,.25f,.68f,1},Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawPath(path,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);p.setColor(0xbaffffff);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        }
        private Path rounded(float l,float t,float r,float b,float radius){Path q=new Path();q.addRoundRect(new RectF(l,t,r,b),radius,radius,Path.Direction.CW);return q;}
        private void ball(Canvas c,float x,float y,float radius,int colour){Path q=new Path();q.addCircle(x,y,radius,Path.Direction.CW);metal(c,q,colour);p.setShader(new RadialGradient(x-radius*.3f,y-radius*.4f,radius,new int[]{0xbaffffff,0x00ffffff},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,radius,p);p.setShader(null);}
        private void line(Canvas c,float x,float y,float x2,float y2,int colour,float width){p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(width+1.7f);p.setColor(0x80112339);c.drawLine(x,y+1.5f,x2,y2+1.5f,p);p.setStrokeWidth(width);p.setShader(new LinearGradient(x,y,x2+1,y2+1,new int[]{Color.WHITE,colour,0xffeefaff},null,Shader.TileMode.CLAMP));c.drawLine(x,y,x2,y2,p);p.setShader(null);p.setStyle(Paint.Style.FILL);}
        private void finder(Canvas c,float x,float y){Path q=rounded(x,y,x+22,y+22,2.5f);q.addRoundRect(new RectF(x+5,y+5,x+17,y+17),1,1,Path.Direction.CCW);q.setFillType(Path.FillType.EVEN_ODD);metal(c,q,0xffc7e1ef);metal(c,rounded(x+8,y+8,x+14,y+14,.8f),tint);}
        private void glyph(Canvas c){int silver=0xffb2c9d7;
            switch(kind){
                case "qr":finder(c,17,20);finder(c,49,20);finder(c,17,52);metal(c,rounded(50,53,57,60,1),silver);metal(c,rounded(62,53,70,60,1),silver);metal(c,rounded(50,65,57,73,1),silver);metal(c,rounded(62,67,70,75,1),silver);break;
                case "users":ball(c,38,30,10,silver);ball(c,55,20,8,silver);metal(c,rounded(24,43,51,72,10),silver);metal(c,rounded(48,33,65,53,7),silver);for(int y=29;y<=65;y+=18)metal(c,rounded(71,y,94,y+5,2.5f),silver);break;
                case "settings":Path gear=new Path();for(int i=0;i<40;i++){double angle=i*Math.PI/20-Math.PI/2;float radius=i%5<3?30:24;float x=50+(float)Math.cos(angle)*radius,y=48+(float)Math.sin(angle)*radius;if(i==0)gear.moveTo(x,y);else gear.lineTo(x,y);}gear.close();gear.addCircle(50,48,14,Path.Direction.CCW);gear.setFillType(Path.FillType.EVEN_ODD);metal(c,gear,silver);ball(c,50,48,9,tint);break;
                case "book":Path book=new Path();book.moveTo(16,22);book.quadTo(32,14,49,25);book.quadTo(66,14,83,22);book.lineTo(83,76);book.quadTo(65,68,49,80);book.quadTo(32,68,16,76);book.close();metal(c,book,silver);line(c,49,26,49,76,0xff738aab,1.3f);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(13);p.setColor(0xff173668);for(int i=0;i<3;i++){float y=37+i*14;c.drawText(Integer.toString(i+1),21,y,p);line(c,32,y-4,35,y-1,0xff163c73,1.7f);line(c,35,y-1,41,y-8,0xff163c73,1.7f);}line(c,59,35,74,59,0xff3b4a86,3.8f);line(c,73,35,59,59,0xff3b4a86,3.8f);ball(c,58,34,3,silver);break;
                case "language":p.setShader(new RadialGradient(37,31,44,new int[]{mix(tint,Color.WHITE,.35f),mix(tint,0xff092e54,.70f)},null,Shader.TileMode.CLAMP));c.drawCircle(50,48,28,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.4f);p.setShader(new LinearGradient(20,15,78,83,new int[]{Color.WHITE,tint,0xffe7fcff},null,Shader.TileMode.CLAMP));c.drawCircle(50,48,28,p);c.drawOval(new RectF(37,20,63,76),p);c.drawOval(new RectF(23,33,77,63),p);c.drawLine(22,48,78,48,p);c.drawLine(50,20,50,76,p);p.setShader(null);p.setStyle(Paint.Style.FILL);break;
                case "palette":Path palette=new Path();palette.moveTo(27,73);palette.cubicTo(0,55,18,15,50,18);palette.cubicTo(83,18,93,45,73,55);palette.cubicTo(62,57,67,66,61,74);palette.cubicTo(53,84,35,81,27,73);metal(c,palette,tint);for(float[] point:new float[][]{{33,34},{51,29},{68,39},{28,53}}){p.setColor(mix(tint,0xff271142,.68f));c.drawCircle(point[0],point[1],5,p);p.setColor(0xfff6d7ff);c.drawCircle(point[0]-1,point[1]-1,2.3f,p);}break;
                case "info":ball(c,50,48,29,tint);metal(c,rounded(46,43,54,66,3),0xfff5ecd1);ball(c,50,33,4.5f,0xfffff1bf);break;
                case "moon":Path moon=new Path();moon.addCircle(48,48,29,Path.Direction.CW);Path bite=new Path();bite.addCircle(62,36,26,Path.Direction.CW);moon.op(bite,Path.Op.DIFFERENCE);metal(c,moon,0xffb7cbdc);break;
                case "sun":for(int n=0;n<8;n++){double a=n*Math.PI/4;line(c,50+(float)Math.cos(a)*29,48+(float)Math.sin(a)*29,50+(float)Math.cos(a)*36,48+(float)Math.sin(a)*36,0xffffc843,2.8f);}ball(c,50,48,21,0xffffb826);break;
                case "leaf":Path leaf=new Path();leaf.moveTo(24,76);leaf.cubicTo(14,26,57,16,83,17);leaf.cubicTo(84,52,61,82,24,76);leaf.close();metal(c,leaf,0xffa9ed36);line(c,25,75,70,30,0xffefffc9,2.2f);line(c,42,58,39,41,0xffefffc9,1.5f);line(c,54,47,70,48,0xffefffc9,1.5f);break;
                case "gem":Path gem=new Path();gem.moveTo(30,22);gem.lineTo(70,22);gem.lineTo(84,42);gem.lineTo(50,79);gem.lineTo(16,42);gem.close();metal(c,gem,0xffa362e9);line(c,30,23,50,79,0xfff0dbff,1.2f);line(c,70,23,50,79,0xfff0dbff,1.2f);line(c,17,42,83,42,0xfff0dbff,1.2f);break;
                default:c.save();c.translate(16,17);fallback.layout(0,0,68,68);fallback.draw(c);c.restore();
            }
        }
        private RectF roundedRect(float l,float t,float r,float b){return new RectF(l,t,r,b);}
        private void orbit(Canvas c,boolean front){p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);c.save();c.rotate(-18,50,53);RectF oval=new RectF(5,52,96,79);float start=front?7:184,sweep=front?167:164;for(int i=4;i>=1;i--){p.setStrokeWidth(i*2.2f);p.setColor((i==1?0xee000000:0x16000000)|(tint&0xffffff));c.drawArc(oval,start,sweep,false,p);}p.setStrokeWidth(.9f);p.setColor(0xffe7ffff);c.drawArc(oval,start,sweep,false,p);c.restore();p.setStyle(Paint.Style.FILL);}
        private void spark(Canvas c,float x,float y,float r,int colour){p.setColor(colour);Path q=new Path();q.moveTo(x,y-r);q.quadTo(x+r*.18f,y-r*.18f,x+r,y);q.quadTo(x+r*.18f,y+r*.18f,x,y+r);q.quadTo(x-r*.18f,y+r*.18f,x-r,y);q.quadTo(x-r*.18f,y-r*.18f,x,y-r);c.drawPath(q,p);}
        @Override protected void onDraw(Canvas c){float size=Math.min(getWidth(),getHeight());c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/100,size/100);ThemeSkin skin=ThemeSkin.active;
            if(boxed){p.setShader(new LinearGradient(0,0,100,100,new int[]{mix(tint,skin.top,.75f),skin.bottom},null,Shader.TileMode.CLAMP));c.drawRoundRect(1,1,99,99,23,23,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(tint);c.drawRoundRect(1,1,99,99,23,23,p);p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,0,0,48,new int[]{0x75ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));c.drawRoundRect(3,3,97,48,20,20,p);p.setShader(null);}
            orbit(c,false);glyph(c);orbit(c,true);spark(c,85,18,8,boxed?tint:0xffffd434);spark(c,94,29,3,0xffe7fcff);c.restore();
        }
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

