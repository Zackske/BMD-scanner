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
    static int cameraTop(){return ThemeSkin.active==ThemeSkin.LIGHT?0xff07516b:ThemeSkin.active.top;}
    static int cameraBottom(){return ThemeSkin.active==ThemeSkin.LIGHT?0xff001b2f:ThemeSkin.active.bottom;}
    static GradientDrawable toolbar(Context context){float d=context.getResources().getDisplayMetrics().density;GradientDrawable surface=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{UiArt.mix(cameraTop(),Color.WHITE,.15f),cameraTop(),cameraBottom()});surface.setCornerRadius(21*d);surface.setStroke(Math.max(1,Math.round(d)),accent());return surface;}

    static GradientDrawable dock(Context c){GradientDrawable g=toolbar(c);g.setCornerRadius(99*c.getResources().getDisplayMetrics().density);return g;}
    /** Shared inset glass surface; badges remain outside its outline without being clipped. */
    static android.graphics.drawable.Drawable glass(Context context){return new Glass(context);}
    static final class Glass extends android.graphics.drawable.Drawable {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final float density;
        Glass(Context context){density=context.getResources().getDisplayMetrics().density;}
        @Override public void draw(Canvas c){Rect b=getBounds();float size=Math.max(0,Math.min(40*density,Math.min(b.width(),b.height())-8*density));if(size==0)return;RectF r=new RectF(b.exactCenterX()-size/2,b.exactCenterY()-size/2,b.exactCenterX()+size/2,b.exactCenterY()+size/2);float radius=size*.27f;
            p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{UiArt.mix(cameraTop(),Color.WHITE,.19f),cameraTop(),cameraBottom()},new float[]{0,.42f,1},Shader.TileMode.CLAMP));c.drawRoundRect(r,radius,radius,p);p.setShader(null);
            int save=c.save();Path clip=new Path();clip.addRoundRect(r,radius,radius,Path.Direction.CW);c.clipPath(clip);p.setShader(new LinearGradient(0,r.top,0,r.centerY(),new int[]{0x45ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));Path gloss=new Path();gloss.moveTo(r.left,r.top);gloss.lineTo(r.right,r.top);gloss.lineTo(r.right,r.top+size*.2f);gloss.cubicTo(r.centerX(),r.top+size*.23f,r.left+size*.2f,r.centerY(),r.left,r.top+size*.58f);gloss.close();c.drawPath(gloss,p);p.setShader(null);c.restoreToCount(save);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(density);p.setColor(accent());c.drawRoundRect(r,radius,radius,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);
        }
        @Override public void setAlpha(int alpha){}@Override public void setColorFilter(ColorFilter filter){}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    static android.graphics.drawable.Drawable popup(Context context){final float d=context.getResources().getDisplayMetrics().density;return new android.graphics.drawable.Drawable(){final Paint p=new Paint(3);@Override public void draw(Canvas c){Rect b=getBounds();Path body=new Path();body.addRoundRect(new RectF(b.left,b.top+8*d,b.right,b.bottom),16*d,16*d,Path.Direction.CW);Path pointer=new Path();float x=b.exactCenterX();pointer.moveTo(x-9*d,b.top+9*d);pointer.lineTo(x,b.top);pointer.lineTo(x+9*d,b.top+9*d);pointer.close();body.op(pointer,Path.Op.UNION);p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,new int[]{cameraTop(),cameraBottom()},null,Shader.TileMode.CLAMP));c.drawPath(body,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setColor(accent());p.setStrokeWidth(d);c.drawPath(body,p);}@Override public void setAlpha(int a){p.setAlpha(a);}@Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}};}
    static void metal(Canvas c,Path shape,Paint p,RectF bounds){
        p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setShader(new LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,new int[]{0xff68737a,0xfff2f5f7,0xffa5aeb4,0xffe7ecef,0xff69747c},new float[]{0,.25f,.52f,.73f,1},Shader.TileMode.CLAMP));c.drawPath(shape,p);p.setShader(null);int clip=c.save();c.clipPath(shape);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(.25f,bounds.width()/200));p.setColor(0x25ffffff);float step=Math.max(.7f,bounds.width()/100);for(float y=bounds.top;y<bounds.bottom;y+=step)c.drawLine(bounds.left,y,bounds.right,y-bounds.height()*.08f,p);c.restoreToCount(clip);p.setStrokeWidth(Math.max(.45f,bounds.width()/90));p.setColor(Color.WHITE);p.setShader(new LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,new int[]{0xfff5f8fa,0xff626e77},null,Shader.TileMode.CLAMP));c.drawPath(shape,p);p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);
    }
    static void satin(Canvas c,Path shape,Paint p,RectF bounds){
        p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setShader(new LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,new int[]{0xffdce2e6,0xfff2f5f6,0xffaab3ba,0xffd8dfe3},new float[]{0,.28f,.73f,1},Shader.TileMode.CLAMP));c.drawPath(shape,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(.45f,bounds.width()/150));p.setColor(0xff9aa5ae);c.drawPath(shape,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);
    }
    static final class Gear extends View {
        private final Paint p=new Paint(3);Gear(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){float scale=Math.min(getWidth(),getHeight())/64f;c.save();c.translate((getWidth()-64*scale)/2,(getHeight()-64*scale)/2);c.scale(scale,scale);Path gear=new Path();for(int i=0;i<32;i++){double a=i*Math.PI/16-Math.PI/2;float r=i%4<2?28:23,x=32+(float)Math.cos(a)*r,y=32+(float)Math.sin(a)*r;if(i==0)gear.moveTo(x,y);else gear.lineTo(x,y);}gear.close();gear.addCircle(32,32,10,Path.Direction.CCW);gear.setFillType(Path.FillType.EVEN_ODD);metal(c,gear,p,new RectF(4,4,60,60));c.restore();}
    }
    static final class More extends View {
        private final Paint p=new Paint(3);More(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.85f;Path ring=new Path();ring.addCircle(x,y,r,Path.Direction.CW);ring.addCircle(x,y,r*.82f,Path.Direction.CCW);ring.setFillType(Path.FillType.EVEN_ODD);metal(c,ring,p,new RectF(x-r,y-r,x+r,y+r));for(int row=-1;row<=1;row+=2){float cy=y+row*r*.23f,left=x-r*.54f,right=x+r*.54f;Path rail=new Path();rail.addRoundRect(new RectF(left,cy-r*.035f,right,cy+r*.035f),r*.035f,r*.035f,Path.Direction.CW);metal(c,rail,p,new RectF(left,cy-r*.035f,right,cy+r*.035f));float cx=x+row*r*.23f,knob=r*.13f;Path thumb=new Path();thumb.addCircle(cx,cy,knob,Path.Direction.CW);metal(c,thumb,p,new RectF(cx-knob,cy-knob,cx+knob,cy+knob));}}
    }
    static final class Save extends View {
        private final Paint paint=new Paint(3);
        private final String label;
        private ValueAnimator motion;
        private float scale=1;
        Save(Context context,String label,String accessible){super(context);this.label=label;setContentDescription(accessible);setClickable(true);setFocusable(true);}
        @Override public void setPressed(boolean pressed){super.setPressed(pressed);if(motion!=null)motion.cancel();float target=pressed?.94f:1;if(!ValueAnimator.areAnimatorsEnabled()){scale=target;invalidate();return;}motion=ValueAnimator.ofFloat(scale,target);motion.setDuration(pressed?100:320);if(!pressed)motion.setInterpolator(new OvershootInterpolator(1.6f));motion.addUpdateListener(a->{scale=(Float)a.getAnimatedValue();invalidate();});motion.start();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float size=Math.min(getWidth(),getHeight()),x=getWidth()/2f,y=getHeight()/2f;int accent=ThemeSkin.active.accent;c.save();c.translate(x,y);c.scale(scale*size/100,scale*size/100);paint.setAlpha(255);Path ring=new Path();ring.addCircle(0,0,48,Path.Direction.CW);ring.addCircle(0,0,42.5f,Path.Direction.CCW);ring.setFillType(Path.FillType.EVEN_ODD);satin(c,ring,paint,new RectF(-48,-48,48,48));paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(-32,-43,35,44,new int[]{UiArt.mix(cameraTop(),accent,.12f),cameraTop(),cameraBottom()},new float[]{0,.35f,1},Shader.TileMode.CLAMP));c.drawCircle(0,0,42,paint);paint.setShader(null);
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
