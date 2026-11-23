package com.bmd.scanner;

import android.animation.*;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.*;
import android.view.animation.PathInterpolator;
import android.widget.*;

/** Settings stay above the existing PreviewView; closing never replaces the scan session. */
final class ScannerSettingsPopup {
    interface Words { String get(String key); }
    private final FrameLayout host,layer;private final Dialog dialog;
    private final LinearLayout panel;
    private final View anchor,scrim;
    private final SharedPreferences prefs;
    private final Runnable onClosed;
    private final Context context;
    private final int originalResolution;
    private int resolution,sensitivity;
    private boolean vibration,closing,disposed;private float progress;
    private android.view.animation.Animation transition;
    private PopupWindow choices;
    private final TextView resolutionValue;
    private final Words words;

    ScannerSettingsPopup(FrameLayout host,View anchor,Words words,Runnable onClosed){
        android.util.Log.i("BMD-settings","construct viewport="+host.getWidth()+"x"+host.getHeight());this.host=host;this.context=host.getContext();this.anchor=anchor;this.words=words;this.onClosed=onClosed;
        prefs=context.getSharedPreferences("MainActivity",0);
        originalResolution=resolution=Math.max(0,Math.min(CameraResolutionOptions.TARGETS.length-1,prefs.getInt("cameraResolution",1)));
        sensitivity=Math.max(0,Math.min(2,prefs.getInt("sensitivity",1)));vibration=prefs.getBoolean("vibrateAfterScan",true);
        layer=new FrameLayout(context);layer.setTag("scanner-settings-overlay");layer.setClickable(true);layer.setFocusableInTouchMode(true);
        scrim=new View(context);scrim.setBackgroundColor(0xa6001020);scrim.setAlpha(0);layer.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
        panel=column();panel.setLayerType(View.LAYER_TYPE_SOFTWARE,null);panel.setTag("scanner-settings-panel");panel.setPadding(dp(14),dp(14),dp(14),dp(18));panel.setBackground(surface(ScannerControls.accent(),26));panel.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(new ScannerControls.Gear(context),new LinearLayout.LayoutParams(dp(34),dp(34)));
        TextView title=label(words.get("scanner_settings"),20,true);title.setPadding(dp(10),0,0,0);title.setGravity(Gravity.CENTER_VERTICAL);title.setSingleLine(true);title.setAutoSizeTextTypeUniformWithConfiguration(13,20,1,android.util.TypedValue.COMPLEX_UNIT_SP);header.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView close=action("×",words.get("close"));close.setTag("scanner-settings-close");close.setTextSize(32);header.addView(close,new LinearLayout.LayoutParams(dp(44),dp(48)));close.setOnClickListener(v->close());panel.addView(header);
        LinearLayout fields=column();fields.setPadding(0,dp(14),0,dp(14));
        LinearLayout resolutionCard=card(fields);resolutionCard.addView(label(words.get("resolution"),19,true));
        FrameLayout resolutionControl=new FrameLayout(context);resolutionControl.setBackground(surface(ThemeSkin.active.edge,13));resolutionControl.setTag("scanner-settings-resolution");resolutionControl.setContentDescription(words.get("resolution"));resolutionControl.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.Button");}});resolutionControl.setClickable(true);resolutionControl.setFocusable(true);resolutionControl.setOnClickListener(v->resolutionChoices());
        resolutionValue=label("",16,false);resolutionValue.setGravity(Gravity.CENTER_VERTICAL);resolutionValue.setPadding(dp(12),0,0,0);resolutionValue.setSingleLine(true);resolutionValue.setEllipsize(android.text.TextUtils.TruncateAt.END);FrameLayout.LayoutParams valueLayout=new FrameLayout.LayoutParams(-1,-1);valueLayout.rightMargin=dp(40);resolutionControl.addView(resolutionValue,valueLayout);
        View chevron=new View(context){final android.graphics.Paint paint=new android.graphics.Paint(3);@Override protected void onDraw(android.graphics.Canvas canvas){paint.setColor(Color.WHITE);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);float x=getWidth()/2f,y=getHeight()/2f;canvas.drawLine(x-dp(6),y-dp(3),x,y+dp(3),paint);canvas.drawLine(x,y+dp(3),x+dp(6),y-dp(3),paint);}};chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);FrameLayout.LayoutParams arrowLayout=new FrameLayout.LayoutParams(dp(36),-1,Gravity.END);resolutionControl.addView(chevron,arrowLayout);resolutionCard.addView(resolutionControl,new LinearLayout.LayoutParams(-1,dp(44)));updateResolution();
        LinearLayout sensitivityCard=card(fields);sensitivityCard.addView(label(words.get("sensitivity"),19,true));
        LinearLayout options=new LinearLayout(context);options.setBackground(surface(ThemeSkin.active.edge,13));options.setClipToOutline(true);GradientDrawable segmentEdge=new GradientDrawable();segmentEdge.setColor(Color.TRANSPARENT);segmentEdge.setCornerRadius(dp(13));segmentEdge.setStroke(dp(1),ThemeSkin.active.edge);options.setForeground(segmentEdge);
        for(int n=0;n<3;n++){final int index=n;TextView choice=action(words.get(new String[]{"low","normal","high"}[n]),words.get("sensitivity")+": "+words.get(new String[]{"low","normal","high"}[n]));choice.setTextSize(15);choice.setTag("scanner-settings-sensitivity-"+n);options.addView(choice,new LinearLayout.LayoutParams(0,dp(44),1));choice.setOnClickListener(v->{sensitivity=index;renderSensitivity(options);});}sensitivityCard.addView(options);renderSensitivity(options);
        LinearLayout vibrationCard=card(fields);LinearLayout vibrationRow=new LinearLayout(context);vibrationRow.setGravity(Gravity.CENTER_VERTICAL);LinearLayout captions=column();captions.addView(label(words.get("vibrate"),19,true));TextView hint=label(words.get("vibrate_hint"),13,false);hint.setTextColor(0xffc7dae9);captions.addView(hint);vibrationRow.addView(captions,new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle=new Switch(context);toggle.setTag("scanner-settings-vibration");toggle.setContentDescription(words.get("vibrate"));toggle.setChecked(vibration);toggle.setMinWidth(dp(48));toggle.setSwitchMinWidth(dp(48));toggle.setShowText(false);toggle.setMinHeight(dp(48));GradientDrawable thumb=new GradientDrawable();thumb.setColor(Color.WHITE);thumb.setCornerRadius(dp(12));thumb.setSize(dp(24),dp(24));toggle.setThumbDrawable(thumb);GradientDrawable track=new GradientDrawable();track.setColor(Color.WHITE);track.setCornerRadius(dp(14));track.setSize(dp(48),dp(28));toggle.setTrackDrawable(track);toggle.setSplitTrack(false);toggle.setThumbTintList(ColorStateList.valueOf(Color.WHITE));toggle.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{ScannerControls.accent(),0xff527181}));toggle.setOnCheckedChangeListener((v,on)->vibration=on);vibrationRow.addView(toggle,new LinearLayout.LayoutParams(dp(52),dp(48)));vibrationCard.addView(vibrationRow);
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(false);scroll.addView(fields);panel.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        TextView done=action(words.get("done"),words.get("done"));done.setTag("scanner-settings-done");done.setTextSize(20);done.setTypeface(ReferenceArt.face(context,false,750));done.setBackground(surface(ScannerControls.accent(),28));done.setOnClickListener(v->close());panel.addView(done,new LinearLayout.LayoutParams(-1,dp(48)));
        int availableHeight=host.getHeight(),availableWidth=host.getWidth();int width=Math.min(availableWidth-dp(24),dp(430));panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.max(0,availableHeight-dp(24)),View.MeasureSpec.AT_MOST));
        int fixed=dp(14+18+52+48);scroll.getLayoutParams().height=Math.min(scroll.getMeasuredHeight(),Math.max(dp(72),availableHeight-dp(24)-fixed));
        FrameLayout.LayoutParams placement=new FrameLayout.LayoutParams(width,-2,Gravity.CENTER);layer.addView(panel,placement);panel.setVisibility(View.INVISIBLE);
        android.util.Log.i("BMD-settings","panel measured="+panel.getMeasuredWidth()+"x"+panel.getMeasuredHeight()+" scroll="+scroll.getLayoutParams().height);dialog=new Dialog(context){@Override public void onBackPressed(){ScannerSettingsPopup.this.close();}};dialog.setContentView(layer);dialog.setCancelable(false);dialog.setCanceledOnTouchOutside(false);dialog.setOnKeyListener((d,key,event)->{if(key==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)close();return true;}return false;});dialog.setOnDismissListener(d->{if(!disposed)remove(false);});dialog.show();Window window=dialog.getWindow();window.setBackgroundDrawableResource(android.R.color.transparent);window.setWindowAnimations(0);window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setGravity(Gravity.CENTER);window.setLayout(host.getWidth(),host.getHeight());
        layer.post(()->{if(disposed||closing)return;panel.setVisibility(View.VISIBLE);animate(true);close.requestFocus();});
    }
    private int dp(float value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(context);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private GradientDrawable surface(int edge,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{ScannerControls.cameraTop(),ScannerControls.cameraBottom()});d.setCornerRadius(dp(radius));d.setStroke(dp(1),edge);return d;}
    private TextView label(String value,int size,boolean bold){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setTypeface(ReferenceArt.face(context,false,bold?700:450));t.setPadding(0,dp(3),0,dp(7));return t;}
    private TextView action(String value,String description){TextView t=label(value,16,false);t.setContentDescription(description);t.setGravity(Gravity.CENTER);t.setPadding(0,0,0,0);t.setMinHeight(dp(44));t.setClickable(true);t.setFocusable(true);t.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.Button");}});return t;}
    private LinearLayout card(LinearLayout parent){LinearLayout l=column();l.setPadding(dp(14),dp(8),dp(14),dp(12));l.setBackground(surface(ThemeSkin.active.edge,17));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);parent.addView(l,p);return l;}
    private void renderSensitivity(LinearLayout options){for(int n=0;n<3;n++){TextView t=(TextView)options.getChildAt(n);t.setSelected(n==sensitivity);t.setTextColor(n==sensitivity?(ThemeSkin.contrast(ScannerControls.accent(),Color.BLACK)>=ThemeSkin.contrast(ScannerControls.accent(),Color.WHITE)?Color.BLACK:Color.WHITE):Color.WHITE);if(n==sensitivity){GradientDrawable selected=new GradientDrawable();selected.setColor(ScannerControls.accent());float r=dp(12);selected.setCornerRadii(n==0?new float[]{r,r,0,0,0,0,r,r}:n==2?new float[]{0,0,r,r,r,r,0,0}:new float[8]);t.setBackground(selected);}else t.setBackgroundColor(Color.TRANSPARENT);}}
    private void updateResolution(){resolutionValue.setText(CameraResolutionOptions.labels(words.get("highest"))[resolution]);if(resolutionValue.getParent() instanceof View)((View)resolutionValue.getParent()).setContentDescription(words.get("resolution")+": "+resolutionValue.getText());}
    private void resolutionChoices(){
        if(closing||disposed)return;if(choices!=null)choices.dismiss();LinearLayout rows=column();rows.setPadding(dp(8),dp(8),dp(8),dp(8));String[] labels=CameraResolutionOptions.labels(words.get("highest"));for(int n=0;n<labels.length;n++){final int index=n;TextView row=action(labels[n],labels[n]);row.setTag("scanner-resolution-"+n);row.setTextSize(14);row.setMinHeight(dp(48));row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),0,dp(8),0);row.setSelected(n==resolution);if(n==resolution)row.setTextColor(ScannerControls.accent());rows.addView(row,new LinearLayout.LayoutParams(-1,dp(48)));row.setOnClickListener(v->{resolution=index;updateResolution();choices.dismiss();});}
        ScrollView scroll=new ScrollView(context);scroll.addView(rows);choices=new PopupWindow(scroll,Math.min(panel.getWidth()-dp(28),dp(360)),Math.min(host.getHeight()-dp(64),dp(380)),true);choices.setBackgroundDrawable(surface(ThemeSkin.active.edge,16));choices.setOutsideTouchable(true);choices.setElevation(dp(4));choices.showAtLocation(layer,Gravity.CENTER,0,0);
    }
    private void animate(boolean opening){
        android.util.Log.i("BMD-settings","animate open="+opening+" panel="+panel.getWidth()+"x"+panel.getHeight());
        if(transition!=null){transition.setAnimationListener(null);transition.cancel();}panel.clearAnimation();scrim.clearAnimation();
        if(!ValueAnimator.areAnimatorsEnabled()){progress=opening?1:0;panel.setAlpha(opening?1:0);scrim.setAlpha(opening?1:0);if(!opening)remove(true);return;}
        int[] a=new int[2],b=new int[2];anchor.getLocationOnScreen(a);panel.getLocationOnScreen(b);
        final float dx=a[0]+anchor.getWidth()/2f-b[0]-panel.getWidth()/2f,dy=a[1]+anchor.getHeight()/2f-b[1]-panel.getHeight()/2f;
        final float from=progress,to=opening?1:0;panel.setAlpha(1);scrim.setAlpha(1);
        transition=new android.view.animation.Animation(){@Override protected void applyTransformation(float time,android.view.animation.Transformation frame){progress=from+(to-from)*time;float scale=.08f+.92f*progress;frame.getMatrix().setScale(scale,scale,panel.getWidth()/2f,panel.getHeight()/2f);frame.getMatrix().postTranslate(dx*(1-progress),dy*(1-progress));frame.setAlpha(progress);}};
        transition.setDuration(opening?380:260);transition.setInterpolator(new PathInterpolator(.22f,.8f,.22f,1));transition.setFillAfter(true);transition.setAnimationListener(new android.view.animation.Animation.AnimationListener(){public void onAnimationStart(android.view.animation.Animation a){}public void onAnimationRepeat(android.view.animation.Animation a){}public void onAnimationEnd(android.view.animation.Animation a){if(!opening&&!disposed)remove(true);}});
        android.view.animation.AlphaAnimation dim=new android.view.animation.AlphaAnimation(from,to);dim.setDuration(opening?380:260);dim.setInterpolator(new PathInterpolator(.22f,.8f,.22f,1));dim.setFillAfter(true);scrim.startAnimation(dim);panel.startAnimation(transition);
    }
    boolean resolutionChanged(){return resolution!=originalResolution;}
    View content(){return layer;}
    boolean isShowing(){return !disposed&&dialog.isShowing();}
    void close(){if(closing||disposed)return;closing=true;apply();if(choices!=null)choices.dismiss();if(panel.getVisibility()!=View.VISIBLE){remove(true);return;}animate(false);}
    private void apply(){prefs.edit().putInt("cameraResolution",resolution).putInt("sensitivity",sensitivity).putBoolean("vibrateAfterScan",vibration).apply();}
    void dispose(){if(disposed)return;apply();if(transition!=null){transition.setAnimationListener(null);transition.cancel();panel.clearAnimation();scrim.clearAnimation();}if(choices!=null)choices.dismiss();remove(false);}
    private void remove(boolean callback){if(disposed)return;disposed=true;dialog.dismiss();anchor.requestFocus();if(callback)onClosed.run();}
}
