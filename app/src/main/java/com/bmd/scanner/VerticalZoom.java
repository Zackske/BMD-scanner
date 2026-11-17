package com.bmd.scanner;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.OvershootInterpolator;
import java.util.Locale;

/** Compact hardware-clamped zoom with the approved spring + directional ripple animation. */
final class VerticalZoom extends View {
    interface Listener{void change(float value);}
    private final Paint p=new Paint(3);private float min=1,max=3,value=1,display=1,pending=Float.NaN,progress=1;private int direction=1;private boolean dragging;
    private Listener listener;private Locale locale=Locale.getDefault();private ValueAnimator motion,effect;
    VerticalZoom(Context c){super(c);setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    void setup(float min,float max,float value,Locale locale,Listener listener){this.min=Math.max(1,min);this.max=Math.max(this.min,Math.min(3,max));this.value=value;display=value;this.locale=locale;this.listener=listener;invalidate();}
    void value(float next){pending=Float.NaN;value=next;if(android.os.Build.VERSION.SDK_INT>=30)setStateDescription(String.format(locale,"%.1f×",value));if(motion!=null)motion.cancel();if(ValueAnimator.areAnimatorsEnabled()){motion=ValueAnimator.ofFloat(display,next);motion.setDuration(480);motion.setInterpolator(new OvershootInterpolator(1.5f));motion.addUpdateListener(a->{display=(Float)a.getAnimatedValue();invalidate();});motion.start();}else{display=next;invalidate();}}
    private void animatePress(int direction){this.direction=direction;if(effect!=null)effect.cancel();if(!ValueAnimator.areAnimatorsEnabled()){progress=1;invalidate();return;}effect=ValueAnimator.ofFloat(0,1);effect.setDuration(560);effect.addUpdateListener(a->{progress=(Float)a.getAnimatedValue();invalidate();});effect.start();}
    @Override protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight(),x=w*.35f,pad=Math.min(38*d,h*.26f),top=pad,bottom=h-pad;
        p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff005072,0xff00172d},null,Shader.TileMode.CLAMP));c.drawRoundRect(1,1,w-1,h-1,w/2,w/2,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d);p.setColor(0xff00d7ea);c.drawRoundRect(1,1,w-1,h-1,w/2,w/2,p);
        float cx=w/2,py=direction>0?17*d:h-17*d;float t=progress,scale=t>=1?1:t<.25f?1-t*.8f:t<.6f?.8f+(t-.25f)*.95f:1.1325f-(t-.6f)*.33125f;
        for(int sign:new int[]{1,-1}){float y=sign>0?17*d:h-17*d;c.save();if(progress<1&&sign==direction)c.scale(scale,scale,cx,y);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(2*d);p.setColor(Color.WHITE);c.drawLine(cx-6*d,y,cx+6*d,y,p);if(sign>0)c.drawLine(cx,y-6*d,cx,y+6*d,p);c.restore();}
        if(progress<1){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d);p.setColor((Math.round((1-t)*170)<<24)|0x0000eaff);c.drawCircle(cx,py,(7+15*t)*d,p);p.setStyle(Paint.Style.FILL);for(int i=0;i<5;i++){float spread=i-2,travel=24+Math.abs(spread)*5;p.setColor((Math.round((1-t)*210)<<24)|0x0000eaff);c.drawCircle(cx+spread*t*4*d,py-direction*travel*t*d,1.6f*d,p);}}
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f*d);p.setColor(0xff21aacb);c.drawLine(x,top,x,bottom,p);p.setStyle(Paint.Style.FILL);p.setTextSize(11*d);p.setTypeface(Typeface.DEFAULT_BOLD);for(int n=1;n<=3;n++){float y=bottom-(n-1)/2f*(bottom-top);p.setColor(n<=max?0xff23b6d5:0xff486573);c.drawCircle(x,y,2.4f*d,p);p.setColor(n<=max?Color.WHITE:0xff79909f);c.drawText(n+"×",x+6*d,y+3.7f*d,p);}float y=bottom-(Math.max(.94f,Math.min(3.06f,display))-1)/2*(bottom-top);p.setColor(MainActivity.CYAN);c.drawCircle(x,y,5.7f*d,p);
    }
    private void request(float requested,int sign){float next=Math.max(min,Math.min(max,requested));if(Math.abs(next-value)<.01f||(!Float.isNaN(pending)&&Math.abs(next-pending)<.01f))return;pending=next;animatePress(sign);if(listener!=null)listener.change(next);}
    @Override public boolean onTouchEvent(MotionEvent e){if(!isEnabled())return false;float d=getResources().getDisplayMetrics().density,pad=Math.min(38*d,getHeight()*.26f),y=e.getY();if(e.getActionMasked()==MotionEvent.ACTION_DOWN){getParent().requestDisallowInterceptTouchEvent(true);dragging=y>=30*d&&y<=getHeight()-30*d;if(!dragging)request(y<30*d?(float)Math.floor(value+.01f)+1:(float)Math.ceil(value-.01f)-1,y<30*d?1:-1);else request(Math.round(1+2*(1-(y-pad)/Math.max(1,getHeight()-2*pad))),y<getHeight()/2?1:-1);return true;}if(e.getActionMasked()==MotionEvent.ACTION_MOVE){if(dragging)request(Math.round(1+2*(1-(y-pad)/Math.max(1,getHeight()-2*pad))),y<getHeight()/2?1:-1);return true;}if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){dragging=false;getParent().requestDisallowInterceptTouchEvent(false);if(e.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onKeyDown(int key,KeyEvent e){if(isEnabled()&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)){int sign=key==KeyEvent.KEYCODE_DPAD_UP?1:-1;request(value+sign,sign);return true;}return super.onKeyDown(key,e);}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.SeekBar");info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,min,max,value));if(isEnabled()){info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(isEnabled()&&(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)){int sign=action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1;request(value+sign,sign);return true;}return super.performAccessibilityAction(action,args);}
    @Override protected void onDetachedFromWindow(){if(motion!=null)motion.cancel();if(effect!=null)effect.cancel();super.onDetachedFromWindow();}
}

