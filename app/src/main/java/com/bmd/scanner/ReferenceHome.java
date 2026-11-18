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
    static final int LEFT=0,TOP=0,RIGHT=868,BOTTOM=1812;
    private static Bitmap artwork;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Rect source;
    private final RectF destination=new RectF();
    private final int[] edges=HomeGeometry.EDGES;
    private float[] screenEdges;
    private float artworkScale;
    private HomeGeometry geometry;
    private final View[] targets=new View[6];
    private final RectF[] boxes={new RectF(38,477,430,899),new RectF(439,477,831,899),new RectF(38,909,430,1331),new RectF(439,909,831,1331),new RectF(125,40,766,245),new RectF(190,1490,678,1648)};
    private boolean menu=false;private String menuTitle="";private final boolean dutch;private final String[] labels;private final String name;
    
    ReferenceHome(Context context,String language,String[] labels,String name,Runnable[] actions){super(context);setTag("reference-home");setWillNotDraw(false);setLayerType(View.LAYER_TYPE_SOFTWARE,null);this.labels=labels;this.name=name;dutch=false;
        synchronized(ReferenceHome.class){if(artwork==null)try(InputStream in=context.getAssets().open("home-clean-3-0.png")){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;artwork=BitmapFactory.decodeStream(in,null,options);if(artwork==null)throw new IOException("Artwork");}catch(IOException error){throw new IllegalStateException("Home artwork missing",error);}}
        source=new Rect(LEFT,TOP,RIGHT,BOTTOM);
        String[] tags={"qr","photo","list","settings","home-identity","home-brand"};
        for(int i=0;i<targets.length;i++){final int index=i;View v;if(i<4){Button button=new Button(context);button.setAllCaps(false);button.setText("");button.setPadding(0,0,0,0);button.setMinimumWidth(0);button.setMinimumHeight(0);GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(22*getResources().getDisplayMetrics().density);button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x44ffffff),null,mask));button.setStateListAnimator(null);button.setElevation(0);button.setContentDescription(labels[i]);button.setOnClickListener(view->actions[index].run());v=button;}else{v=new View(context);v.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);v.setContentDescription(i==4?"BMD "+name:"Blue Medical, A Translumina Group Company");}v.setTag(tags[i]);targets[i]=v;addView(v,new FrameLayout.LayoutParams(1,1));}
        setBackgroundColor(MainActivity.NAVY);
    }
    ReferenceHome menu(String title){menu=true;menuTitle=title;setTag("scan-menu");invalidate();return this;}
    private void fit(int width,int height){geometry=new HomeGeometry(width,height);artworkScale=geometry.scale;screenEdges=geometry.rows;destination.set(geometry.left,0,geometry.right,height);}
    private float screenY(float y){return geometry.y(y);}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){fit(r-l,b-t);for(int i=0;i<targets.length;i++){RectF box=boxes[i];targets[i].layout(Math.round(destination.left+(box.left-LEFT)*artworkScale),Math.round(screenY(box.top)),Math.round(destination.left+(box.right-LEFT)*artworkScale),Math.round(screenY(box.bottom)));}}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);fit(getWidth(),getHeight());
        Rect[] bands={new Rect(110,717,374,793),new Rect(457,717,816,793),new Rect(88,1146,399,1220),new Rect(477,1146,807,1220),new Rect(295,173,724,241)};
        for(int segment=0;segment<7;segment++){
            Rect src=new Rect(LEFT,edges[segment],RIGHT,edges[segment+1]);
            RectF dst=new RectF(destination.left,screenEdges[segment],destination.right,screenEdges[segment+1]);
            canvas.drawBitmap(artwork,src,dst,paint);
            if(!dutch){canvas.save();canvas.clipRect(dst);canvas.translate(dst.left,dst.top);canvas.scale(dst.width()/src.width(),dst.height()/src.height());canvas.translate(-LEFT,-src.top);
                for(int i=0;i<bands.length;i++){Rect band=bands[i];if(band.top>=src.top&&band.bottom<=src.bottom){drawLabel(canvas,band,i<4?"":name,i==4);}}
                canvas.restore();}
        }
    }
    @Override protected void dispatchDraw(Canvas canvas){
        for(int i=0;i<4;i++){
            RectF sourceBox=boxes[i];RectF r=new RectF(destination.left+sourceBox.left*artworkScale,screenY(sourceBox.top),destination.left+sourceBox.right*artworkScale,screenY(sourceBox.bottom));
            int[][] colours=menu?new int[][]{{0xff00e7de,0xff00565d,0xff65fff2},{0xffffcf22,0xffd33a00,0xffffec73},{0xffc737ef,0xff480076,0xfff89dff},{0xff17b4ff,0xff0721b8,0xff70eaff}}:new int[][]{{0xff31bdff,0xff071bc0,0xff73edff},{0xff39f997,0xff006b2b,0xff8affb3},{0xffd34cff,0xff460096,0xfff1a2ff},{0xffffc02d,0xff8e230b,0xffffdb76}};
            android.graphics.drawable.Drawable face=UiArt.raised(colours[i][0],colours[i][1],colours[i][2],r.width()*.14f);face.setBounds(Math.round(r.left),Math.round(r.top),Math.round(r.right),Math.round(r.bottom));face.draw(canvas);
            String[] icons=menu?new String[]{"qr","pallet","mail","document"}:new String[]{"qr","users","book","settings"};UiArt.Icon icon=new UiArt.Icon(getContext(),icons[i]);int size=Math.round(Math.min(r.width(),r.height())*.25f);canvas.save();canvas.translate(r.centerX()-size/2f,r.top+r.height()*.24f);icon.layout(0,0,size,size);icon.draw(canvas);canvas.restore();
            Rect band=new Rect(Math.round(r.left+r.width()*.06f),Math.round(r.top+r.height()*.55f),Math.round(r.right-r.width()*.06f),Math.round(r.bottom-r.height()*.1f));drawLabel(canvas,band,labels[i],false);
        }
        if(menu){Rect title=new Rect((int)(getWidth()*.1f),(int)screenY(307),(int)(getWidth()*.9f),(int)screenY(423));drawLabel(canvas,title,menuTitle,false);}
        super.dispatchDraw(canvas);
    }
    private void drawLabel(Canvas canvas,Rect rect,String text,boolean title){TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setColor(title?0xffa6e7ff:Color.WHITE);p.setTypeface(Typeface.create("sans-serif",title?Typeface.BOLD_ITALIC:Typeface.BOLD));if(title)p.setShader(new LinearGradient(0,0,rect.width(),0,new int[]{0xff7af4ee,0xff9656e6},null,Shader.TileMode.CLAMP));StaticLayout layout=null;for(int size=title?62:Math.max(24,Math.round(rect.width()*.14f));size>=14;size--){p.setTextSize(size);layout=StaticLayout.Builder.obtain(text,0,text.length(),p,rect.width()).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build();if(layout.getHeight()<=rect.height()-8&&layout.getLineCount()<=(text.contains(" ")||text.contains("/")?2:1))break;}if(layout!=null){canvas.save();canvas.translate(rect.left,rect.top+(rect.height()-layout.getHeight())/2f);layout.draw(canvas);canvas.restore();}}
}
