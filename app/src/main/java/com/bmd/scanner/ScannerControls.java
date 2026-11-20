package com.bmd.scanner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.OvershootInterpolator;

/** Native, density-independent camera controls matching the approved scanner design. */
final class ScannerControls {
    private ScannerControls() {}
    static int accent(){return ThemeSkin.active==ThemeSkin.BMD?0xff00cfff:ThemeSkin.active.accent;}
    private static int cameraTop(){return ThemeSkin.active==ThemeSkin.LIGHT?0xff07516b:ThemeSkin.active.top;}
    private static int cameraBottom(){return ThemeSkin.active==ThemeSkin.LIGHT?0xff001b2f:ThemeSkin.active.bottom;}
    static GradientDrawable toolbar(Context context){float d=context.getResources().getDisplayMetrics().density;GradientDrawable surface=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{UiArt.mix(cameraTop(),Color.WHITE,.15f),cameraTop(),cameraBottom()});surface.setCornerRadius(21*d);surface.setStroke(Math.max(1,Math.round(d)),accent());return surface;}

    static final class Save extends View {
        private final Paint paint=new Paint(3);
        private final String label;
        private ValueAnimator motion;
        private float scale=1;
        Save(Context context,String label,String accessible){super(context);this.label=label;setContentDescription(accessible);setClickable(true);setFocusable(true);}
        @Override public void setPressed(boolean pressed){super.setPressed(pressed);if(motion!=null)motion.cancel();float target=pressed?.94f:1;if(!ValueAnimator.areAnimatorsEnabled()){scale=target;invalidate();return;}motion=ValueAnimator.ofFloat(scale,target);motion.setDuration(pressed?100:320);if(!pressed)motion.setInterpolator(new OvershootInterpolator(1.6f));motion.addUpdateListener(a->{scale=(Float)a.getAnimatedValue();invalidate();});motion.start();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float size=Math.min(getWidth(),getHeight()),x=getWidth()/2f,y=getHeight()/2f;int accent=ThemeSkin.active.accent;c.save();c.translate(x,y);c.scale(scale*size/100,scale*size/100);paint.setAlpha(isEnabled()?255:150);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.3f);paint.setColor(accent());c.drawCircle(0,0,48,paint);paint.setStrokeWidth(1.8f);paint.setColor(accent());c.drawCircle(0,0,45.8f,paint);paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(-32,-43,35,44,new int[]{UiArt.mix(cameraTop(),accent,.22f),cameraTop(),cameraBottom()},new float[]{0,.35f,1},Shader.TileMode.CLAMP));c.drawCircle(0,0,44,paint);paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.8f);paint.setColor(accent);c.drawCircle(0,0,44,paint);
            c.save();Path clip=new Path();clip.addCircle(0,0,43.5f,Path.Direction.CW);c.clipPath(clip);paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(0,-44,0,-4,new int[]{0x66ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));Path gloss=new Path();gloss.moveTo(-45,-42);gloss.lineTo(45,-42);gloss.lineTo(45,-30);gloss.cubicTo(10,-29,-23,-18,-45,-6);gloss.close();c.drawPath(gloss,paint);paint.setShader(null);c.restore();
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.3f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setColor(Color.WHITE);Path page=new Path();page.moveTo(-11,-28);page.lineTo(5,-28);page.lineTo(14,-19);page.lineTo(14,4);page.quadTo(14,7,11,7);page.lineTo(-11,7);page.quadTo(-14,7,-14,4);page.lineTo(-14,-25);page.quadTo(-14,-28,-11,-28);c.drawPath(page,paint);Path fold=new Path();fold.moveTo(5,-27);fold.lineTo(5,-19);fold.lineTo(13,-19);c.drawPath(fold,paint);c.drawLine(0,-13,0,0,paint);c.drawLine(-5,-5,0,0,paint);c.drawLine(0,0,5,-5,paint);
            paint.setStyle(Paint.Style.FILL);paint.setTypeface(ReferenceArt.face(getContext(),false,750));paint.setTextSize(13);while(paint.measureText(label)>68&&paint.getTextSize()>7)paint.setTextSize(paint.getTextSize()-.5f);paint.setTextAlign(Paint.Align.CENTER);c.drawText(label,0,27,paint);paint.setTextAlign(Paint.Align.LEFT);paint.setAlpha(255);c.restore();}
        @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Button");}
        @Override protected void onDetachedFromWindow(){if(motion!=null)motion.cancel();super.onDetachedFromWindow();}
    }

    static final class Torch extends View {
        private final Paint paint=new Paint(3);private final String onLabel,offLabel,caption;
        private boolean lit,pending,available=true;private float phase=1;private ValueAnimator effect;
        Torch(Context context,String on,String off,String caption){super(context);onLabel=on;offLabel=off;this.caption=caption;setClickable(true);setFocusable(true);updateDescription();}
        boolean lit(){return lit;}
        void available(boolean value){available=value;setEnabled(available&&!pending);invalidate();}
        void pending(boolean value){pending=value;setEnabled(available&&!pending);invalidate();}
        void state(boolean on,boolean animate){lit=on;setSelected(on);updateDescription();if(effect!=null)effect.cancel();phase=1;if(animate&&ValueAnimator.areAnimatorsEnabled()){effect=ValueAnimator.ofFloat(0,1);effect.setDuration(650);effect.addUpdateListener(a->{phase=(Float)a.getAnimatedValue();invalidate();});effect.start();}invalidate();}
        private void updateDescription(){setContentDescription(caption+" · "+(lit?onLabel:offLabel));if(android.os.Build.VERSION.SDK_INT>=30)setStateDescription(lit?onLabel:offLabel);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight(),cx=w/2,cy=h/2;int colour=lit?0xffffd94b:Color.WHITE;paint.setAlpha(available?255:150);paint.setTypeface(ReferenceArt.face(getContext(),false,600));paint.setTextSize(13*d);String label=lit?onLabel:offLabel;while(paint.measureText(label)+28*d>w-4*d&&paint.getTextSize()>8*d)paint.setTextSize(paint.getTextSize()-.5f*d);float text=paint.measureText(label),total=22*d+6*d+text,start=(w-total)/2;
            if(lit){paint.setStyle(Paint.Style.FILL);paint.setShader(new RadialGradient(cx,cy,Math.max(w,h)*.5f,new int[]{0x22ffd347,0x00ffd347},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);paint.setShader(null);}if(phase<1&&lit){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);paint.setColor((Math.round((1-phase)*180)<<24)|0x00ffda58);c.drawOval(cx-(19+phase*24)*d,cy-(13+phase*12)*d,cx+(19+phase*24)*d,cy+(13+phase*12)*d,paint);}
            float spring=phase>=1?1:phase<.25f?1-phase*.7f:phase<.6f?.825f+(phase-.25f)*.85f:1.1225f-(phase-.6f)*.30625f;c.save();c.translate(start,cy-14*d);c.scale(d,d);c.scale(spring,spring,11,14);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setColor(colour);Path bolt=new Path();bolt.moveTo(14,0);bolt.lineTo(2,16);bolt.lineTo(10,16);bolt.lineTo(7,28);bolt.lineTo(21,11);bolt.lineTo(12,11);bolt.close();c.drawPath(bolt,paint);c.restore();paint.setStyle(Paint.Style.FILL);paint.setColor(colour);c.drawText(label,start+28*d,cy-(paint.ascent()+paint.descent())/2,paint);paint.setAlpha(255);}
        @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.ToggleButton");info.setCheckable(true);info.setChecked(lit);}
        @Override protected void onDetachedFromWindow(){if(effect!=null)effect.cancel();super.onDetachedFromWindow();}
    }
}
