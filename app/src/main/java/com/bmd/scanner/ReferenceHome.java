package com.bmd.scanner;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;

/** Original approved artwork with real, accessible Android controls aligned to its tiles. */
final class ReferenceHome extends FrameLayout {
    static final int TOP=56,BOTTOM=1612;
    private static Bitmap artwork;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Rect source;
    private final RectF[] destinations={new RectF(),new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};
    private final View[] targets=new View[6];
    private final RectF[] boxes={new RectF(53,475,470,884),new RectF(470,475,887,884),new RectF(53,884,470,1293),new RectF(470,884,887,1293),new RectF(143,164,801,380),new RectF(275,1347,665,1480)};
    private final boolean dutch;private final String[] labels;private final String name;
    private final Bitmap[] localizedBands;
    ReferenceHome(Context context,String language,String[] labels,String name,Runnable[] actions){super(context);setTag("reference-home");setWillNotDraw(false);setLayerType(View.LAYER_TYPE_SOFTWARE,null);this.labels=labels;this.name=name;dutch=language.equals("nl");
        synchronized(ReferenceHome.class){if(artwork==null)try(InputStream in=context.getAssets().open("home-reference-2-3.png")){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;artwork=BitmapFactory.decodeStream(in,null,options);if(artwork==null)throw new IOException("Artwork");}catch(IOException error){throw new IllegalStateException("Home artwork missing",error);}}
        source=new Rect(0,TOP,artwork.getWidth(),BOTTOM);localizedBands=dutch?null:new Bitmap[5];
        String[] tags={"qr","photo","list","settings","home-identity","home-brand"};
        for(int i=0;i<targets.length;i++){final int index=i;View v;if(i<4){Button button=new Button(context);button.setAllCaps(false);button.setText("");button.setPadding(0,0,0,0);button.setMinimumWidth(0);button.setMinimumHeight(0);GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(22*getResources().getDisplayMetrics().density);button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x44ffffff),null,mask));button.setStateListAnimator(null);button.setElevation(0);button.setContentDescription(labels[i]);button.setOnClickListener(view->actions[index].run());v=button;}else{v=new View(context);v.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);v.setContentDescription(i==4?"BMD "+name:"Blue Medical, A Translumina Group Company");}v.setTag(tags[i]);targets[i]=v;addView(v,new FrameLayout.LayoutParams(1,1));}
        setBackgroundColor(MainActivity.NAVY);
    }
    private float dp(float value){return value*getResources().getDisplayMetrics().density;}
    private void fit(int width,int height){
        float margin=dp(12),gap=dp(4),top=dp(18),bottom=dp(18);
        float heroW=Math.min(width*.80f,(height*.20f)*658/216f),heroH=heroW*216/658f;
        float brandW=Math.min(width*.40f,(height*.09f)*390/133f),brandH=brandW*133/390f;
        destinations[4].set((width-heroW)/2,top,(width+heroW)/2,top+heroH);
        destinations[5].set((width-brandW)/2,height-bottom-brandH,(width+brandW)/2,height-bottom);
        float gridTop=destinations[4].bottom+dp(24),gridBottom=destinations[5].top-dp(24);
        float tileW=Math.max(1,Math.min((width-2*margin-gap)/2,((gridBottom-gridTop-gap)/2)*417/409f));
        float tileH=tileW*409/417f,gridW=tileW*2+gap,gridH=tileH*2+gap;
        float left=(width-gridW)/2,y=gridTop+(gridBottom-gridTop-gridH)/2;
        for(int i=0;i<4;i++){float x=left+(i%2)*(tileW+gap),ty=y+(i/2)*(tileH+gap);destinations[i].set(x,ty,x+tileW,ty+tileH);}
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){fit(r-l,b-t);for(int i=0;i<targets.length;i++){RectF d=destinations[i];targets[i].layout(Math.round(d.left),Math.round(d.top),Math.round(d.right),Math.round(d.bottom));}}
    private void wave(Canvas canvas,float y){
        Path path=new Path();path.moveTo(-10,y+dp(5));path.cubicTo(getWidth()*.25f,y-dp(20),getWidth()*.65f,y+dp(22),getWidth()+10,y-dp(3));
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setColor(0xff00dcff);paint.setShadowLayer(dp(5),0,0,0xff008cff);canvas.drawPath(path,paint);paint.clearShadowLayer();paint.setStyle(Paint.Style.FILL);
    }
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);fit(getWidth(),getHeight());
        paint.setShader(new LinearGradient(0,0,getWidth(),getHeight(),new int[]{0xff001020,0xff002b4b,0xff001024},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,getWidth(),getHeight(),paint);paint.setShader(null);
        wave(canvas,destinations[4].bottom+dp(10));wave(canvas,getHeight()-dp(5));
        Rect[] bands={new Rect(125,708,434,794),new Rect(498,708,861,794),new Rect(101,1110,437,1202),new Rect(508,1110,855,1202),new Rect(327,303,799,379)};
        for(int i=0;i<6;i++){RectF box=boxes[i],d=destinations[i];canvas.save();canvas.clipRect(d);canvas.translate(d.left,d.top);canvas.scale(d.width()/box.width(),d.height()/box.height());canvas.translate(-box.left,-box.top);canvas.drawBitmap(artwork,0,0,paint);
            if(!dutch&&i<5){Rect band=bands[i];if(localizedBands[i]==null)localizedBands[i]=backgroundBand(band);canvas.drawBitmap(localizedBands[i],null,new RectF(band),paint);drawLabel(canvas,band,i<4?labels[i]:name,i==4);}canvas.restore();}
    }
    private Bitmap backgroundBand(Rect rect){int w=128,h=48;Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);int[] colors={artwork.getPixel(rect.left,rect.top),artwork.getPixel(rect.right,rect.top),artwork.getPixel(rect.left,rect.bottom),artwork.getPixel(rect.right,rect.bottom)};for(int y=0;y<h;y++)for(int x=0;x<w;x++){float fx=x/(float)(w-1),fy=y/(float)(h-1);int c=Color.rgb(channel(colors,16,fx,fy),channel(colors,8,fx,fy),channel(colors,0,fx,fy));bitmap.setPixel(x,y,c);}return bitmap;}
    private int channel(int[] c,int shift,float x,float y){return Math.round((((c[0]>>shift)&255)*(1-x)+((c[1]>>shift)&255)*x)*(1-y)+(((c[2]>>shift)&255)*(1-x)+((c[3]>>shift)&255)*x)*y);}
    private void drawLabel(Canvas canvas,Rect rect,String text,boolean title){TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setColor(title?0xffa6e7ff:Color.WHITE);p.setTypeface(Typeface.create("sans-serif",title?Typeface.BOLD_ITALIC:Typeface.BOLD));p.setShadowLayer(3,1,3,0xaa000000);StaticLayout layout=null;for(int size=title?62:46;size>=24;size--){p.setTextSize(size);layout=StaticLayout.Builder.obtain(text,0,text.length(),p,rect.width()).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build();if(layout.getHeight()<=rect.height()-8&&layout.getLineCount()<=2)break;}if(layout!=null){canvas.save();canvas.translate(rect.left,rect.top+(rect.height()-layout.getHeight())/2f);layout.draw(canvas);canvas.restore();}}
}

