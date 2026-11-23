package com.bmd.scanner;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import java.util.function.Function;

/** First-photo instructions only. Acknowledging instructions does not validate markers. */
final class MarkerHelpPopup {
    final Dialog dialog;private final LinearLayout panel;private boolean closing,disposed;
    MarkerHelpPopup(Context c,Function<String,String> words,Runnable dismissed){
        dialog=new Dialog(c);panel=new LinearLayout(c);panel.setTag("marker-help-panel");panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(c,18),dp(c,16),dp(c,18),dp(c,16));panel.setBackground(ScannerControls.toolbar(c));
        TextView title=text(c,words.apply("marker_setup"),19);title.setTypeface(ReferenceArt.face(c,false,750));panel.addView(title);panel.addView(text(c,words.apply("marker_help"),13));View map=new MarkerMap(c);map.setContentDescription("A · B / C · D");panel.addView(map,new LinearLayout.LayoutParams(-1,dp(c,120)));panel.addView(text(c,words.apply("overview_hint"),13));
        TextView done=text(c,words.apply("marker_ack"),15);done.setGravity(Gravity.CENTER);done.setTypeface(ReferenceArt.face(c,false,700));done.setBackground(ScannerControls.toolbar(c));done.setMinHeight(dp(c,48));done.setTag("marker-help-done");done.setClickable(true);done.setFocusable(true);done.setOnClickListener(v->close());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(c,10);panel.addView(done,lp);
        ScrollView scroll=new ScrollView(c);scroll.setFillViewport(false);scroll.addView(panel);dialog.setContentView(scroll);dialog.setCanceledOnTouchOutside(false);dialog.setOnDismissListener(d->{if(!disposed)dismissed.run();});dialog.setOnKeyListener((d,key,e)->{if(key==KeyEvent.KEYCODE_BACK&&e.getAction()==KeyEvent.ACTION_UP){close();return true;}return key==KeyEvent.KEYCODE_BACK;});dialog.show();Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);int width=Math.min(dp(c,420),c.getResources().getDisplayMetrics().widthPixels-dp(c,28));panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int max=c.getResources().getDisplayMetrics().heightPixels-dp(c,90);window.setLayout(width,Math.min(panel.getMeasuredHeight(),max));window.setGravity(Gravity.CENTER);}animate(true);
    }
    private static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String value,int size){TextView v=new TextView(c);v.setText(value);v.setTextSize(size);v.setTextColor(Color.WHITE);v.setPadding(0,dp(c,4),0,dp(c,5));return v;}
    private void animate(boolean open){if(!ValueAnimator.areAnimatorsEnabled()){if(!open)dialog.dismiss();return;}AnimationSet set=new AnimationSet(true);set.addAnimation(new AlphaAnimation(open?0:1,open?1:0));set.addAnimation(new ScaleAnimation(open?.92f:1,open?1:.95f,open?.92f:1,open?1:.95f,Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f));set.setDuration(open?300:220);set.setInterpolator(new DecelerateInterpolator());if(!open)set.setAnimationListener(new Animation.AnimationListener(){public void onAnimationStart(Animation a){}public void onAnimationRepeat(Animation a){}public void onAnimationEnd(Animation a){if(!disposed)dialog.dismiss();}});panel.startAnimation(set);}
    void close(){if(closing||disposed)return;closing=true;animate(false);}
    void dispose(){disposed=true;panel.clearAnimation();dialog.setOnCancelListener(null);dialog.setOnDismissListener(null);dialog.dismiss();}
    private static final class MarkerMap extends View{
        final Paint p=new Paint(3);final Bitmap box;MarkerMap(Context c){super(c);box=ReferenceArt.bitmap(c,R.drawable.wizard_full_box);}
        @Override protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,w=Math.min(Math.min(getWidth()-16*d,280*d),getHeight()*2f),h=Math.min(getHeight(),w*.5f),left=(getWidth()-w)/2,top=(getHeight()-h)/2;RectF bounds=new RectF(left,top,left+w,top+h);c.drawBitmap(box,null,bounds,p);
            for(int n=0;n<4;n++){float x=left+w*(n%2==0?.34f:.66f),y=top+h*(n/2==0?.35f:.70f),r=10*d;p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);c.drawRoundRect(x-r,y-r,x+r,y+r,3*d,3*d,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d);p.setColor(ScannerControls.accent());c.drawRoundRect(x-r,y-r,x+r,y+r,3*d,3*d,p);p.setStyle(Paint.Style.FILL);p.setColor(0xff001b32);p.setTextSize(15*d);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(String.valueOf((char)('A'+n)),x,y+5*d,p);}
        }
    }
}
