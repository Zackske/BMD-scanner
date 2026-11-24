package com.bmd.scanner;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

/** Explicit choice of the final check's capture workflow, after picklist review. */
final class ControlStagePopup {
    interface Words {String get(String key,Object... args);}
    interface Choice {void choose(String mode);}
    private final Activity activity;private final Words words;private final Choice choice;
    ControlStagePopup(Activity activity,Words words,Choice choice){this.activity=activity;this.words=words;this.choice=choice;}
    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
    void show(){
        Dialog dialog=new Dialog(activity);LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(18),dp(18),dp(18));content.setBackground(ScannerControls.toolbar(activity));
        TextView title=text(words.get("packing_question"),23,true);content.addView(title);
        for(String mode:new String[]{"box","stack"}){
            boolean box=mode.equals("box");LinearLayout tile=new LinearLayout(activity);tile.setOrientation(LinearLayout.VERTICAL);tile.setPadding(dp(14),dp(14),dp(14),dp(14));tile.setBackground(UiArt.raised(box?0xff00a9bc:0xffac31cf,box?0xff003345:0xff321140,ThemeSkin.active.accent,dp(18)));tile.setTag("control-stage-"+mode);tile.setClickable(true);tile.setFocusable(true);
            tile.addView(text(words.get(box?"packed_box":"before_packing"),18,true));tile.addView(text(words.get(box?"packed_box_hint":"before_packing_hint"),14,false));tile.setContentDescription(words.get(box?"packed_box":"before_packing"));tile.setOnClickListener(v->{dialog.dismiss();choice.choose(mode);});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(12);content.addView(tile,p);
        }
        TextView cancel=text(words.get("back"),16,true);cancel.setMinHeight(dp(48));cancel.setGravity(Gravity.CENTER);cancel.setOnClickListener(v->dialog.dismiss());content.addView(cancel);
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);dialog.setContentView(scroll);dialog.show();Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setDimAmount(.64f);window.setGravity(Gravity.BOTTOM);window.setLayout(-1,-2);}
        if(android.animation.ValueAnimator.areAnimatorsEnabled()){content.setTranslationY(dp(24));content.setAlpha(0);content.animate().translationY(0).alpha(1).setDuration(260).start();}
    }
    private TextView text(String value,int size,boolean bold){TextView text=new TextView(activity);text.setText(value);text.setTextSize(size);text.setTextColor(Color.WHITE);text.setTypeface(ReferenceArt.face(activity,false,bold?850:550));text.setPadding(0,dp(4),0,dp(4));return text;}
}
