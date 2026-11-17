package com.bmd.scanner;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.Locale;

final class VerticalZoom extends View {
    interface Listener{void change(float value);}
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private float min=1,max=4,value=1;private Listener listener;private Locale locale=Locale.getDefault();
    VerticalZoom(Context c){super(c);setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    void setup(float min,float max,float value,Locale locale,Listener listener){this.min=min;this.max=max;this.value=value;this.locale=locale;this.listener=listener;invalidate();}
    void value(float v){value=v;setStateDescriptionCompat();invalidate();}
    private void setStateDescriptionCompat(){if(android.os.Build.VERSION.SDK_INT>=30)setStateDescription(String.format(locale,"%.1f×",value));}
    @Override protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight(),x=w-21*d,top=35*d,bottom=h-35*d;
        p.setColor(0xdd052744);p.setStyle(Paint.Style.FILL);c.drawRoundRect(w-42*d,0,w,h,21*d,21*d,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d);p.setColor(0xff6d91b2);c.drawRoundRect(w-42*d,0,w,h,21*d,21*d,p);
        p.setStrokeWidth(2*d);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.WHITE);c.drawLine(x-6*d,17*d,x+6*d,17*d,p);c.drawLine(x,11*d,x,23*d,p);c.drawLine(x-6*d,h-17*d,x+6*d,h-17*d,p);p.setColor(UiArt.CYAN);c.drawLine(x,top,x,bottom,p);
        float y=bottom-(max>min?(value-min)/(max-min):0)*(bottom-top);p.setStyle(Paint.Style.FILL);p.setColor(0x4400eaff);c.drawCircle(x,y,16*d,p);p.setColor(UiArt.CYAN);c.drawCircle(x,y,10*d,p);p.setColor(Color.WHITE);c.drawCircle(x,y,3*d,p);
        String label=String.format(locale,"%.1f×",value);p.setTextSize(12*d);p.setTypeface(Typeface.DEFAULT_BOLD);float labelWidth=p.measureText(label)+14*d;p.setColor(0xee001c35);c.drawRoundRect(x-18*d-labelWidth,y-13*d,x-15*d,y+13*d,12*d,12*d,p);p.setColor(Color.WHITE);c.drawText(label,x-11*d-labelWidth,y+4*d,p);
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(!isEnabled())return false;float d=getResources().getDisplayMetrics().density;if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){getParent().requestDisallowInterceptTouchEvent(true);float v;if(e.getY()<30*d)v=value+.2f;else if(e.getY()>getHeight()-30*d)v=value-.2f;else v=min+(1-(e.getY()-35*d)/(getHeight()-70*d))*(max-min);if(listener!=null)listener.change(Math.max(min,Math.min(max,v)));return true;}if(e.getAction()==MotionEvent.ACTION_UP){performClick();getParent().requestDisallowInterceptTouchEvent(false);return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onKeyDown(int key,KeyEvent e){if(listener!=null&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)){listener.change(Math.max(min,Math.min(max,value+(key==KeyEvent.KEYCODE_DPAD_UP?.2f:-.2f))));return true;}return super.onKeyDown(key,e);}
}
