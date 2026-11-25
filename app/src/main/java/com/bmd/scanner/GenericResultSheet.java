package com.bmd.scanner;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.PathInterpolator;
import android.widget.*;

/** Compact result sheet: themed navy surface, neutral silver trim and explicit actions. */
final class GenericResultSheet extends Dialog {
    private final LinearLayout panel;
    private boolean closing;

    GenericResultSheet(Context context,String title,String format,String closeLabel,LinearLayout contents){
        super(context);requestWindowFeature(Window.FEATURE_NO_TITLE);
        panel=new LinearLayout(context);panel.setTag("generic-result-popup");panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(context,20),dp(context,12),dp(context,20),dp(context,24));panel.setBackground(sheet(context));
        View handle=new View(context);GradientDrawable rail=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xffa0aeb9,0xffedf4f8,0xffa0aeb9});rail.setCornerRadius(dp(context,4));handle.setBackground(rail);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(context,40),dp(context,4));hp.gravity=Gravity.CENTER_HORIZONTAL;hp.bottomMargin=dp(context,20);panel.addView(handle,hp);
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);LinearLayout titles=new LinearLayout(context);titles.setOrientation(LinearLayout.VERTICAL);
        if(format!=null&&!format.isEmpty()){TextView type=label(context,format,11,0xffbcd1dd,800);type.setLetterSpacing(.12f);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.bottomMargin=dp(context,4);titles.addView(type,tp);}
        TextView heading=label(context,title,25,Color.WHITE,850);heading.setTag("generic-result-heading");heading.setMaxLines(2);heading.setAutoSizeTextTypeUniformWithConfiguration(17,25,1,android.util.TypedValue.COMPLEX_UNIT_SP);titles.addView(heading);header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        FrameLayout close=new FrameLayout(context);close.setTag("generic-close");close.setBackground(outline(context,99,0x45cbd5db));close.setContentDescription(closeLabel);close.setClickable(true);close.setFocusable(true);close.addView(new UiArt.Icon(context,"close",0xffd9e3ea,true),new FrameLayout.LayoutParams(dp(context,22),dp(context,22),Gravity.CENTER));close.setOnClickListener(v->close());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(context,44),dp(context,44));cp.leftMargin=dp(context,10);header.addView(close,cp);LinearLayout.LayoutParams titleSpace=new LinearLayout.LayoutParams(-1,-2);titleSpace.bottomMargin=dp(context,20);panel.addView(header,titleSpace);panel.addView(contents);
        ScrollView scroll=new ScrollView(context){@Override protected void onMeasure(int widthSpec,int heightSpec){int cap=Math.round(getResources().getDisplayMetrics().heightPixels*.84f);if(View.MeasureSpec.getMode(heightSpec)!=View.MeasureSpec.UNSPECIFIED)cap=Math.min(cap,View.MeasureSpec.getSize(heightSpec));super.onMeasure(widthSpec,View.MeasureSpec.makeMeasureSpec(cap,View.MeasureSpec.AT_MOST));}};scroll.setFillViewport(false);scroll.setClipToOutline(true);scroll.addView(panel);setContentView(scroll);setCanceledOnTouchOutside(false);
        setOnKeyListener((d,key,event)->{if(key==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)close();return true;}return false;});
    }
    @Override public void show(){super.show();Window window=getWindow();if(window!=null){Context c=getContext();android.util.DisplayMetrics screen=c.getResources().getDisplayMetrics();int width=Math.min(Math.min(screen.widthPixels,screen.heightPixels),dp(c,430));window.setBackgroundDrawableResource(android.R.color.transparent);window.setDimAmount(.6f);window.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);window.setWindowAnimations(0);window.setLayout(width,WindowManager.LayoutParams.WRAP_CONTENT);}if(ValueAnimator.areAnimatorsEnabled()){panel.setAlpha(0);panel.setTranslationY(dp(getContext(),24));panel.animate().alpha(1).translationY(0).setDuration(260).setInterpolator(new PathInterpolator(.2f,.8f,.2f,1)).start();}}
    void close(){if(closing||!isShowing())return;closing=true;if(!ValueAnimator.areAnimatorsEnabled()){dismiss();return;}panel.animate().alpha(0).translationY(dp(getContext(),24)).setDuration(200).withEndAction(this::dismiss).start();}
    @Override public void onBackPressed(){close();}
    @Override public void dismiss(){panel.animate().cancel();super.dismiss();}

    static int dp(Context context,float value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
    static TextView label(Context c,String value,int size,int colour,int weight){TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(colour);t.setTypeface(ReferenceArt.face(c,false,weight));t.setIncludeFontPadding(false);return t;}
    static GradientDrawable outline(Context c,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(Color.TRANSPARENT);d.setCornerRadius(dp(c,radius));d.setStroke(dp(c,1),stroke);return d;}
    static GradientDrawable card(Context c){GradientDrawable d=outline(c,22,0x45cbd5db);d.setColor(ScannerControls.cameraBottom());return d;}
    static GradientDrawable sheet(Context c){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{UiArt.mix(ScannerControls.cameraTop(),Color.WHITE,.08f),ScannerControls.cameraBottom()});float r=dp(c,30);d.setCornerRadii(new float[]{r,r,r,r,0,0,0,0});d.setStroke(dp(c,1),0xffcbd5db);return d;}
    static Drawable silver(Context context){return new Drawable(){final Paint p=new Paint(3);@Override public void draw(Canvas canvas){Rect b=getBounds();float radius=b.height()/2f;RectF area=new RectF(b);p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,new int[]{0xfff4f8fa,0xffbdcbd4,0xffe7eef3},new float[]{0,.55f,1},Shader.TileMode.CLAMP));canvas.drawRoundRect(area,radius,radius,p);p.setShader(null);int save=canvas.save();Path clip=new Path();clip.addRoundRect(area,radius,radius,Path.Direction.CW);canvas.clipPath(clip);p.setColor(0x16ffffff);p.setStrokeWidth(.5f);for(float x=b.left-b.height();x<b.right;x+=2)canvas.drawLine(x,b.bottom,x+b.height()*.18f,b.top,p);canvas.restoreToCount(save);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(context,1));p.setColor(0xffedf4f8);canvas.drawRoundRect(area,radius,radius,p);p.setStyle(Paint.Style.FILL);}@Override public void setAlpha(int alpha){p.setAlpha(alpha);}@Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}};}
    static View action(Context c,String title,String icon,boolean primary,Runnable run){
        LinearLayout button=new LinearLayout(c){@Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Button");}};button.setGravity(Gravity.CENTER);button.setPadding(dp(c,13),dp(c,9),dp(c,13),dp(c,9));button.setMinimumHeight(dp(c,primary?54:48));button.setBackground(primary?silver(c):outline(c,99,0x45cbd5db));button.setClickable(true);button.setFocusable(true);button.setContentDescription(title);int colour=primary?0xff153348:Color.WHITE;button.addView(new UiArt.Icon(c,icon,colour,true),new LinearLayout.LayoutParams(dp(c,21),dp(c,21)));TextView text=label(c,title,15,colour,primary?850:650);text.setMaxLines(2);text.setGravity(Gravity.CENTER);text.setAutoSizeTextTypeUniformWithConfiguration(11,15,1,android.util.TypedValue.COMPLEX_UNIT_SP);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=dp(c,8);button.addView(text,tp);button.setOnClickListener(v->run.run());return button;
    }
}
