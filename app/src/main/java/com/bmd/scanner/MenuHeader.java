package com.bmd.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.animation.ValueAnimator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.TypedValue;
import java.util.HashMap;

/** One menu identity: the original tile artwork, a translated title and a Glass arrow. */
final class MenuHeader extends ViewGroup {
    private final BackButton back;
    private final View icon;
    private final HeaderCurve wave;
    private final Smoke smoke;
    final TextView title;
    private final TextView subtitle;
    private int rowHeight;
    private boolean compact,prominent,homeSpacing;

    MenuHeader(Context context,String key,String caption,String backLabel,Runnable navigate){
        super(context);setTag("menu-header");setClipChildren(false);
        smoke=new Smoke(context);
        back=new BackButton(context,backLabel);back.setTag("menu-back");back.setOnClickListener(v->navigate.run());
        icon=icon(context,key);icon.setTag("menu-header-icon-"+key);
        title=new TextView(context);title.setTag("page-title");title.setText(caption);
        title.setTextColor(ThemeSkin.active.text);title.setTypeface(ReferenceArt.face(context,false,900));
        title.setGravity(Gravity.CENTER_VERTICAL);title.setIncludeFontPadding(false);title.setMaxLines(2);
        if(android.os.Build.VERSION.SDK_INT>=29)title.setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_SIMPLE);
        title.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        title.setPadding(dp(4),0,dp(4),0);title.setTextSize(TypedValue.COMPLEX_UNIT_SP,26);
        subtitle=new TextView(context);subtitle.setTag("menu-header-subtitle");subtitle.setVisibility(GONE);
        subtitle.setTextColor(ThemeSkin.active==ThemeSkin.LIGHT?0xff8d6200:0xffffd166);
        subtitle.setTypeface(ReferenceArt.face(context,false,800));subtitle.setIncludeFontPadding(false);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP,13);subtitle.setSingleLine(true);subtitle.setPadding(dp(4),0,dp(4),0);
        subtitle.setAutoSizeTextTypeUniformWithConfiguration(11,13,1,TypedValue.COMPLEX_UNIT_SP);
        subtitle.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        wave=new HeaderCurve(context);wave.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(smoke);addView(back);addView(icon);addView(title);addView(subtitle);addView(wave);
    }
    void setSubtitle(String value){
        String text=value==null?"":value;
        if(text.contentEquals(subtitle.getText()))return;
        subtitle.setText(text);subtitle.setVisibility(text.isEmpty()?GONE:VISIBLE);requestLayout();
    }
    MenuHeader compact(){compact=true;smoke.setVisibility(GONE);requestLayout();return this;}
    MenuHeader prominent(){prominent=true;requestLayout();return this;}
    MenuHeader homeSpacing(){homeSpacing=true;prominent=true;wave.homeSpacing=true;setClipToPadding(false);requestLayout();return this;}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec),available=Math.max(1,width-getPaddingLeft()-getPaddingRight());
        float visualScale=homeSpacing?width/(float)dp(360):1;
        float maxSize=(homeSpacing?36:prominent?34:26)*visualScale,maxArt=(homeSpacing?76:prominent?52:38)*visualScale;
        int button=dp(available<=dp(360)?44:48),art=dp(maxArt),reserved=button+dp(24),gap=dp(8);
        back.measure(MeasureSpec.makeMeasureSpec(button,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(button,MeasureSpec.EXACTLY));
        int titleLimit=Math.max(1,available-reserved-art-gap-dp(4)),inkLimit=Math.max(1,titleLimit-dp(8));
        float scaled=getResources().getDisplayMetrics().scaledDensity,size=maxSize;
        android.text.TextPaint probe=new android.text.TextPaint(title.getPaint());
        probe.setTextSize(size*scaled);
        while(size>18*visualScale&&android.text.Layout.getDesiredWidth(title.getText(),probe)>inkLimit){size-=.5f;probe.setTextSize(size*scaled);if(prominent){art=dp(Math.max(38*visualScale,maxArt*size/maxSize));titleLimit=Math.max(1,available-reserved-art-gap-dp(4));inkLimit=Math.max(1,titleLimit-dp(8));}}
        icon.measure(MeasureSpec.makeMeasureSpec(art,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(art,MeasureSpec.EXACTLY));
        if(Math.abs(title.getTextSize()-size*scaled)>.1f)title.setTextSize(TypedValue.COMPLEX_UNIT_PX,size*scaled);
        int titleWidth=Math.min(titleLimit,(int)Math.ceil(android.text.Layout.getDesiredWidth(title.getText(),probe))+dp(8)+2);
        title.measure(MeasureSpec.makeMeasureSpec(Math.max(1,titleWidth),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(dp(64),MeasureSpec.AT_MOST));
        boolean hasSubtitle=subtitle.getVisibility()!=GONE;
        subtitle.measure(MeasureSpec.makeMeasureSpec(titleLimit,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(hasSubtitle?dp(24):0,MeasureSpec.AT_MOST));
        int groupHeight=title.getMeasuredHeight()+(hasSubtitle?subtitle.getMeasuredHeight()+dp(3):0);
        rowHeight=Math.max(homeSpacing?Math.round(width*300f/868)-dp(24):dp(69),Math.max(art,groupHeight)+dp(12));
        smoke.measure(MeasureSpec.makeMeasureSpec(Math.round(available*.92f),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(rowHeight+dp(8),MeasureSpec.EXACTLY));
        int height=homeSpacing?Math.round(width*(300f/868+.04f))+dp(8):rowHeight+dp(14)+getPaddingTop()+getPaddingBottom();
        height=Math.max(height,rowHeight+getPaddingTop()+getPaddingBottom());
        wave.measure(MeasureSpec.makeMeasureSpec(homeSpacing?width:available,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(homeSpacing?height:dp(14),MeasureSpec.EXACTLY));
        setMeasuredDimension(width,resolveSize(height,heightSpec));
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int left=getPaddingLeft(),top=getPaddingTop(),available=getWidth()-left-getPaddingRight();
        smoke.layout(left+Math.round(available*.04f),top,getWidth()-getPaddingRight()-Math.round(available*.04f),top+rowHeight+dp(8));
        int button=back.getMeasuredWidth(),art=icon.getMeasuredWidth(),gap=dp(8);
        back.layout(left,top+(rowHeight-button)/2,left+button,top+(rowHeight+button)/2);
        int x=left+button+dp(24),y=top+(rowHeight-art)/2;
        icon.layout(x,y,x+art,y+art);x+=art+gap;
        boolean hasSubtitle=subtitle.getVisibility()!=GONE;
        int groupHeight=title.getMeasuredHeight()+(hasSubtitle?subtitle.getMeasuredHeight()+dp(3):0);
        y=top+(rowHeight-groupHeight)/2;
        title.layout(x,y,x+title.getMeasuredWidth(),y+title.getMeasuredHeight());
        int subtitleTop=y+title.getMeasuredHeight()+dp(3);
        subtitle.layout(x,subtitleTop,x+subtitle.getMeasuredWidth(),subtitleTop+subtitle.getMeasuredHeight());
        if(homeSpacing)wave.layout(0,0,getWidth(),getHeight());
        else wave.layout(left,top+rowHeight,left+available,top+rowHeight+dp(14));
    }
    /** Use Home's actual curve and width, including its edge-to-edge glow. */
    private static final class HeaderCurve extends UiArt.Wave {
        boolean homeSpacing;
        HeaderCurve(Context context){super(context);}
        @Override protected void onDraw(Canvas canvas){
            if(homeSpacing)ReferenceArt.ribbon(canvas,getWidth(),getWidth()*300f/868+4*getResources().getDisplayMetrics().density,ThemeSkin.active,true);
            else super.onDraw(canvas);
        }
    }
    /** Cache the illustration separately: drifting smoke must never remeasure the heading. */
    private static final class Smoke extends View {
        private final Bitmap image;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final RectF bounds=new RectF();
        private final Runnable tick=this::invalidate;
        private Bitmap texture;
        private long motionUntil;
        Smoke(Context context){super(context);image=ReferenceArt.bitmap(context,R.drawable.header_smoke);paint.setAlpha(46);paint.setColorFilter(new PorterDuffColorFilter(ThemeSkin.active.mistInk(),PorterDuff.Mode.SRC_IN));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){
            if(texture!=null){texture.recycle();texture=null;}
            if(width>0&&height>0){
                texture=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
                bounds.set(0,height*.02f,width,height*.94f);
                new Canvas(texture).drawBitmap(image,null,bounds,paint);
            }
        }
        @Override protected void onDraw(Canvas canvas){
            boolean motion=ValueAnimator.areAnimatorsEnabled()&&hasWindowFocus()&&android.os.SystemClock.uptimeMillis()<motionUntil;float shift=motion?(float)Math.sin(android.os.SystemClock.uptimeMillis()/4200.0)*2*getResources().getDisplayMetrics().density:0;
            if(texture!=null)canvas.drawBitmap(texture,shift,0,null);
            removeCallbacks(tick);if(motion&&isAttachedToWindow()&&getWindowVisibility()==VISIBLE&&isShown())postDelayed(tick,80);
        }
        private void refreshMotion(){if(tick==null)return;removeCallbacks(tick);if(isAttachedToWindow()&&getWindowVisibility()==VISIBLE&&isShown())invalidate();}
        @Override protected void onAttachedToWindow(){super.onAttachedToWindow();motionUntil=android.os.SystemClock.uptimeMillis()+8400;refreshMotion();}
        @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);refreshMotion();}
        @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);refreshMotion();}
        @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);refreshMotion();}
        @Override protected void onDetachedFromWindow(){removeCallbacks(tick);super.onDetachedFromWindow();}
    }
    static View icon(Context context,String key){
        int resource;
        switch(key){
            case "scan":resource=ReferenceArt.HOME[0];break;
            case "portal":resource=ReferenceArt.HOME[1];break;
            case "book":resource=ReferenceArt.HOME[2];break;
            case "settings":resource=ReferenceArt.HOME[3];break;
            case "qr":resource=R.drawable.ref_home_qr;break;
            case "box":resource=R.drawable.wizard_full_box;break;
            case "stack":resource=R.drawable.wizard_stack;break;
            default:
                int tint=key.equals("language")?0xff00e1ff:key.equals("palette")?0xffd276ff:key.equals("info")?0xffffbc31:key.equals("pallet")?0xff49f794:key.equals("document")?0xffffb526:key.equals("mail")?0xffd178ff:ThemeSkin.active.accent;
                UiArt.Magic glyph=new UiArt.Magic(context,key,tint);
                return key.equals("pallet")||key.equals("document")||key.equals("mail")?glyph.unboxed():glyph;
        }
        return new Artwork(context,resource);
    }
    private static final class Artwork extends View {
        private static final HashMap<Integer,Rect> crops=new HashMap<>();
        private final Bitmap image;private final Rect source;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        Artwork(Context context,int resource){
            super(context);image=ReferenceArt.bitmap(context,resource);
            synchronized(crops){
                Rect crop=crops.get(resource);
                if(crop==null){
                    int width=image.getWidth(),height=image.getHeight(),left=width,top=height,right=0,bottom=0;
                    int[] pixels=new int[width*height];image.getPixels(pixels,0,width,0,0,width,height);
                    for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(Color.alpha(pixels[y*width+x])>8){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);}
                    crop=right>left&&bottom>top?new Rect(left,top,right,bottom):new Rect(0,0,width,height);crops.put(resource,crop);
                }
                source=crop;
            }
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        @Override protected void onDraw(Canvas canvas){
            float scale=Math.min(getWidth()/(float)source.width(),getHeight()/(float)source.height());
            float width=source.width()*scale,height=source.height()*scale;
            canvas.drawBitmap(image,source,new RectF((getWidth()-width)/2,(getHeight()-height)/2,(getWidth()+width)/2,(getHeight()+height)/2),paint);
        }
    }
}

