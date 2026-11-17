package com.bmd.scanner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.io.*;

/** Approved static scanner chrome. The camera area is never painted from this artwork. */
final class ScannerLayout extends android.widget.FrameLayout {
    private static Bitmap art;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF header=new RectF(),footer=new RectF();
    private final boolean dutch;private final String[] labels;private boolean lamp=false,lampAvailable=true;private float glow;private ValueAnimator pulse;
    ScannerLayout(Context context,boolean dutch,String[] labels){super(context);this.dutch=dutch;this.labels=labels;setWillNotDraw(false);setBackgroundColor(MainActivity.NAVY);synchronized(ScannerLayout.class){if(art==null)try(InputStream in=context.getAssets().open("scanner-layout-2-2.png")){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;art=BitmapFactory.decodeStream(in,null,options);if(art==null)throw new IOException("Scanner artwork");}catch(IOException e){throw new IllegalStateException(e);}}}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private int headerHeight(int w,int h){return w<h?Math.round(w*244f/941):dp(88);}
    private int footerHeight(int w,int h){return w<h?Math.round(w*161f/941):dp(72);}
    private RectF fit(Rect source,int width,int top,int height){float s=Math.min(width/(float)source.width(),height/(float)source.height());float w=source.width()*s,h=source.height()*s;return new RectF((width-w)/2,top+(height-h)/2,(width+w)/2,top+(height+h)/2);}
    @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);setMeasuredDimension(w,h);int hh=headerHeight(w,h),fh=footerHeight(w,h);header.set(fit(new Rect(0,0,941,244),w,0,hh));footer.set(fit(new Rect(0,1511,941,1672),w,h-fh,fh));for(int i=0;i<getChildCount();i++){View v=getChildAt(i);Rect r=place(v,w,h,hh,fh);v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,r.width()),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,r.height()),MeasureSpec.EXACTLY));}}
    private Rect place(View v,int w,int h,int hh,int fh){String tag=String.valueOf(v.getTag());float scale=header.width()/941f;if(tag.equals("camera-viewport"))return new Rect(0,hh,w,h-fh);int slot=tag.equals("lamp-button")?0:tag.equals("camera-switch")?1:tag.equals("scanner-settings")?2:-1;if(slot>=0)return new Rect(Math.round(header.left+(14+slot*302)*scale),Math.round(header.top+111*scale),Math.round(header.left+(319+slot*302)*scale),Math.round(header.top+233*scale));int size=dp(48),cy=h-fh/2;if(tag.equals("list-button"))return new Rect(dp(22),cy-size/2,dp(22)+size,cy+size/2);if(tag.equals("reset-button"))return new Rect(w-dp(22)-size,cy-size/2,w-dp(22),cy+size/2);return new Rect(w/2-dp(46),h-fh,w/2+dp(46),h);}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){int w=r-l,h=b-t,hh=headerHeight(w,h),fh=footerHeight(w,h);for(int i=0;i<getChildCount();i++){View v=getChildAt(i);Rect rect=place(v,w,h,hh,fh);v.layout(rect.left,rect.top,rect.right,rect.bottom);}}
    @Override protected void onDraw(Canvas c){super.onDraw(c);p.setShader(null);p.setColorFilter(null);c.drawBitmap(art,new Rect(0,0,941,244),header,p);c.drawBitmap(art,new Rect(0,1511,941,1672),footer,p);float s=header.width()/941f;c.save();c.translate(header.left,header.top);c.scale(s,s);
        if(!dutch){p.setColor(MainActivity.NAVY);c.drawRect(242,12,760,88,p);label(c,labels[0],new RectF(240,14,760,84),42,true);String[] top={labels[1],labels[2],labels[3]};for(int i=0;i<3;i++){float x=30+i*302;p.setShader(new LinearGradient(x,180,x+280,224,new int[]{0xff073451,0xff03243d},null,Shader.TileMode.CLAMP));c.drawRect(x,181,x+280,226,p);p.setShader(null);label(c,top[i],new RectF(x,181,x+280,226),29,false);}}
        if(!lampAvailable){p.setColor(0x77001b32);c.drawRoundRect(14,111,319,233,20,20,p);}
        if(lamp){p.setColor(0xff06304a);c.drawRoundRect(141,120,187,181,8,8,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(0xffffd443);Path bolt=new Path();bolt.moveTo(170,125);bolt.lineTo(150,153);bolt.lineTo(165,153);bolt.lineTo(158,177);bolt.lineTo(181,145);bolt.lineTo(167,145);c.drawPath(bolt,p);p.setStyle(Paint.Style.FILL);}
        p.setColor(lamp?0xff8b6200:0xff0b354c);c.drawRoundRect(218,133,285,165,16,16,p);label(c,lamp?labels[6]:labels[5],new RectF(220,134,283,165),20,false);
        if(glow>0){p.setShader(new RadialGradient(165,155,75,new int[]{(Math.round(glow*120)<<24)|0x00ffd443,0x00ffd443},null,Shader.TileMode.CLAMP));c.drawCircle(165,155,75,p);p.setShader(null);}c.restore();
        if(!dutch){c.save();float fs=footer.width()/941;c.translate(footer.left,footer.top);c.scale(fs,fs);p.setColor(MainActivity.NAVY);c.drawRect(365,83,576,136,p);label(c,labels[4],new RectF(360,82,581,136),30,true);c.restore();}}
    private void label(Canvas c,String text,RectF r,float size,boolean bold){p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextSize(size);while(p.measureText(text)>r.width()-8&&size>14)p.setTextSize(--size);Paint.FontMetrics fm=p.getFontMetrics();c.drawText(text,r.centerX()-p.measureText(text)/2,r.centerY()-(fm.ascent+fm.descent)/2,p);}
    void lamp(boolean on){boolean activate=on&&!lamp;lamp=on;if(pulse!=null)pulse.cancel();if(activate&&ValueAnimator.areAnimatorsEnabled()){pulse=ValueAnimator.ofFloat(1,0);pulse.setDuration(500);pulse.addUpdateListener(a->{glow=(Float)a.getAnimatedValue();invalidate();});pulse.start();}else{glow=0;invalidate();}}
    void lampAvailable(boolean available){lampAvailable=available;invalidate();}
    @Override protected void onDetachedFromWindow(){if(pulse!=null)pulse.cancel();super.onDetachedFromWindow();}
    static View gallery(Context context,boolean dutch,String text){return new View(context){private final Paint p=new Paint(3);@Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();Path clip=new Path();clip.addRoundRect(new RectF(0,0,w,h),h/2,h/2,Path.Direction.CW);c.save();c.clipPath(clip);if(dutch)c.drawBitmap(art,new Rect(222,1310,720,1422),new RectF(0,0,w,h),p);else{p.setShader(new LinearGradient(0,0,0,h,new int[]{0xff008cb5,0xff002c47,0xff001a31},null,Shader.TileMode.CLAMP));c.drawRoundRect(0,0,w,h,h/2,h/2,p);p.setShader(null);p.setColor(Color.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(h*.31f);while(p.measureText(text)>w*.88f)p.setTextSize(p.getTextSize()-1);c.drawText(text,(w-p.measureText(text))/2,h*.61f,p);}c.restore();}};}
}
