package com.bmd.scanner;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.HashMap;

/** Production reference-style artwork. Text and every interactive control stay native. */
final class ReferenceArt {
    private static final HashMap<Integer,Bitmap> images=new HashMap<>();
    private static final HashMap<String,Typeface> fonts=new HashMap<>();
    private static final HashMap<String,Bitmap> scannerColours=new HashMap<>();
    static final int[] HOME={R.drawable.ref_home_scan,R.drawable.ref_home_customers,R.drawable.ref_home_instructions,R.drawable.ref_home_settings};
    static synchronized Bitmap bitmap(Context context,int resource){Bitmap cached=images.get(resource);if(cached==null){cached=BitmapFactory.decodeResource(context.getResources(),resource);if(cached==null)throw new IllegalStateException("Bundled reference artwork missing");images.put(resource,cached);}return cached;}
    static synchronized Typeface face(Context context,boolean italic,int weight){String key=italic+":"+weight;Typeface font=fonts.get(key);if(font==null){font=new Typeface.Builder(context.getAssets(),italic?"fonts/nunito_italic.ttf":"fonts/nunito_regular.ttf").setFontVariationSettings("'wght' "+weight).setWeight(weight).setItalic(italic).build();fonts.put(key,font);}return font;}
    static synchronized Typeface magicFace(Context context,String text){
        // Devanagari uses the system shaping/font fallback; Latin and Cyrillic use the bundled script.
        if(text.codePoints().anyMatch(c->(c>=0x600&&c<=0x6ff)||(c>=0x900&&c<=0x97f)))return face(context,true,850);
        Typeface font=fonts.get("magic");if(font==null){font=Typeface.createFromAsset(context.getAssets(),"fonts/lobster.ttf");fonts.put("magic",font);}return font;
    }
    static synchronized Bitmap scanner(Context context){ThemeSkin skin=ThemeSkin.active;if(skin==ThemeSkin.ORANGE||skin==ThemeSkin.GOLD||skin==ThemeSkin.PINK){Bitmap cached=scannerColours.get(skin.id);if(cached!=null)return cached;Bitmap source=bitmap(context,R.drawable.launcher_foreground);int w=source.getWidth(),h=source.getHeight();int[] pixels=new int[w*h];source.getPixels(pixels,0,w,0,0,w,h);float[] hsv=new float[3];float hue=skin==ThemeSkin.ORANGE?28:skin==ThemeSkin.GOLD?46:330;for(int i=0;i<pixels.length;i++){int colour=pixels[i];if(Color.alpha(colour)==0)continue;Color.colorToHSV(colour,hsv);if(hsv[1]>.2f&&hsv[0]>=150&&hsv[0]<=260){hsv[0]=(hue+(hsv[0]-210)*.25f+360)%360;pixels[i]=Color.HSVToColor(Color.alpha(colour),hsv);}}cached=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);scannerColours.put(skin.id,cached);return cached;}int resource=skin==ThemeSkin.LIME?R.drawable.ref_scanner_lime:skin==ThemeSkin.VIOLET?R.drawable.ref_scanner_violet:skin==ThemeSkin.TRANSLUMINA?R.drawable.ref_scanner_red:skin==ThemeSkin.DARK?R.drawable.ref_scanner_dark:skin==ThemeSkin.LIGHT?R.drawable.ref_scanner_light:R.drawable.launcher_foreground;return bitmap(context,resource);}
    static void homeTileIcon(Canvas canvas,Context context,int index,boolean directControl,RectF bounds,int colour){
        if(index==0&&!directControl){icon(canvas,context,index,bounds);return;}
        int resource=directControl&&index==0?R.drawable.ref_scan_final_check:HOME[index];
        OrbitalArtwork.draw(canvas,context,resource,bounds,OrbitalArtwork.surfaceInk(colour));
    }
    static void icon(Canvas canvas,Context context,int index,RectF bounds){Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);canvas.drawBitmap(bitmap(context,HOME[index]),null,bounds,paint);}
    static void qrIcon(Canvas canvas,Context context,RectF bounds){Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);canvas.drawBitmap(bitmap(context,R.drawable.ref_home_qr),null,bounds,paint);}
    static void scanMenuIcon(Canvas canvas,Context context,int index,RectF bounds){int[] resources={R.drawable.ref_scan_final_check,R.drawable.ref_scan_pallet_orange,R.drawable.ref_scan_document_blue,R.drawable.ref_scan_qr_purple};Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);canvas.drawBitmap(bitmap(context,resources[index]),null,bounds,paint);}
    static int curveInk(ThemeSkin skin){return skin==ThemeSkin.LIME?0xffd4ff72:skin==ThemeSkin.VIOLET?0xffdda4ff:skin==ThemeSkin.TRANSLUMINA?0xfffff1eb:skin==ThemeSkin.DARK?0xfff1f6fb:skin==ThemeSkin.LIGHT?0xff00c9e8:skin==ThemeSkin.BMD?0xff00e9ff:UiArt.mix(skin.accent,Color.WHITE,.28f);}
    private static int glowInk(ThemeSkin skin){return skin==ThemeSkin.LIME?0xff91ff00:skin==ThemeSkin.VIOLET?0xffa22bff:skin==ThemeSkin.TRANSLUMINA?0xffff1636:skin==ThemeSkin.DARK?0xffa5b9c8:skin==ThemeSkin.BMD||skin==ThemeSkin.LIGHT?0xff00bfff:skin.accent;}
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
        int base=skin==ThemeSkin.BMD?0xff00172e:skin==ThemeSkin.TRANSLUMINA?0xff300007:skin==ThemeSkin.DARK?0xff111518:skin==ThemeSkin.LIME?0xff061e09:skin==ThemeSkin.VIOLET?0xff19082c:skin==ThemeSkin.LIGHT?0xfff6faff:skin.background;
        int halo=skin==ThemeSkin.BMD?0xff052f53:skin==ThemeSkin.TRANSLUMINA?0xffa50019:skin==ThemeSkin.DARK?0xff343c43:skin==ThemeSkin.LIME?0xff2c5c11:skin==ThemeSkin.VIOLET?0xff57216f:skin==ThemeSkin.LIGHT?0xffe4f1fb:skin.halo;
        paint.setShader(new LinearGradient(0,0,width,height,new int[]{base,halo,base},new float[]{0,.48f,1},Shader.TileMode.CLAMP));canvas.drawRect(0,0,width,height,paint);
        paint.setShader(new RadialGradient(width*.86f,height*.15f,width*.95f,new int[]{skin==ThemeSkin.LIGHT?0x70ffffff:skin==ThemeSkin.TRANSLUMINA?0x24ffb8b4:skin==ThemeSkin.DARK?0x18ffffff:skin==ThemeSkin.LIME?0x208aff33:skin==ThemeSkin.VIOLET?0x20c17aff:skin==ThemeSkin.BMD?0x124e9bce:0x20000000|(skin.accent&0xffffff),0},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,width,height,paint);paint.setShader(null);
        Path fold=new Path();fold.moveTo(width*.20f,0);fold.cubicTo(width*.53f,height*.26f,width*.77f,height*.13f,width*1.1f,height*.055f);fold.lineTo(width*1.1f,0);fold.close();paint.setShader(new LinearGradient(width*.5f,0,width,height*.22f,new int[]{0,skin==ThemeSkin.LIGHT?0x90ffffff:skin==ThemeSkin.BMD?0x03ffffff:0x08ffffff},null,Shader.TileMode.CLAMP));canvas.drawPath(fold,paint);paint.setShader(null);
        ribbon(canvas,width,height*.947f,skin,false);
    }
    static Drawable tile(int requestedTop,int requestedBottom,int requestedEdge,float radius){
        return tile(requestedTop,requestedBottom,requestedEdge,radius,false);
    }
    static Drawable homeTile(int top,int bottom,int edge,float radius){
        return tile(top,bottom,edge,radius,true);
    }
    private static Drawable tile(int requestedTop,int requestedBottom,int requestedEdge,float radius,boolean keepTileColours){
        final ThemeSkin skin=ThemeSkin.active;final boolean coloured=keepTileColours||skin==ThemeSkin.BMD||skin==ThemeSkin.TRANSLUMINA;
        final int top=coloured?requestedTop:skin==ThemeSkin.DARK?0xff41464d:skin==ThemeSkin.LIME?0xff426d19:skin==ThemeSkin.VIOLET?0xff653589:skin==ThemeSkin.LIGHT?0xffffffff:skin.top;
        final int bottom=coloured?requestedBottom:skin==ThemeSkin.DARK?0xff181d23:skin==ThemeSkin.LIME?0xff102c0a:skin==ThemeSkin.VIOLET?0xff260d3c:skin==ThemeSkin.LIGHT?UiArt.mix(requestedEdge,0xffe9f1f9,.88f):skin.bottom;
        final int edge=coloured?requestedEdge:skin==ThemeSkin.DARK?0xffc7d0d8:skin==ThemeSkin.LIME?0xffc7ff50:skin==ThemeSkin.VIOLET?0xffd39aff:skin==ThemeSkin.LIGHT?UiArt.mix(requestedEdge,Color.WHITE,.45f):skin.accent;
        return new Drawable(){final Paint p=new Paint(3);
            @Override public void draw(Canvas c){Rect b=getBounds();float w=b.width(),h=b.height(),u=w/392f,depth=Math.min(8*u,h*.04f),rr=Math.min(radius,Math.min(w,h)*.15f);c.save();c.translate(b.left,b.top);
                RectF face=new RectF(2*u,2*u,w-2*u,h-depth-2*u);p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(skin==ThemeSkin.LIGHT?0xffbac9d7:0xff000b14);c.drawRoundRect(2*u,7*u,w-2*u,h-1*u,rr,rr,p);p.setStyle(Paint.Style.STROKE);int bloom=coloured?requestedTop:edge;for(int n=3;n>=1;n--){p.setStrokeWidth((3+n*3)*u);p.setColor((Math.round(52f/(n+1))<<24)|(bloom&0xffffff));c.drawRoundRect(face,rr,rr,p);}p.setStyle(Paint.Style.FILL);p.setAlpha(255);
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


