package com.bmd.scanner;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.HashMap;

/** Production reference-style artwork. Text and every interactive control stay native. */
final class ReferenceArt {
    private static final HashMap<Integer,Bitmap> images=new HashMap<>();
    private static final HashMap<String,Bitmap> themedImages=new HashMap<>();
    private static final HashMap<String,Typeface> fonts=new HashMap<>();
    static final int[] HOME={R.drawable.ref_home_qr,R.drawable.ref_home_customers,R.drawable.ref_home_instructions,R.drawable.ref_home_settings};
    static synchronized Bitmap bitmap(Context context,int resource){Bitmap cached=images.get(resource);if(cached==null){cached=BitmapFactory.decodeResource(context.getResources(),resource);if(cached==null)throw new IllegalStateException("Bundled reference artwork missing");images.put(resource,cached);}return cached;}
    static synchronized Typeface face(Context context,boolean italic,int weight){String key=italic+":"+weight;Typeface font=fonts.get(key);if(font==null){font=new Typeface.Builder(context.getAssets(),italic?"fonts/nunito_italic.ttf":"fonts/nunito_regular.ttf").setFontVariationSettings("'wght' "+weight).setWeight(weight).setItalic(italic).build();fonts.put(key,font);}return font;}
    static Bitmap scanner(Context context){int resource=ThemeSkin.active==ThemeSkin.LIME?R.drawable.ref_scanner_lime:ThemeSkin.active==ThemeSkin.VIOLET?R.drawable.ref_scanner_violet:ThemeSkin.active==ThemeSkin.TRANSLUMINA?R.drawable.ref_scanner_red:ThemeSkin.active==ThemeSkin.DARK?R.drawable.ref_scanner_dark:ThemeSkin.active==ThemeSkin.LIGHT?R.drawable.ref_scanner_light:R.drawable.launcher_foreground;return bitmap(context,resource);}
    private static synchronized Bitmap home(Context context,int index){Bitmap base=bitmap(context,HOME[index]);ThemeSkin skin=ThemeSkin.active;if(skin!=ThemeSkin.LIME&&skin!=ThemeSkin.VIOLET)return base;String key=skin.id+":"+index;Bitmap cached=themedImages.get(key);if(cached!=null)return cached;int w=base.getWidth(),h=base.getHeight();int[] pixels=new int[w*h];base.getPixels(pixels,0,w,0,0,w,h);float[] hsv=new float[3];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int n=y*w+x,colour=pixels[n];if(Color.alpha(colour)<=8)continue;Color.colorToHSV(colour,hsv);if(hsv[1]>.23f&&!(y<h*.26f&&hsv[0]>20&&hsv[0]<70)){hsv[0]=skin==ThemeSkin.LIME?85:274;hsv[1]=Math.min(.95f,hsv[1]*.95f);pixels[n]=Color.HSVToColor(Color.alpha(colour),hsv);}}cached=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);themedImages.put(key,cached);return cached;}
    static void icon(Canvas canvas,Context context,int index,RectF bounds){Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);canvas.drawBitmap(home(context,index),null,bounds,paint);}
    static int curveInk(ThemeSkin skin){return skin==ThemeSkin.LIME?0xffd4ff72:skin==ThemeSkin.VIOLET?0xffdda4ff:skin==ThemeSkin.TRANSLUMINA?0xfffff1eb:skin==ThemeSkin.DARK?0xfff1f6fb:skin==ThemeSkin.LIGHT?0xff00c9e8:0xff00e9ff;}
    private static int glowInk(ThemeSkin skin){return skin==ThemeSkin.LIME?0xff91ff00:skin==ThemeSkin.VIOLET?0xffa22bff:skin==ThemeSkin.TRANSLUMINA?0xffff1636:skin==ThemeSkin.DARK?0xffa5b9c8:0xff00bfff;}
    static void ribbon(Canvas canvas,float width,float y,ThemeSkin skin,boolean header){
        float unit=width/868f;Paint paint=new Paint(3);Path curve=new Path();
        if(header){curve.moveTo(-24*unit,y+width*.035f);curve.cubicTo(width*.23f,y-width*.065f,width*.39f,y+width*.037f,width*.64f,y+width*.013f);curve.cubicTo(width*.81f,y+width*.006f,width*.94f,y-width*.025f,width+24*unit,y-width*.055f);}
        else{curve.moveTo(-24*unit,y+width*.025f);curve.cubicTo(width*.22f,y-width*.065f,width*.47f,y+width*.072f,width*.72f,y+width*.028f);curve.cubicTo(width*.86f,y+width*.009f,width*.95f,y-width*.007f,width+24*unit,y-width*.030f);}
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);int glow=glowInk(skin);
        for(int n=6;n>=1;n--){paint.setStrokeWidth((2+n*3.5f)*unit);paint.setColor((Math.round(60f/(n+1))<<24)|(glow&0xffffff));canvas.drawPath(curve,paint);}
        canvas.save();canvas.translate(0,-6*unit);paint.setStrokeWidth(11*unit);paint.setShader(new LinearGradient(0,y,width,y,new int[]{0x00ffffff,skin==ThemeSkin.LIGHT?0x60ffffff:0x22ffffff,0x08ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));canvas.drawPath(curve,paint);canvas.restore();paint.setShader(null);
        paint.setStrokeWidth(4.5f*unit);paint.setColor(curveInk(skin));canvas.drawPath(curve,paint);
        paint.setStrokeWidth(1.5f*unit);paint.setShader(new LinearGradient(0,y,width,y,new int[]{0xffe7ffff,curveInk(skin),0xffffffff},new float[]{0,.65f,1},Shader.TileMode.CLAMP));canvas.drawPath(curve,paint);paint.setShader(null);
    }
    static void background(Canvas canvas,float width,float height){ThemeSkin skin=ThemeSkin.active;Paint paint=new Paint(3);
        int base=skin==ThemeSkin.BMD?0xff00172e:skin==ThemeSkin.TRANSLUMINA?0xff300007:skin==ThemeSkin.DARK?0xff111518:skin==ThemeSkin.LIME?0xff061e09:skin==ThemeSkin.VIOLET?0xff19082c:0xfff6faff;
        int halo=skin==ThemeSkin.BMD?0xff052f53:skin==ThemeSkin.TRANSLUMINA?0xffa50019:skin==ThemeSkin.DARK?0xff343c43:skin==ThemeSkin.LIME?0xff2c5c11:skin==ThemeSkin.VIOLET?0xff57216f:0xffe4f1fb;
        paint.setShader(new LinearGradient(0,0,width,height,new int[]{base,halo,base},new float[]{0,.48f,1},Shader.TileMode.CLAMP));canvas.drawRect(0,0,width,height,paint);
        paint.setShader(new RadialGradient(width*.86f,height*.15f,width*.95f,new int[]{skin==ThemeSkin.LIGHT?0x70ffffff:skin==ThemeSkin.TRANSLUMINA?0x24ffb8b4:skin==ThemeSkin.DARK?0x18ffffff:skin==ThemeSkin.LIME?0x208aff33:skin==ThemeSkin.VIOLET?0x20c17aff:0x124e9bce,0},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,width,height,paint);paint.setShader(null);
        Path fold=new Path();fold.moveTo(width*.20f,0);fold.cubicTo(width*.53f,height*.26f,width*.77f,height*.13f,width*1.1f,height*.055f);fold.lineTo(width*1.1f,0);fold.close();paint.setShader(new LinearGradient(width*.5f,0,width,height*.22f,new int[]{0,skin==ThemeSkin.LIGHT?0x90ffffff:skin==ThemeSkin.BMD?0x03ffffff:0x08ffffff},null,Shader.TileMode.CLAMP));canvas.drawPath(fold,paint);paint.setShader(null);
        ribbon(canvas,width,height*.947f,skin,false);
    }
    static Drawable tile(int requestedTop,int requestedBottom,int requestedEdge,float radius){
        final ThemeSkin skin=ThemeSkin.active;final boolean coloured=skin==ThemeSkin.BMD||skin==ThemeSkin.TRANSLUMINA;
        final int top=coloured?requestedTop:skin==ThemeSkin.DARK?0xff41464d:skin==ThemeSkin.LIME?0xff426d19:skin==ThemeSkin.VIOLET?0xff653589:0xffffffff;
        final int bottom=coloured?requestedBottom:skin==ThemeSkin.DARK?0xff181d23:skin==ThemeSkin.LIME?0xff102c0a:skin==ThemeSkin.VIOLET?0xff260d3c:UiArt.mix(requestedEdge,0xffe9f1f9,.88f);
        final int edge=coloured?requestedEdge:skin==ThemeSkin.DARK?0xffc7d0d8:skin==ThemeSkin.LIME?0xffc7ff50:skin==ThemeSkin.VIOLET?0xffd39aff:UiArt.mix(requestedEdge,Color.WHITE,.45f);
        return new Drawable(){final Paint p=new Paint(3);
            @Override public void draw(Canvas c){Rect b=getBounds();float w=b.width(),h=b.height(),u=w/392f,depth=Math.min(8*u,h*.04f),rr=Math.min(radius,Math.min(w,h)*.15f);c.save();c.translate(b.left,b.top);
                RectF face=new RectF(2*u,2*u,w-2*u,h-depth-2*u);p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(skin==ThemeSkin.LIGHT?0xffbac9d7:0xff000b14);c.drawRoundRect(2*u,7*u,w-2*u,h-1*u,rr,rr,p);p.setStyle(Paint.Style.STROKE);int bloom=coloured?requestedTop:edge;for(int n=3;n>=1;n--){p.setStrokeWidth((3+n*3)*u);p.setColor((Math.round(52f/(n+1))<<24)|(bloom&0xffffff));c.drawRoundRect(face,rr,rr,p);}p.setStyle(Paint.Style.FILL);
                int dark=skin==ThemeSkin.LIGHT?UiArt.mix(bottom,0xffafc4d4,.20f):UiArt.mix(bottom,0xff020c15,coloured?.72f:.46f);
                p.setShader(new LinearGradient(0,0,w,h,new int[]{top,bottom,dark},new float[]{0,.57f,1},Shader.TileMode.CLAMP));c.drawRoundRect(face,rr,rr,p);p.setShader(null);
                int save=c.save();Path clip=new Path();clip.addRoundRect(face,rr,rr,Path.Direction.CW);c.clipPath(clip);
                p.setShader(new RadialGradient(w*.11f,h*.07f,w*.83f,new int[]{skin==ThemeSkin.LIGHT?0x70ffffff:0x45ffffff,0},null,Shader.TileMode.CLAMP));c.drawRect(face,p);p.setShader(null);
                Path gloss=new Path();gloss.moveTo(0,0);gloss.lineTo(w,0);gloss.lineTo(w,h*.105f);gloss.cubicTo(w*.78f,h*.19f,w*.16f,h*.17f,0,h*.43f);gloss.close();p.setShader(new LinearGradient(0,0,0,h*.48f,new int[]{0xb5ffffff,0x2cffffff,0x00ffffff},new float[]{0,.30f,1},Shader.TileMode.CLAMP));c.drawPath(gloss,p);p.setShader(null);
                p.setStyle(Paint.Style.STROKE);RectF rim=new RectF(6*u,6*u,w-6*u,h-depth-5*u);for(int n=4;n>=1;n--){p.setStrokeWidth((n*2+1)*u);p.setColor((n==1?0xce000000:0x16000000)|(edge&0xffffff));c.drawRoundRect(rim,rr-4*u,rr-4*u,p);}
                p.setStrokeWidth(2.2f*u);p.setShader(new LinearGradient(0,0,w,h,new int[]{Color.WHITE,edge,UiArt.mix(edge,skin==ThemeSkin.LIGHT?Color.WHITE:bottom,.65f)},new float[]{0,.38f,1},Shader.TileMode.CLAMP));c.drawRoundRect(rim,rr-4*u,rr-4*u,p);p.setShader(null);c.restoreToCount(save);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f*u);p.setShader(new LinearGradient(0,0,w,h,new int[]{Color.WHITE,edge,skin==ThemeSkin.LIGHT?0xffb9cad8:UiArt.mix(edge,bottom,.7f)},new float[]{0,.45f,1},Shader.TileMode.CLAMP));c.drawRoundRect(face,rr,rr,p);p.setShader(null);
                Path highlight=new Path();highlight.moveTo(10*u,rr);highlight.quadTo(10*u,10*u,rr,10*u);highlight.lineTo(w*.72f,10*u);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(3*u);p.setColor(0xdcffffff);c.drawPath(highlight,p);
                p.setStyle(Paint.Style.FILL);c.restore();
            }
            @Override public void setAlpha(int alpha){p.setAlpha(alpha);}@Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}@Override public void getOutline(Outline outline){outline.setRoundRect(getBounds(),radius);}
        };
    }
}

