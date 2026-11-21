package com.bmd.scanner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.widget.*;
import java.util.function.Function;

/** Compact, localized overview/quarter guide; it never creates or approves a scan. */
final class BoxPhotoSteps extends LinearLayout {
    private final int current;private final Function<String,String> words;
    BoxPhotoSteps(Context c,int current,boolean landscape,Function<String,String> words,String position,String title){super(c);this.current=Math.min(4,current);this.words=words;setTag("box-photo-guide");setOrientation(landscape?VERTICAL:HORIZONTAL);setGravity(android.view.Gravity.CENTER_VERTICAL);setPadding(dp(8),dp(5),dp(8),dp(5));setBackground(ScannerControls.toolbar(c));
        LinearLayout labels=new LinearLayout(c);labels.setOrientation(VERTICAL);TextView count=label(position,10);count.setTextColor(0xffd8eaf0);labels.addView(count);TextView name=label(title,landscape?12:13);name.setTypeface(ReferenceArt.face(c,false,650));name.setMaxLines(2);name.setAutoSizeTextTypeUniformWithConfiguration(10,landscape?12:13,1,android.util.TypedValue.COMPLEX_UNIT_SP);labels.addView(name);addView(labels,new LayoutParams(landscape?-1:0,-2,landscape?0:1));
        LinearLayout cells=new LinearLayout(c);cells.setGravity(android.view.Gravity.CENTER);for(int n=0;n<5;n++){StepCell cell=new StepCell(c,n);cell.setTag("box-photo-step-"+n);cell.setSelected(n==this.current);cell.setContentDescription(words.apply(n==0?"overview":new String[]{"top_left","top_right","bottom_left","bottom_right"}[n-1]));cells.addView(cell,new LayoutParams(0,dp(34),1));}LayoutParams cp=new LayoutParams(landscape?-1:0,dp(34),landscape?0:1.05f);cp.topMargin=landscape?dp(3):0;addView(cells,cp);
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String s,int size){TextView v=new TextView(getContext());v.setText(s);v.setTextSize(size);v.setTextColor(Color.WHITE);v.setIncludeFontPadding(false);return v;}
    void reveal(){if(!ValueAnimator.areAnimatorsEnabled())return;View cell=findViewWithTag("box-photo-step-"+current);cell.setScaleX(.86f);cell.setScaleY(.86f);cell.animate().scaleX(1).scaleY(1).setDuration(350).setInterpolator(new android.view.animation.OvershootInterpolator(1.4f)).start();}
    private final class StepCell extends View {
        final int index;final Paint p=new Paint(3);StepCell(Context c,int n){super(c);index=n;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
        @Override protected void onDraw(Canvas canvas){float density=getResources().getDisplayMetrics().density,w=getWidth(),cx=w/2,size=Math.min(21*density,w-4*density),top=2*density;int accent=ScannerControls.accent();p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(ScannerControls.cameraBottom());RectF box=new RectF(cx-size/2,top,cx+size/2,top+size*.74f);canvas.drawRoundRect(box,3*density,3*density,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(density);p.setColor(index==current?accent:0xff7793a2);canvas.drawRoundRect(box,3*density,3*density,p);p.setStyle(Paint.Style.FILL);float gap=2*density,pad=3*density,cellW=(box.width()-2*pad-gap)/2,cellH=(box.height()-2*pad-gap)/2;for(int q=0;q<4;q++){p.setColor(index==current&&(index==0||q==index-1)?accent:0xff35566b);float x=box.left+pad+(q%2)*(cellW+gap),y=box.top+pad+(q/2)*(cellH+gap);canvas.drawRoundRect(x,y,x+cellW,y+cellH,density,density,p);}p.setTextAlign(Paint.Align.CENTER);p.setColor(index<=current?Color.WHITE:0xffc3d1dc);p.setTextSize(10*density);p.setTypeface(ReferenceArt.face(getContext(),false,index==current?750:500));canvas.drawText(index<current?"✓":Integer.toString(index+1),cx,31*density,p);}
    }
}
