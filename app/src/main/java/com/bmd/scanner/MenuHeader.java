package com.bmd.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.widget.TextViewCompat;
import android.util.TypedValue;
import java.util.HashMap;

/** One menu identity: the original tile artwork, a translated title and a Glass arrow. */
final class MenuHeader extends ViewGroup {
    private final BackButton back;
    private final View icon;
    private final UiArt.Wave wave;
    private final Drawable smoke;
    final TextView title;
    private int rowHeight;
    private boolean compact;

    MenuHeader(Context context,String key,String caption,String backLabel,Runnable navigate){
        super(context);setTag("menu-header");setClipChildren(false);setWillNotDraw(false);
        smoke=UiArt.mist(context);smoke.setCallback(this);smoke.setAlpha(46);
        smoke.setColorFilter(new PorterDuffColorFilter(ThemeSkin.active.mistInk(),PorterDuff.Mode.SRC_IN));
        back=new BackButton(context,backLabel);back.setTag("menu-back");back.setOnClickListener(v->navigate.run());
        icon=icon(context,key);icon.setTag("menu-header-icon-"+key);
        title=new TextView(context);title.setTag("page-title");title.setText(caption);
        title.setTextColor(ThemeSkin.active.text);title.setTypeface(ReferenceArt.face(context,false,900));
        title.setGravity(Gravity.CENTER_VERTICAL);title.setIncludeFontPadding(false);title.setMaxLines(2);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(title,18,26,1,TypedValue.COMPLEX_UNIT_SP);
        wave=new UiArt.Wave(context);wave.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(back);addView(icon);addView(title);addView(wave);
    }
    MenuHeader compact(){compact=true;TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(title,18,26,1,TypedValue.COMPLEX_UNIT_SP);requestLayout();return this;}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec),available=Math.max(1,width-getPaddingLeft()-getPaddingRight());
        int button=dp(available<=dp(360)?44:48),art=dp(38),reserved=compact?dp(58):dp(100),gap=dp(8);
        back.measure(MeasureSpec.makeMeasureSpec(button,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(button,MeasureSpec.EXACTLY));
        icon.measure(MeasureSpec.makeMeasureSpec(art,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(art,MeasureSpec.EXACTLY));
        title.measure(MeasureSpec.makeMeasureSpec(Math.max(1,available-reserved-art-gap),MeasureSpec.AT_MOST),MeasureSpec.makeMeasureSpec(dp(64),MeasureSpec.AT_MOST));
        rowHeight=Math.max(dp(69),title.getMeasuredHeight()+dp(12));
        wave.measure(MeasureSpec.makeMeasureSpec(available,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(dp(14),MeasureSpec.EXACTLY));
        setMeasuredDimension(width,resolveSize(rowHeight+dp(14)+getPaddingTop()+getPaddingBottom(),heightSpec));
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int left=getPaddingLeft(),top=getPaddingTop(),available=getWidth()-left-getPaddingRight();
        int button=back.getMeasuredWidth(),art=icon.getMeasuredWidth(),gap=dp(8),reserved=compact?dp(58):dp(100);
        if(!compact){
            back.layout(left,top+(rowHeight-button)/2,left+button,top+(rowHeight+button)/2);
            int group=art+gap+title.getMeasuredWidth(),x=left+Math.max(0,(available-group)/2),centreY=top+rowHeight/2,y=centreY-art/2;
            icon.layout(x,y,x+art,y+art);x+=art+gap;y=centreY-title.getMeasuredHeight()/2;
            title.layout(x,y,x+title.getMeasuredWidth(),y+title.getMeasuredHeight());
            wave.layout(left,top+rowHeight,left+available,top+rowHeight+dp(14));return;
        }
        back.layout(left,top+(rowHeight-button)/2,left+button,top+(rowHeight+button)/2);
        int group=art+gap+title.getMeasuredWidth();
        int x=left+reserved+Math.max(0,(available-reserved-group)/2),y=top+(rowHeight-art)/2;
        icon.layout(x,y,x+art,y+art);x+=art+gap;
        y=top+(rowHeight-title.getMeasuredHeight())/2;
        title.layout(x,y,x+title.getMeasuredWidth(),y+title.getMeasuredHeight());
        wave.layout(left,top+rowHeight,left+available,top+rowHeight+dp(14));
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(compact)return;
        int width=getWidth()-getPaddingLeft()-getPaddingRight();
        smoke.setBounds(getPaddingLeft()+Math.round(width*.04f),getPaddingTop(),getWidth()-getPaddingRight()-Math.round(width*.04f),getPaddingTop()+rowHeight+dp(8));
        smoke.draw(canvas);
    }
    @Override protected boolean verifyDrawable(Drawable drawable){return drawable==smoke||super.verifyDrawable(drawable);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();smoke.setVisible(!compact,false);}
    @Override protected void onDetachedFromWindow(){smoke.setVisible(false,false);super.onDetachedFromWindow();}
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
