package com.bmd.scanner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

/** Shared round frosted chevron. Navigation remains owned by each screen. */
final class BackButton extends FrameLayout {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean camera;
    private final float density;

    BackButton(Context context,String label){this(context,label,false);}
    BackButton(Context context,String label,boolean camera){
        super(context);this.camera=camera;density=getResources().getDisplayMetrics().density;
        setWillNotDraw(false);setContentDescription(label);setClickable(true);setFocusable(true);
        setMinimumWidth(Math.round(44*density));setMinimumHeight(Math.round(44*density));
        setTag("back-button");
    }

    @Override public void setPressed(boolean pressed){
        super.setPressed(pressed);animate().cancel();
        float scale=pressed?.93f:1;
        if(ValueAnimator.areAnimatorsEnabled())animate().scaleX(scale).scaleY(scale).setDuration(pressed?90:180).start();
        else{setScaleX(scale);setScaleY(scale);}
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);ThemeSkin skin=ThemeSkin.active;
        int top=camera?ScannerControls.cameraTop():skin.top;
        int ink=camera?Color.WHITE:skin.text;
        int accent=camera?ScannerControls.accent():skin.accent;
        float size=Math.max(0,Math.min(48*density,Math.min(getWidth(),getHeight()))-2*density);
        if(size==0)return;
        float x=getWidth()/2f,y=getHeight()/2f,r=size/2;
        paint.setStyle(Paint.Style.FILL);paint.setAlpha(isEnabled()?255:125);
        paint.setShader(new LinearGradient(x-r,y-r,x+r,y+r,
                new int[]{UiArt.mix(top,ink,.35f),top,top},new float[]{0,.75f,1},Shader.TileMode.CLAMP));
        canvas.drawCircle(x,y,r,paint);paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density);
        paint.setColor(UiArt.mix(ink,accent,.5f));canvas.drawCircle(x,y,r,paint);
        if(isFocused()){paint.setStrokeWidth(2*density);paint.setColor(accent);canvas.drawCircle(x,y,r-2*density,paint);}
        paint.setColor(ink);paint.setStrokeWidth(size*.066f);
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        Path chevron=new Path();chevron.moveTo(x+size*.075f,y-size*.18f);
        chevron.lineTo(x-size*.105f,y);chevron.lineTo(x+size*.075f,y+size*.18f);
        canvas.drawPath(chevron,paint);paint.setAlpha(255);
    }

    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Button");}
    @Override protected void onDetachedFromWindow(){animate().cancel();super.onDetachedFromWindow();}
}
