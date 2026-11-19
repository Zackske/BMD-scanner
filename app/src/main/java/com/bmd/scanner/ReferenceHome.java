package com.bmd.scanner;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;

/** Reference-driven artwork with real, accessible Android controls aligned to its tiles. */
final class ReferenceHome extends FrameLayout {
    static final int LEFT=0,TOP=0,RIGHT=868,BOTTOM=1812;
    private final android.graphics.drawable.Drawable headerMist;
    private final UiArt.Backdrop backdrop;private final UiArt.ScannerMark scanner;private final UiArt.Brand brand;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Rect source;
    private final RectF destination=new RectF();
    private final int[] edges=HomeGeometry.EDGES;
    private float[] screenEdges;
    private float artworkScale;private float headerNameRight;
    private HomeGeometry geometry;
    private final View[] targets=new View[6];
    private final RectF[] boxes={new RectF(38,477,430,899),new RectF(439,477,831,899),new RectF(38,909,430,1331),new RectF(439,909,831,1331),new RectF(125,40,766,245),new RectF(190,1490,678,1648)};
    private boolean menu=false;private String menuTitle="";private final boolean dutch;private final String[] labels;private final String name;
    
    ReferenceHome(Context context,String language,String[] labels,String name,Runnable[] actions){super(context);setTag("reference-home");setWillNotDraw(false);setLayerType(View.LAYER_TYPE_SOFTWARE,null);this.labels=labels;this.name=name;dutch=false;headerMist=UiArt.mist(context);headerMist.setCallback(this);
        backdrop=new UiArt.Backdrop(context);scanner=new UiArt.ScannerMark(context);brand=new UiArt.Brand(context);
        source=new Rect(LEFT,TOP,RIGHT,BOTTOM);
        String[] tags={"qr","photo","list","settings","home-identity","home-brand"};
        for(int i=0;i<targets.length;i++){final int index=i;View v;if(i<4){Button button=new Button(context);button.setAllCaps(false);button.setText("");button.setPadding(0,0,0,0);button.setMinimumWidth(0);button.setMinimumHeight(0);GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(22*getResources().getDisplayMetrics().density);button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x44ffffff),null,mask));button.setStateListAnimator(null);button.setElevation(0);button.setContentDescription(labels[i]);button.setOnClickListener(view->actions[index].run());v=button;}else{v=new View(context);v.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);v.setContentDescription(i==4?"BMD "+name:"Blue Medical, A Translumina Group Company");}v.setTag(tags[i]);targets[i]=v;addView(v,new FrameLayout.LayoutParams(1,1));}
        setBackgroundColor(ThemeSkin.active.background);
    }
    ReferenceHome menu(String title){menu=true;menuTitle=title;targets[4].setContentDescription(title);setTag("scan-menu");invalidate();return this;}
    private void fit(int width,int height){geometry=new HomeGeometry(width,height);artworkScale=geometry.scale;screenEdges=geometry.rows;destination.set(geometry.left,0,geometry.right,height);}
    private float screenY(float y){return geometry.y(y);}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){fit(r-l,b-t);for(int i=0;i<targets.length;i++){RectF box=boxes[i];targets[i].layout(Math.round(destination.left+(box.left-LEFT)*artworkScale),Math.round(screenY(box.top)),Math.round(destination.left+(box.right-LEFT)*artworkScale),Math.round(screenY(box.bottom)));}}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);fit(getWidth(),getHeight());backdrop.layout(0,0,getWidth(),getHeight());backdrop.draw(canvas);float left=destination.left,scale=artworkScale;RectF id=boxes[4];float y=screenY(id.top),height=screenY(id.bottom)-y;canvas.save();canvas.translate(left+id.left*scale,y-height*.18f);headerMist.setBounds(0,0,Math.round(700*scale),Math.round(height*1.5f));headerMist.draw(canvas);canvas.restore();canvas.save();canvas.translate(left+id.left*scale,y);int size=Math.round(height*.96f);scanner.layout(0,0,size,size);scanner.draw(canvas);canvas.restore();drawWordmark(canvas,left+320*scale,y,height,485*scale);ReferenceArt.ribbon(canvas,getWidth(),screenY(300)+4*getResources().getDisplayMetrics().density,ThemeSkin.active,true);float[] logo=geometry.brand();canvas.save();canvas.translate(Math.round(logo[0]),Math.round(logo[1]));brand.layout(0,0,Math.round(logo[2])-Math.round(logo[0]),Math.round(logo[3])-Math.round(logo[1]));brand.draw(canvas);canvas.restore();}

    @Override protected void dispatchDraw(Canvas canvas){
        for(int i=0;i<4;i++){
            RectF sourceBox=boxes[i];RectF r=new RectF(destination.left+sourceBox.left*artworkScale,screenY(sourceBox.top),destination.left+sourceBox.right*artworkScale,screenY(sourceBox.bottom));
            int[][] colours=menu?new int[][]{{0xff00e7de,0xff00565d,0xff65fff2},{0xffffcf22,0xffd33a00,0xffffec73},{0xffc737ef,0xff480076,0xfff89dff},{0xff17b4ff,0xff0721b8,0xff70eaff}}:new int[][]{{0xff31bdff,0xff071bc0,0xff73edff},{0xff39f997,0xff006b2b,0xff8affb3},{0xffd34cff,0xff460096,0xfff1a2ff},{0xffffc02d,0xff8e230b,0xffffdb76}};
            android.graphics.drawable.Drawable face=UiArt.tile(colours[i][0],colours[i][1],colours[i][2],r.width()*.14f);face.setBounds(Math.round(r.left),Math.round(r.top),Math.round(r.right),Math.round(r.bottom));face.draw(canvas);
            if(!menu||i==0){int size=Math.round(Math.min(r.width(),r.height())*.69f);float x=r.centerX()-size/2f,y=r.top+r.height()*.385f-size/2f;ReferenceArt.icon(canvas,getContext(),i,new RectF(x,y,x+size,y+size));}else{String[] icons={"qr","pallet","mail","document"};UiArt.Magic icon=new UiArt.Magic(getContext(),icons[i],new int[]{0xff00ddff,0xff49f794,0xffd178ff,0xffffb526}[i]).unboxed();int size=Math.round(Math.min(r.width(),r.height())*.47f);canvas.save();canvas.translate(r.centerX()-size/2f,r.top+r.height()*.14f);icon.layout(0,0,size,size);icon.draw(canvas);canvas.restore();}
            Rect band=new Rect(Math.round(r.left+r.width()*.06f),Math.round(r.top+r.height()*.59f),Math.round(r.right-r.width()*.06f),Math.round(r.bottom-r.height()*.1f));drawLabel(canvas,band,labels[i],false);
        }
        super.dispatchDraw(canvas);
    }
    private void drawWordmark(Canvas canvas,float x,float y,float height,float availableWidth){
        String heading=menu?menuTitle:name;
        TextPaint namePaint=new TextPaint(3);namePaint.setTypeface(ReferenceArt.magicFace(getContext(),heading));namePaint.setTextSize(100);
        Rect nameInk=new Rect();namePaint.getTextBounds(heading,0,heading.length(),nameInk);
        float nameSize=100*Math.min(height*(menu?.44f:.30f)/Math.max(1,nameInk.height()),availableWidth/Math.max(1,namePaint.measureText(heading)));
        namePaint.setTextSize(nameSize);namePaint.getTextBounds(heading,0,heading.length(),nameInk);
        float inkHeight=nameInk.height(),nameY=menu?y+(height-inkHeight)*.5f:y+height*.56f;
        namePaint.setShader(new LinearGradient(x,0,x+Math.max(1,namePaint.measureText(heading)),0,ThemeSkin.active.titleGradient(),null,Shader.TileMode.CLAMP));
        canvas.drawText(heading,x-nameInk.left,nameY-nameInk.top,namePaint);
        if(!menu){
            TextPaint initials=new TextPaint(3);initials.setTypeface(ReferenceArt.face(getContext(),true,850));initials.setTextSize(100);Rect initialsInk=new Rect();initials.getTextBounds("BMD",0,3,initialsInk);
            initials.setTextSize(100*inkHeight/Math.max(1,initialsInk.height()));initials.getTextBounds("BMD",0,3,initialsInk);initials.setColor(ThemeSkin.active==ThemeSkin.LIGHT?0xff173e7b:0xfff5f9ff);
            canvas.drawText("BMD",x-initialsInk.left,y+height*.16f-initialsInk.top,initials);
        }
        headerNameRight=x+namePaint.measureText(heading);spark(canvas,Math.min(getWidth()-18*artworkScale,headerNameRight+18*artworkScale),nameY+inkHeight*.5f,13*artworkScale);
    }
    private void drawLabel(Canvas canvas,Rect rect,String text,boolean title){TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setColor(title?ThemeSkin.active.accent:ThemeSkin.active.text);p.setTypeface(title?ReferenceArt.magicFace(getContext(),text):ReferenceArt.face(getContext(),false,850));if(title)p.setShader(new LinearGradient(0,0,rect.width(),0,ThemeSkin.active.titleGradient(),null,Shader.TileMode.CLAMP));StaticLayout layout=null;for(int size=title?62:Math.max(24,Math.round(rect.width()*.14f));size>=14;size--){p.setTextSize(size);layout=StaticLayout.Builder.obtain(text,0,text.length(),p,rect.width()).setAlignment(title?Layout.Alignment.ALIGN_NORMAL:Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build();if(layout.getHeight()<=rect.height()-8&&layout.getLineCount()<=(title?1:(text.contains(" ")||text.contains("/")?2:1)))break;}if(layout!=null){if(title){float width=0;for(int line=0;line<layout.getLineCount();line++)width=Math.max(width,layout.getLineWidth(line));headerNameRight=rect.left+width;}canvas.save();canvas.translate(rect.left,rect.top+(rect.height()-layout.getHeight())/2f);layout.draw(canvas);canvas.restore();}}
    private void spark(Canvas canvas,float x,float y,float r){Paint p=new Paint(3);p.setColor(0xffffd436);Path star=new Path();star.moveTo(x,y-r);star.quadTo(x+r*.15f,y-r*.15f,x+r,y);star.quadTo(x+r*.15f,y+r*.15f,x,y+r);star.quadTo(x-r*.15f,y+r*.15f,x-r,y);star.quadTo(x-r*.15f,y-r*.15f,x,y-r);canvas.drawPath(star,p);}
    @Override protected boolean verifyDrawable(android.graphics.drawable.Drawable drawable){return drawable==headerMist||super.verifyDrawable(drawable);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();headerMist.setVisible(true,false);}
    @Override protected void onDetachedFromWindow(){headerMist.setVisible(false,false);super.onDetachedFromWindow();}

}

