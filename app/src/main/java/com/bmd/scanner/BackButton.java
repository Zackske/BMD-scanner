package com.bmd.scanner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

/** Shared compact, unframed chevron. Navigation remains owned by each screen. */
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
        float scale=pressed?.92f:1;
        if(ValueAnimator.areAnimatorsEnabled())animate().scaleX(scale).scaleY(scale).setDuration(pressed?90:180).start();
        else{setScaleX(scale);setScaleY(scale);}
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);ThemeSkin skin=ThemeSkin.active;
        int ink=camera?Color.WHITE:skin.text;
        int accent=camera?ScannerControls.accent():skin.accent;
        float size=Math.max(0,Math.min(28*density,Math.min(getWidth(),getHeight())-16*density));
        if(size==0)return;
        float x=getWidth()/2f,y=getHeight()/2f;
        paint.setStyle(Paint.Style.FILL);paint.setColor(accent);
        if(isPressed()){paint.setAlpha(26);canvas.drawRoundRect(x-15*density,y-15*density,x+15*density,y+15*density,10*density,10*density,paint);}
        if(isFocused()){paint.setStyle(Paint.Style.STROKE);paint.setAlpha(210);paint.setStrokeWidth(1.5f*density);canvas.drawRoundRect(x-15*density,y-15*density,x+15*density,y+15*density,10*density,10*density,paint);}
        paint.setStyle(Paint.Style.STROKE);paint.setColor(UiArt.mix(accent,ink,.60f));paint.setAlpha(isEnabled()?255:125);paint.setStrokeWidth(size*1.6f/24);
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        float direction=getLayoutDirection()==LAYOUT_DIRECTION_RTL?-1:1;
        Path arrow=new Path();arrow.moveTo(x+direction*size*.125f,y-size*.25f);
        arrow.lineTo(x-direction*size*.125f,y);arrow.lineTo(x+direction*size*.125f,y+size*.25f);
        canvas.drawPath(arrow,paint);paint.setAlpha(255);
    }

    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Button");}
    @Override protected void onDetachedFromWindow(){animate().cancel();super.onDetachedFromWindow();}
}
