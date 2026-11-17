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
    static final int LEFT=31,TOP=106,RIGHT=910,BOTTOM=1564;
    private static Bitmap artwork;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Rect source;
    private final RectF destination=new RectF();
    private final int[] edges=HomeGeometry.EDGES;
    private float[] screenEdges;
    private float artworkScale;
    private HomeGeometry geometry;
    private final View[] targets=new View[6];
    private final RectF[] boxes={new RectF(80,470,461,828),new RectF(480,470,861,828),new RectF(80,841,461,1199),new RectF(480,841,861,1199),new RectF(143,164,801,350),new RectF(224,1255,716,1408)};
    private final boolean dutch;private final String[] labels;private final String name;
    private final Bitmap[] localizedBands;
    ReferenceHome(Context context,String language,String[] labels,String name,Runnable[] actions){super(context);setTag("reference-home");setWillNotDraw(false);setLayerType(View.LAYER_TYPE_SOFTWARE,null);this.labels=labels;this.name=name;dutch=language.equals("nl");
        synchronized(ReferenceHome.class){if(artwork==null)try(InputStream in=context.getAssets().open("home-reference-2-3-2.png")){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;artwork=BitmapFactory.decodeStream(in,null,options);if(artwork==null)throw new IOException("Artwork");}catch(IOException error){throw new IllegalStateException("Home artwork missing",error);}}
        source=new Rect(LEFT,TOP,RIGHT,BOTTOM);localizedBands=dutch?null:new Bitmap[5];
        String[] tags={"qr","photo","list","settings","home-identity","home-brand"};
        for(int i=0;i<targets.length;i++){final int index=i;View v;if(i<4){Button button=new Button(context);button.setAllCaps(false);button.setText("");button.setPadding(0,0,0,0);button.setMinimumWidth(0);button.setMinimumHeight(0);GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(22*getResources().getDisplayMetrics().density);button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x44ffffff),null,mask));button.setStateListAnimator(null);button.setElevation(0);button.setContentDescription(labels[i]);button.setOnClickListener(view->actions[index].run());v=button;}else{v=new View(context);v.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);v.setContentDescription(i==4?"BMD "+name:"Blue Medical, A Translumina Group Company");}v.setTag(tags[i]);targets[i]=v;addView(v,new FrameLayout.LayoutParams(1,1));}
        setBackgroundColor(MainActivity.NAVY);
    }
    private void fit(int width,int height){geometry=new HomeGeometry(width,height);artworkScale=geometry.scale;screenEdges=geometry.rows;destination.set(geometry.left,0,geometry.right,height);}
    private float screenY(float y){return geometry.y(y);}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){fit(r-l,b-t);for(int i=0;i<targets.length;i++){RectF box=boxes[i];targets[i].layout(Math.round(destination.left+(box.left-LEFT)*artworkScale),Math.round(screenY(box.top)),Math.round(destination.left+(box.right-LEFT)*artworkScale),Math.round(screenY(box.bottom)));}}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);fit(getWidth(),getHeight());
        Rect[] bands={new Rect(132,672,439,752),new Rect(500,672,848,752),new Rect(120,1050,441,1130),new Rect(518,1050,851,1130),new Rect(329,285,796,350)};
        for(int segment=0;segment<7;segment++){
            Rect src=new Rect(LEFT,edges[segment],RIGHT,edges[segment+1]);
            RectF dst=new RectF(destination.left,screenEdges[segment],destination.right,screenEdges[segment+1]);
            canvas.drawBitmap(artwork,src,dst,paint);
            if(!dutch){canvas.save();canvas.clipRect(dst);canvas.translate(dst.left,dst.top);canvas.scale(dst.width()/src.width(),dst.height()/src.height());canvas.translate(-LEFT,-src.top);
                for(int i=0;i<bands.length;i++){Rect band=bands[i];if(band.top>=src.top&&band.bottom<=src.bottom){if(localizedBands[i]==null)localizedBands[i]=backgroundBand(band);canvas.drawBitmap(localizedBands[i],null,new RectF(band),paint);drawLabel(canvas,band,i<4?labels[i]:name,i==4);}}
                canvas.restore();}
        }
    }
    private Bitmap backgroundBand(Rect rect){int w=128,h=48;Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);int[] colors={artwork.getPixel(rect.left,rect.top),artwork.getPixel(rect.right,rect.top),artwork.getPixel(rect.left,rect.bottom),artwork.getPixel(rect.right,rect.bottom)};for(int y=0;y<h;y++)for(int x=0;x<w;x++){float fx=x/(float)(w-1),fy=y/(float)(h-1);int c=Color.rgb(channel(colors,16,fx,fy),channel(colors,8,fx,fy),channel(colors,0,fx,fy));bitmap.setPixel(x,y,c);}return bitmap;}
    private int channel(int[] c,int shift,float x,float y){return Math.round((((c[0]>>shift)&255)*(1-x)+((c[1]>>shift)&255)*x)*(1-y)+(((c[2]>>shift)&255)*(1-x)+((c[3]>>shift)&255)*x)*y);}
    private void drawLabel(Canvas canvas,Rect rect,String text,boolean title){TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setColor(title?0xffa6e7ff:Color.WHITE);p.setTypeface(Typeface.create("sans-serif",title?Typeface.BOLD_ITALIC:Typeface.BOLD));p.setShadowLayer(3,1,3,0xaa000000);StaticLayout layout=null;for(int size=title?62:46;size>=24;size--){p.setTextSize(size);layout=StaticLayout.Builder.obtain(text,0,text.length(),p,rect.width()).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build();if(layout.getHeight()<=rect.height()-8&&layout.getLineCount()<=2)break;}if(layout!=null){canvas.save();canvas.translate(rect.left,rect.top+(rect.height()-layout.getHeight())/2f);layout.draw(canvas);canvas.restore();}}
}

