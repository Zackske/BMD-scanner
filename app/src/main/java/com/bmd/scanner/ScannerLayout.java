package com.bmd.scanner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.*;

/** Density-independent native scanner chrome; the camera area is always a real preview. */
final class ScannerLayout extends android.widget.FrameLayout {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String[] labels;
    private final UiArt.Icon flash,flashOn,qr,barcode,both,settings,photo;
    private boolean lamp=false,lampAvailable=true;private String mode="both";private float glow,gearRotation=0,saveScale=1;private ValueAnimator pulse,gearMotion,saveMotion;
    ScannerLayout(Context c,boolean ignored,String[] labels){super(c);this.labels=labels;setWillNotDraw(false);setBackgroundColor(Color.BLACK);flash=new UiArt.Icon(c,"flash");flashOn=new UiArt.Icon(c,"flash",0xffffd443);qr=new UiArt.Icon(c,"qr");barcode=new UiArt.Icon(c,"barcode");both=new UiArt.Icon(c,"both");settings=new UiArt.Icon(c,"settings");photo=new UiArt.Icon(c,"camera");}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private int headerHeight(int w,int h){return Math.min(h/3,Math.max(dp(86),Math.min(dp(146),Math.round(w*.27f))));}
    private int footerHeight(int w,int h){return Math.min(h/3,Math.max(dp(100),Math.min(dp(150),Math.round(w*.36f))));}
    @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);setMeasuredDimension(w,h);int hh=headerHeight(w,h),fh=footerHeight(w,h);for(int i=0;i<getChildCount();i++){View v=getChildAt(i);Rect r=place(v,w,h,hh,fh);v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,r.width()),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,r.height()),MeasureSpec.EXACTLY));}}
    private Rect place(View v,int w,int h,int hh,int fh){String tag=String.valueOf(v.getTag());if(tag.equals("camera-viewport"))return new Rect(0,hh,w,h-fh);int slot=tag.equals("lamp-button")?0:tag.equals("camera-mode")?1:tag.equals("scanner-settings")?2:-1;if(slot>=0)return new Rect(Math.round(w*slot/3f)+dp(6),Math.round(hh*.43f),Math.round(w*(slot+1)/3f)-dp(6),hh-dp(5));return new Rect(Math.round(w*.18f),h-fh+dp(15),Math.round(w*.82f),h-dp(15));}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){int w=r-l,h=b-t,hh=headerHeight(w,h),fh=footerHeight(w,h);for(int i=0;i<getChildCount();i++){View v=getChildAt(i);Rect q=place(v,w,h,hh,fh);v.layout(q.left,q.top,q.right,q.bottom);}}
    private void glass(Canvas c,RectF r,float radius,int top,int bottom){p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{top,bottom,0xff001327},null,Shader.TileMode.CLAMP));c.drawRoundRect(r,radius,radius,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(0xff00dff2);c.drawRoundRect(r,radius,radius,p);p.setStyle(Paint.Style.FILL);}
    private void icon(Canvas c,View v,float x,float y,float size){c.save();c.translate(x,y);v.layout(0,0,Math.round(size),Math.round(size));v.draw(c);c.restore();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),hh=headerHeight(getWidth(),getHeight()),fh=footerHeight(getWidth(),getHeight());
        p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,0,w,hh,new int[]{0xff00162c,0xff073957,0xff00142a},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,hh,p);p.setShader(null);
        Path wave=new Path();wave.moveTo(w*.55f,hh*.38f);wave.cubicTo(w*.76f,hh*.4f,w*.9f,hh*.13f,w,hh*.08f);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(0xff00ddf7);c.drawPath(wave,p);p.setStyle(Paint.Style.FILL);
        label(c,labels[0],new RectF(dp(8),dp(4),w-dp(8),hh*.4f),Math.min(dp(21),w*.05f),true,Color.WHITE);
        RectF toolbar=new RectF(dp(6),hh*.43f,w-dp(6),hh-dp(5));glass(c,toolbar,dp(12),0xff07456c,0xff00172c);
        float iconSize=Math.min(dp(28),toolbar.height()*.49f),iy=toolbar.top+dp(5),slot=w/3;
        for(int i=1;i<3;i++){p.setColor(0xff00a5c4);p.setStrokeWidth(dp(.7f));c.drawLine(slot*i,toolbar.top+dp(6),slot*i,toolbar.bottom-dp(6),p);}
        float fx=slot*.48f-iconSize*.6f;icon(c,lamp?flashOn:flash,fx,iy,iconSize);
        label(c,lamp?labels[6]:labels[5],new RectF(fx+iconSize,iy,slot-dp(7),iy+iconSize),dp(10),false,lamp?0xffffd443:Color.WHITE);
        float mx=slot+slot*.28f-iconSize*.5f;icon(c,mode.equals("qr")?qr:mode.equals("bar")?barcode:both,mx,iy,iconSize);
        String status=mode.equals("qr")?"QR":mode.equals("bar")?labels[8]:"QR +\n"+labels[8];RectF mr=new RectF(mx+iconSize+dp(3),iy,slot*2-dp(6),iy+iconSize);String[] lines=status.split("\n");for(int i=0;i<lines.length;i++)label(c,lines[i],new RectF(mr.left,mr.top+i*mr.height()/lines.length,mr.right,mr.top+(i+1)*mr.height()/lines.length),dp(10),false,Color.WHITE);
        c.save();c.rotate(gearRotation,slot*2+slot/2,iy+iconSize/2);icon(c,settings,slot*2+slot/2-iconSize/2,iy,iconSize);c.restore();
        for(int i=0;i<3;i++)label(c,labels[i+1],new RectF(slot*i+dp(5),toolbar.top+toolbar.height()*.57f,slot*(i+1)-dp(5),toolbar.bottom-dp(3)),dp(13),false,Color.WHITE);
        if(!lampAvailable){p.setColor(0x9900172c);c.drawRoundRect(dp(7),toolbar.top,slot-dp(1),toolbar.bottom,dp(10),dp(10),p);}
        if(glow>0){p.setShader(new RadialGradient(fx+iconSize/2,iy+iconSize/2,dp(24),new int[]{(Math.round(80*glow)<<24)|0x00ffd443,0x00ffd443},null,Shader.TileMode.CLAMP));c.drawCircle(fx+iconSize/2,iy+iconSize/2,dp(24),p);p.setShader(null);}
        p.setShader(new LinearGradient(0,h-fh,w,h,new int[]{0xff063c70,0xff00152c},null,Shader.TileMode.CLAMP));c.drawRect(0,h-fh,w,h,p);p.setShader(null);p.setColor(0xff00d6e9);c.drawRect(0,h-fh,w,h-fh+dp(.8f),p);
        RectF button=new RectF(w*.18f,h-fh+dp(15),w*.82f,h-dp(15));c.save();c.scale(saveScale,saveScale,button.centerX(),button.centerY());glass(c,button,dp(23),0xff007be0,0xff002247);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(0xff00e6f2);c.drawRoundRect(button,dp(23),dp(23),p);p.setStyle(Paint.Style.FILL);
        float photoSize=Math.min(dp(44),button.height()*.48f);icon(c,photo,button.centerX()-photoSize/2,button.top+button.height()*.08f,photoSize);label(c,labels[4],new RectF(button.left+dp(8),button.top+button.height()*.56f,button.right-dp(8),button.bottom-dp(5)),dp(21),true,Color.WHITE);c.restore();
    }
    private void label(Canvas c,String value,RectF r,float size,boolean bold,int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextSize(size);while(p.measureText(value)>r.width()-dp(2)&&size>dp(7))p.setTextSize(--size);Paint.FontMetrics fm=p.getFontMetrics();c.drawText(value,r.centerX()-p.measureText(value)/2,r.centerY()-(fm.ascent+fm.descent)/2,p);}
    void settingsPressed(){if(!ValueAnimator.areAnimatorsEnabled())return;if(gearMotion!=null)gearMotion.cancel();gearMotion=ValueAnimator.ofFloat(0,90);gearMotion.setDuration(280);gearMotion.addUpdateListener(a->{gearRotation=(Float)a.getAnimatedValue();invalidate();});gearMotion.start();}
    void savePressed(){if(!ValueAnimator.areAnimatorsEnabled())return;if(saveMotion!=null)saveMotion.cancel();saveMotion=ValueAnimator.ofFloat(1,.95f,1.02f,1);saveMotion.setDuration(330);saveMotion.addUpdateListener(a->{saveScale=(Float)a.getAnimatedValue();invalidate();});saveMotion.start();}
    void mode(String mode){this.mode=mode;invalidate();}
    void lamp(boolean on){boolean activate=on&&!lamp;lamp=on;if(pulse!=null)pulse.cancel();if(activate&&ValueAnimator.areAnimatorsEnabled()){pulse=ValueAnimator.ofFloat(1,0);pulse.setDuration(500);pulse.addUpdateListener(a->{glow=(Float)a.getAnimatedValue();invalidate();});pulse.start();}else{glow=0;invalidate();}}
    void lampAvailable(boolean available){lampAvailable=available;invalidate();}
    @Override protected void onDetachedFromWindow(){if(pulse!=null)pulse.cancel();if(gearMotion!=null)gearMotion.cancel();if(saveMotion!=null)saveMotion.cancel();super.onDetachedFromWindow();}
}

