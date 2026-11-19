package com.bmd.scanner;

import android.content.Context;
import android.view.View;
import android.widget.*;

/** Settings uses exactly the Home viewport and footer geometry; content scrolls above it. */
final class BrandedSettings extends android.view.ViewGroup {
    private final UiArt.Backdrop background;private final ScrollView scroll;private final UiArt.Brand brand;
    BrandedSettings(Context c,LinearLayout content){super(c);setTag("branded-settings");background=new UiArt.Backdrop(c);scroll=new ScrollView(c);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.addView(content);brand=new UiArt.Brand(c);brand.setTag("theme-brand");addView(background);addView(scroll);addView(brand);}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec);setMeasuredDimension(w,h);float[] b=new HomeGeometry(w,h).brand();background.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));scroll.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,Math.round(b[1])-dp(14)),MeasureSpec.EXACTLY));brand.measure(MeasureSpec.makeMeasureSpec(Math.round(b[2])-Math.round(b[0]),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.round(b[3])-Math.round(b[1]),MeasureSpec.EXACTLY));}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){int w=r-l,h=b-t;float[] box=new HomeGeometry(w,h).brand();background.layout(0,0,w,h);scroll.layout(0,0,w,scroll.getMeasuredHeight());brand.layout(Math.round(box[0]),Math.round(box[1]),Math.round(box[2]),Math.round(box[3]));}
}

