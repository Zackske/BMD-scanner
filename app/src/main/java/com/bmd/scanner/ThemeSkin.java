package com.bmd.scanner;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.core.view.WindowCompat;

/** Shared application chrome. Physical camera images and lot identities never pass through it. */
final class ThemeSkin {
    static final String[] IDS={"bmd","translumina","lime","violet","light","dark"};
    static final String[] NAMES={"Blue","Red","Green","Purple","Light","Dark"};
    static final ThemeSkin BMD=new ThemeSkin("bmd",0xff00152d,0xff083d62,0xff063853,0xff00182c,0xffffffff,0xffc7dae9,0xff00dce9,0xff60a8c5);
    static final ThemeSkin TRANSLUMINA=new ThemeSkin("translumina",0xff300008,0xff9b1328,0xff740f24,0xff38000b,0xffffffff,0xffefdce1,0xffff4a62,0xffd0a4af);
    static final ThemeSkin DARK=new ThemeSkin("dark",0xff101214,0xff2b3034,0xff30363b,0xff181c20,0xfff7f9fb,0xffd0d8de,0xffb6c8d2,0xff8c9da8);
    static final ThemeSkin LIGHT=new ThemeSkin("light",0xffedf3f8,0xffffffff,0xfff9fcff,0xffdce7f0,0xff12263c,0xff41586c,0xff007c9f,0xff94b0c2);
    static final ThemeSkin LIME=new ThemeSkin("lime",0xff051d09,0xff234e10,0xff23440d,0xff0a230b,0xffffffff,0xffe0edcd,0xffc3ff39,0xff9aca60);
    static final ThemeSkin VIOLET=new ThemeSkin("violet",0xff19082d,0xff4c206a,0xff3e1c5a,0xff210c37,0xffffffff,0xffe7d8f5,0xffc58bff,0xffb498d3);
    static ThemeSkin active=BMD;
    final String id;final int background,halo,top,bottom,text,secondary,accent,edge;
    ThemeSkin(String id,int bg,int halo,int top,int bottom,int text,int secondary,int accent,int edge){this.id=id;background=bg;this.halo=halo;this.top=top;this.bottom=bottom;this.text=text;this.secondary=secondary;this.accent=accent;this.edge=edge;}
    static ThemeSkin forId(String id){if("translumina".equals(id))return TRANSLUMINA;if("dark".equals(id))return DARK;if("light".equals(id))return LIGHT;if("lime".equals(id))return LIME;if("violet".equals(id))return VIOLET;return BMD;}
    static ThemeSkin load(Context context){return forId(context.getSharedPreferences("MainActivity",0).getString("colourTheme","bmd"));}
    static void prepare(Activity activity){active=load(activity);activity.setTheme(active==LIGHT?R.style.AppThemeLight:active==TRANSLUMINA?R.style.AppThemeTranslumina:active==DARK?R.style.AppThemeDark:active==LIME?R.style.AppThemeLime:active==VIOLET?R.style.AppThemeViolet:R.style.AppTheme);}
    static void window(Activity activity){active=load(activity);WindowCompat.getInsetsController(activity.getWindow(),activity.getWindow().getDecorView()).setAppearanceLightStatusBars(active==LIGHT);WindowCompat.getInsetsController(activity.getWindow(),activity.getWindow().getDecorView()).setAppearanceLightNavigationBars(active==LIGHT&&android.os.Build.VERSION.SDK_INT>=27);activity.getWindow().setStatusBarColor(active.background);activity.getWindow().setNavigationBarColor(active==LIGHT&&android.os.Build.VERSION.SDK_INT<27?0xff142331:active.background);}
    static boolean structural(int c){return c==0xff00dcff||c==0xff00e4f5||c==0xff00d9ee||c==0xff00dff2||c==0xff00d7ea||c==0xff477a98||c==0xff216381||c==0xff426378||c==0xff256784||c==0xff167899||c==0xff25516d||c==0xff477a98;}
    static int edge(int original){return structural(original)?active.edge:original;}
    static int icon(int original){return original==0xffffffff?active.text:structural(original)?active.accent:original;}
    static GradientDrawable preview(String id,float radius){ThemeSkin s=forId(id);GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{s.halo,s.top,s.bottom});d.setCornerRadius(radius);d.setStroke(2,s.edge);return d;}
    static void footer(LinearLayout root){if(root.findViewWithTag("theme-brand")!=null)return;Context c=root.getContext();float density=c.getResources().getDisplayMetrics().density;UiArt.Brand brand=new UiArt.Brand(c);brand.setTag("theme-brand");LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Math.round(65*density));p.topMargin=Math.round(9*density);p.bottomMargin=Math.round(12*density);root.addView(brand,p);}
    int[] titleGradient(){
        if(this==TRANSLUMINA)return new int[]{0xffffc4b6,0xffff897d,0xffff685f};
        if(this==LIME)return new int[]{0xffedffca,0xffc1ff63,0xff6fe542};
        if(this==VIOLET)return new int[]{0xfff3ddff,0xffd5a2ff,0xffa86aec};
        if(this==LIGHT)return new int[]{0xff00687c,0xff345db0,0xff752d9c};
        if(this==DARK)return new int[]{0xfff0f6fa,0xffc3d8e4,0xff91acbf};
        return new int[]{0xff7af4ee,0xff89c8ef,0xffa975df};
    }
    int mistInk(){return this==TRANSLUMINA?0xffff5341:this==LIGHT?0xff2781b1:this==DARK?0xffa0bdcf:accent;}
    ColorFilter brandFilter(){
        // Source logos contain a partial white matte as well as real alpha.
        // Reject that matte at render time, preserving the original glyph asset.
        float[] matrix={1,0,0,0,0,0,1,0,0,0,0,0,1,0,0,-5.1024f,-17.1648f,-1.7328f,24,-840};
        if(this==TRANSLUMINA){matrix[0]=.3f;matrix[4]=178;matrix[5]=-.7f;matrix[9]=178;matrix[10]=-.7f;matrix[14]=178;}
        else if(this!=LIGHT){int ink=this==BMD?0xffd7f3ff:this==LIME?0xffe6ffd1:this==VIOLET?0xffedddff:0xfff4f7f9;matrix[0]=matrix[6]=matrix[12]=0;matrix[4]=Color.red(ink);matrix[9]=Color.green(ink);matrix[14]=Color.blue(ink);}
        return new ColorMatrixColorFilter(new ColorMatrix(matrix));
    }
    static double channel(int n){double x=n/255.0;return x<=.04045?x/12.92:Math.pow((x+.055)/1.055,2.4);}
    static double luminance(int c){return .2126*channel((c>>16)&255)+.7152*channel((c>>8)&255)+.0722*channel(c&255);}
    static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
}

