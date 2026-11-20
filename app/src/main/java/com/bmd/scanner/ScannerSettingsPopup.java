package com.bmd.scanner;

import android.animation.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.PathInterpolator;
import android.widget.*;

/** Settings stay above the existing PreviewView; closing never replaces the scan session. */
final class ScannerSettingsPopup {
    interface Words { String get(String key); }
    private final FrameLayout host,layer;
    private final LinearLayout panel;
    private final View anchor,scrim;
    private final SharedPreferences prefs;
    private final Runnable onClosed;
    private final Context context;
    private final int originalResolution;
    private int resolution,sensitivity;
    private boolean vibration,closing,disposed;
    private ValueAnimator transition;
    private PopupWindow choices;
    private final TextView resolutionValue;
    private final Words words;

    ScannerSettingsPopup(FrameLayout host,View anchor,Words words,Runnable onClosed){
        this.host=host;this.context=host.getContext();this.anchor=anchor;this.words=words;this.onClosed=onClosed;
        prefs=context.getSharedPreferences("MainActivity",0);
        originalResolution=resolution=Math.max(0,Math.min(CameraResolutionOptions.TARGETS.length-1,prefs.getInt("cameraResolution",1)));
        sensitivity=Math.max(0,Math.min(2,prefs.getInt("sensitivity",1)));vibration=prefs.getBoolean("vibrateAfterScan",true);
        layer=new FrameLayout(context);layer.setTag("scanner-settings-overlay");layer.setClickable(true);layer.setFocusableInTouchMode(true);
        scrim=new View(context);scrim.setBackgroundColor(0xa6001020);layer.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
        panel=column();panel.setTag("scanner-settings-panel");panel.setPadding(dp(14),dp(14),dp(14),dp(18));panel.setBackground(surface(ScannerControls.accent(),26));panel.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(new ScannerControls.Gear(context),new LinearLayout.LayoutParams(dp(34),dp(34)));
        TextView title=label(words.get("scanner_settings"),20,true);title.setPadding(dp(10),0,0,0);title.setSingleLine(true);title.setAutoSizeTextTypeUniformWithConfiguration(13,20,1,android.util.TypedValue.COMPLEX_UNIT_SP);header.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView close=action("×",words.get("close"));close.setTag("scanner-settings-close");close.setTextSize(32);header.addView(close,new LinearLayout.LayoutParams(dp(44),dp(48)));close.setOnClickListener(v->close());panel.addView(header);
        LinearLayout fields=column();fields.setPadding(0,dp(14),0,dp(14));
        LinearLayout resolutionCard=card(fields);resolutionCard.addView(label(words.get("resolution"),19,true));
        resolutionValue=action("",words.get("resolution"));resolutionValue.setTextSize(16);resolutionValue.setGravity(Gravity.CENTER_VERTICAL);resolutionValue.setPadding(dp(12),0,dp(12),0);resolutionValue.setBackground(surface(ThemeSkin.active.edge,13));resolutionValue.setTag("scanner-settings-resolution");resolutionValue.setOnClickListener(v->resolutionChoices());resolutionCard.addView(resolutionValue,new LinearLayout.LayoutParams(-1,dp(44)));updateResolution();
        LinearLayout sensitivityCard=card(fields);sensitivityCard.addView(label(words.get("sensitivity"),19,true));
        LinearLayout options=new LinearLayout(context);options.setBackground(surface(ThemeSkin.active.edge,13));
        for(int n=0;n<3;n++){final int index=n;TextView choice=action(words.get(new String[]{"low","normal","high"}[n]),words.get("sensitivity")+": "+words.get(new String[]{"low","normal","high"}[n]));choice.setTextSize(15);choice.setTag("scanner-settings-sensitivity-"+n);options.addView(choice,new LinearLayout.LayoutParams(0,dp(44),1));choice.setOnClickListener(v->{sensitivity=index;renderSensitivity(options);});}sensitivityCard.addView(options);renderSensitivity(options);
        LinearLayout vibrationCard=card(fields);LinearLayout vibrationRow=new LinearLayout(context);vibrationRow.setGravity(Gravity.CENTER_VERTICAL);LinearLayout captions=column();captions.addView(label(words.get("vibrate"),19,true));TextView hint=label(words.get("vibrate_hint"),13,false);hint.setTextColor(0xffc7dae9);captions.addView(hint);vibrationRow.addView(captions,new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle=new Switch(context);toggle.setTag("scanner-settings-vibration");toggle.setContentDescription(words.get("vibrate"));toggle.setChecked(vibration);toggle.setMinWidth(dp(48));toggle.setMinHeight(dp(48));toggle.setThumbTintList(ColorStateList.valueOf(Color.WHITE));toggle.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{ScannerControls.accent(),0xff527181}));toggle.setOnCheckedChangeListener((v,on)->vibration=on);vibrationRow.addView(toggle,new LinearLayout.LayoutParams(dp(52),dp(48)));vibrationCard.addView(vibrationRow);
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(false);scroll.addView(fields);panel.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        TextView done=action(words.get("done"),words.get("done"));done.setTag("scanner-settings-done");done.setTextSize(20);done.setTypeface(ReferenceArt.face(context,false,750));done.setBackground(surface(ScannerControls.accent(),28));done.setOnClickListener(v->close());panel.addView(done,new LinearLayout.LayoutParams(-1,dp(48)));
        int availableHeight=host.getHeight(),availableWidth=host.getWidth();int width=Math.min(availableWidth-dp(24),dp(430));panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.max(0,availableHeight-dp(24)),View.MeasureSpec.AT_MOST));
        int fixed=dp(14+18+52+48);scroll.getLayoutParams().height=Math.min(scroll.getMeasuredHeight(),Math.max(dp(72),availableHeight-dp(24)-fixed));
        FrameLayout.LayoutParams placement=new FrameLayout.LayoutParams(width,-2,Gravity.CENTER);layer.addView(panel,placement);panel.setVisibility(View.INVISIBLE);
        host.addView(layer,new FrameLayout.LayoutParams(-1,-1));layer.requestFocus();layer.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)close();return true;}return false;});
        host.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        for(int i=0;i<host.getChildCount();i++)if(host.getChildAt(i)!=layer)host.getChildAt(i).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        if(Build.VERSION.SDK_INT>=28)panel.setAccessibilityPaneTitle(words.get("scanner_settings"));
        layer.post(()->{if(disposed||closing)return;panel.setVisibility(View.VISIBLE);animate(true);close.requestFocus();panel.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);});
    }
    private int dp(float value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(context);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private GradientDrawable surface(int edge,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{ScannerControls.cameraTop(),ScannerControls.cameraBottom()});d.setCornerRadius(dp(radius));d.setStroke(dp(1),edge);return d;}
    private TextView label(String value,int size,boolean bold){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setTypeface(ReferenceArt.face(context,false,bold?700:450));t.setPadding(0,dp(3),0,dp(7));return t;}
    private TextView action(String value,String description){TextView t=label(value,16,false);t.setContentDescription(description);t.setGravity(Gravity.CENTER);t.setPadding(0,0,0,0);t.setMinHeight(dp(44));t.setClickable(true);t.setFocusable(true);t.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.Button");}});return t;}
    private LinearLayout card(LinearLayout parent){LinearLayout l=column();l.setPadding(dp(14),dp(8),dp(14),dp(12));l.setBackground(surface(ThemeSkin.active.edge,17));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);parent.addView(l,p);return l;}
    private void renderSensitivity(LinearLayout options){for(int n=0;n<3;n++){TextView t=(TextView)options.getChildAt(n);t.setSelected(n==sensitivity);t.setTextColor(n==sensitivity?(ThemeSkin.contrast(ScannerControls.accent(),Color.BLACK)>=ThemeSkin.contrast(ScannerControls.accent(),Color.WHITE)?Color.BLACK:Color.WHITE):Color.WHITE);t.setBackgroundColor(n==sensitivity?ScannerControls.accent():Color.TRANSPARENT);}}
    private void updateResolution(){resolutionValue.setText(CameraResolutionOptions.labels(words.get("highest"))[resolution]+"   ⌄");}
    private void resolutionChoices(){
        if(closing||disposed)return;if(choices!=null)choices.dismiss();LinearLayout rows=column();rows.setPadding(dp(8),dp(8),dp(8),dp(8));String[] labels=CameraResolutionOptions.labels(words.get("highest"));for(int n=0;n<labels.length;n++){final int index=n;TextView row=action(labels[n],labels[n]);row.setTag("scanner-resolution-"+n);row.setTextSize(14);row.setMinHeight(dp(48));row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),0,dp(8),0);row.setSelected(n==resolution);if(n==resolution)row.setTextColor(ScannerControls.accent());rows.addView(row,new LinearLayout.LayoutParams(-1,dp(48)));row.setOnClickListener(v->{resolution=index;updateResolution();choices.dismiss();});}
        ScrollView scroll=new ScrollView(context);scroll.addView(rows);choices=new PopupWindow(scroll,Math.min(panel.getWidth()-dp(28),dp(360)),Math.min(host.getHeight()-dp(64),dp(380)),true);choices.setBackgroundDrawable(surface(ThemeSkin.active.edge,16));choices.setOutsideTouchable(true);choices.setElevation(dp(4));choices.showAtLocation(layer,Gravity.CENTER,0,0);
    }
    private void animate(boolean opening){
        if(transition!=null)transition.cancel();int[] a=new int[2],b=new int[2];anchor.getLocationOnScreen(a);panel.getLocationOnScreen(b);float px=Math.max(0,Math.min(panel.getWidth(),a[0]+anchor.getWidth()/2f-b[0])),py=a[1]+anchor.getHeight()/2f-b[1];panel.setPivotX(px);panel.setPivotY(py);
        if(!ValueAnimator.areAnimatorsEnabled()){panel.setScaleX(opening?1:.08f);panel.setScaleY(opening?1:.08f);panel.setAlpha(opening?1:0);scrim.setAlpha(opening?1:0);if(!opening)remove(true);return;}
        final float from=opening?0:1,to=opening?1:0;transition=ValueAnimator.ofFloat(from,to);transition.setDuration(opening?380:260);transition.setInterpolator(new PathInterpolator(.22f,.8f,.22f,1));transition.addUpdateListener(v->{float value=(Float)v.getAnimatedValue();panel.setScaleX(.08f+.92f*value);panel.setScaleY(.08f+.92f*value);panel.setAlpha(value);scrim.setAlpha(value);});transition.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(!opening&&!disposed)remove(true);}});transition.start();
    }
    boolean resolutionChanged(){return resolution!=originalResolution;}
    boolean isShowing(){return !disposed;}
    void close(){if(closing||disposed)return;closing=true;apply();if(choices!=null)choices.dismiss();animate(false);}
    private void apply(){prefs.edit().putInt("cameraResolution",resolution).putInt("sensitivity",sensitivity).putBoolean("vibrateAfterScan",vibration).apply();}
    void dispose(){if(disposed)return;apply();if(transition!=null){transition.removeAllListeners();transition.cancel();}if(choices!=null)choices.dismiss();remove(false);}
    private void remove(boolean callback){if(disposed)return;disposed=true;host.removeView(layer);for(int i=0;i<host.getChildCount();i++)host.getChildAt(i).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);anchor.requestFocus();if(callback)onClosed.run();}
}
