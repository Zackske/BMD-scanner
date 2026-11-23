package com.bmd.scanner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.OvershootInterpolator;
import java.text.NumberFormat;
import java.util.Locale;

/** Horizontal zoom-out control with hardware bounds and spring/directional ripple feedback. */
final class HorizontalZoom extends View {
    interface Listener {void change(float value);}
    private final Paint paint=new Paint(3);private ZoomOutRange range=new ZoomOutRange(1,1);
    private float value=1,display=1,pending=Float.NaN,progress=1;private int direction=1;private boolean dragging;
    private Listener listener;private Locale locale=Locale.getDefault();private ValueAnimator motion,effect;
    HorizontalZoom(Context c){super(c);setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    void setup(float min,float max,float initial,Locale locale,Listener listener){range=new ZoomOutRange(min,max);value=range.clamp(initial);display=value;pending=Float.NaN;this.locale=locale;this.listener=listener;setEnabled(range.available());invalidate();}
    boolean canZoomOut(){return range.available();}
    float current(){return value;}
    void value(float next){pending=Float.NaN;value=range.clamp(next);if(android.os.Build.VERSION.SDK_INT>=30)setStateDescription(label(value));if(motion!=null)motion.cancel();if(ValueAnimator.areAnimatorsEnabled()){motion=ValueAnimator.ofFloat(display,value);motion.setDuration(450);motion.setInterpolator(new OvershootInterpolator(1.4f));motion.addUpdateListener(a->{display=(Float)a.getAnimatedValue();invalidate();});motion.start();}else{display=value;invalidate();}}
    void revert(){pending=Float.NaN;display=value;invalidate();}
    private String label(float f){NumberFormat nf=NumberFormat.getNumberInstance(locale);nf.setMaximumFractionDigits(2);nf.setGroupingUsed(false);return nf.format(f)+"×";}
    private float pad(){return 48*getResources().getDisplayMetrics().density;}
    private float position(float v){float left=pad(),right=getWidth()-pad();return range.available()?left+(range.clamp(v)-range.min)/(range.max-range.min)*(right-left):getWidth()/2f;}
    private void request(float next,int sign){next=range.clamp(next);if(Math.abs(next-value)<.005f||(!Float.isNaN(pending)&&Math.abs(next-pending)<.005f))return;pending=next;direction=sign;if(effect!=null)effect.cancel();progress=1;if(ValueAnimator.areAnimatorsEnabled()){effect=ValueAnimator.ofFloat(0,1);effect.setDuration(560);effect.addUpdateListener(a->{progress=(Float)a.getAnimatedValue();invalidate();});effect.start();}if(listener!=null)listener.change(next);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight(),trackY=h*.64f;paint.setColor(Color.WHITE);paint.setShader(new LinearGradient(0,0,w,h,new int[]{ScannerControls.cameraTop(),ScannerControls.cameraBottom()},null,Shader.TileMode.CLAMP));paint.setStyle(Paint.Style.FILL);c.drawRoundRect(1,1,w-1,h-1,h/2,h/2,paint);paint.setShader(null);paint.setColor(ScannerControls.accent());paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);c.drawRoundRect(1,1,w-1,h-1,h/2,h/2,paint);
        if(range.available()){paint.setStrokeWidth(1.7f*d);c.drawLine(pad(),trackY,w-pad(),trackY,paint);}paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(ReferenceArt.face(getContext(),false,700));paint.setTextSize(11*d);for(float stop:range.stops){float x=position(stop);paint.setStyle(Paint.Style.FILL);paint.setColor(ThemeSkin.active.edge);c.drawCircle(x,trackY,2.9f*d,paint);paint.setColor(Color.WHITE);c.drawText(label(stop),x,h*.30f,paint);}paint.setTextAlign(Paint.Align.LEFT);paint.setColor(ScannerControls.accent());c.drawCircle(position(display),trackY,5*d,paint);
        for(int sign:new int[]{-1,1}){float x=sign<0?21*d:w-21*d,y=h/2;float t=progress,scale=t>=1?1:t<.25f?1-t*.8f:t<.6f?.8f+(t-.25f)*.95f:1.1325f-(t-.6f)*.33125f;c.save();c.translate(x,y);if(sign==direction&&progress<1)c.scale(scale,scale);Path symbol=new Path();symbol.addRoundRect(-7*d,-1.8f*d,7*d,1.8f*d,1.1f*d,1.1f*d,Path.Direction.CW);if(sign>0)symbol.addRoundRect(-1.8f*d,-7*d,1.8f*d,7*d,1.1f*d,1.1f*d,Path.Direction.CW);int alpha=range.available()?255:100;int save=c.saveLayerAlpha(-10*d,-10*d,10*d,10*d,alpha);ScannerControls.metal(c,symbol,paint,new RectF(-8*d,-8*d,8*d,8*d));c.restoreToCount(save);c.restore();}
        if(progress<1){float x=direction<0?21*d:w-21*d,t=progress,y=h/2;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);paint.setColor((Math.round((1-t)*170)<<24)|(ScannerControls.accent()&0x00ffffff));c.drawCircle(x,y,(7+15*t)*d,paint);paint.setStyle(Paint.Style.FILL);for(int i=0;i<5;i++){float spread=i-2;paint.setColor((Math.round((1-t)*190)<<24)|(ScannerControls.accent()&0x00ffffff));c.drawCircle(x+direction*(24+Math.abs(spread)*4)*t*d,y+spread*t*3*d,1.3f*d,paint);}}
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(!isEnabled())return false;float d=getResources().getDisplayMetrics().density,x=e.getX();if(e.getActionMasked()==MotionEvent.ACTION_DOWN){getParent().requestDisallowInterceptTouchEvent(true);dragging=x>=42*d&&x<=getWidth()-42*d;if(!dragging){int sign=x<42*d?-1:1;request(range.step(value,sign),sign);}else drag(x);return true;}if(e.getActionMasked()==MotionEvent.ACTION_MOVE){if(dragging)drag(x);return true;}if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){dragging=false;getParent().requestDisallowInterceptTouchEvent(false);if(e.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;}return true;}
    private void drag(float x){float target=range.min+(range.max-range.min)*(x-pad())/Math.max(1,getWidth()-2*pad());float next=range.nearest(target);request(next,next<value?-1:1);}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onKeyDown(int key,KeyEvent e){if(isEnabled()&&(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)){int sign=key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1;request(range.step(value,sign),sign);return true;}return super.onKeyDown(key,e);}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.SeekBar");info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,range.min,range.max,value));if(isEnabled()){info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);}}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(isEnabled()&&(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD||action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)){int sign=action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD?-1:1;request(range.step(value,sign),sign);return true;}return super.performAccessibilityAction(action,args);}
    @Override protected void onDetachedFromWindow(){if(motion!=null)motion.cancel();if(effect!=null)effect.cancel();super.onDetachedFromWindow();}
}
