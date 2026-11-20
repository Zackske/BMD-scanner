package com.bmd.scanner;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.Locale;

/** Two-finger camera gesture with a temporary, animated ratio readout; no zoom button. */
final class PinchCameraZoom {
    interface Listener {boolean ready();void begin();void change(float ratio);void end();}
    private final PinchZoomModel model=new PinchZoomModel();private final View preview;private final TextView hud;private final Listener listener;
    private final ScaleGestureDetector detector;private boolean multiple;private Locale locale=Locale.getDefault();private Runnable hide;
    PinchCameraZoom(Context context,View preview,TextView hud,Listener listener){this.preview=preview;this.hud=hud;this.listener=listener;hud.setVisibility(View.GONE);hud.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        detector=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScaleBegin(ScaleGestureDetector d){if(!model.available()||!listener.ready())return false;listener.begin();show();return true;}@Override public boolean onScale(ScaleGestureDetector d){float old=model.target,next=model.scale(d.getScaleFactor());if(Math.abs(old-next)>.0001f){show();listener.change(next);}return true;}@Override public void onScaleEnd(ScaleGestureDetector d){listener.end();hideSoon();}});detector.setQuickScaleEnabled(false);detector.setStylusScaleEnabled(false);
        preview.setOnTouchListener((v,event)->touch(event));
        preview.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);if(model.available()){info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}if(android.os.Build.VERSION.SDK_INT>=30)info.setStateDescription(label());else info.setText(label());}@Override public boolean performAccessibilityAction(View host,int action,Bundle args){if(model.available()&&listener.ready()&&(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)){listener.begin();float old=model.target,next=model.scale(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1.2f:1/1.2f);show();if(old!=next)listener.change(next);listener.end();hideSoon();return true;}return super.performAccessibilityAction(host,action,args);}});
    }
    void configure(float min,float max,float value,Locale locale){this.locale=locale;model.configure(min,max,value);hud.setText(label());}
    private String label(){NumberFormat f=NumberFormat.getNumberInstance(locale);f.setMinimumFractionDigits(1);f.setMaximumFractionDigits(2);f.setGroupingUsed(false);return f.format(model.target)+"×";}
    boolean touch(MotionEvent e){if(e.getPointerCount()>1)multiple=true;detector.onTouchEvent(e);boolean consumed=multiple;if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){if(!multiple&&e.getActionMasked()==MotionEvent.ACTION_UP)preview.performClick();multiple=false;}return true;}
    private void show(){if(hide!=null)hud.removeCallbacks(hide);boolean fresh=hud.getVisibility()!=View.VISIBLE;hud.animate().cancel();hud.setText(label());hud.setVisibility(View.VISIBLE);if(android.animation.ValueAnimator.areAnimatorsEnabled()){if(fresh){hud.setAlpha(0);hud.setScaleX(.85f);hud.setScaleY(.85f);}else{hud.setAlpha(1);hud.setScaleX(1.035f);hud.setScaleY(1.035f);}hud.animate().alpha(1).scaleX(1).scaleY(1).setDuration(fresh?180:110).start();}else{hud.setAlpha(1);hud.setScaleX(1);hud.setScaleY(1);}}
    private void hideSoon(){if(hide!=null)hud.removeCallbacks(hide);hide=()->{if(android.animation.ValueAnimator.areAnimatorsEnabled())hud.animate().alpha(0).scaleX(.94f).scaleY(.94f).setDuration(250).withEndAction(()->hud.setVisibility(View.GONE)).start();else hud.setVisibility(View.GONE);};hud.postDelayed(hide,650);preview.announceForAccessibility(label());}
    void restore(float actual){model.target=model.clamp(actual);hud.setText(label());hideSoon();}
    void dispose(){if(hide!=null)hud.removeCallbacks(hide);hud.animate().cancel();hud.setVisibility(View.GONE);preview.setOnTouchListener(null);preview.setAccessibilityDelegate(null);}
}
