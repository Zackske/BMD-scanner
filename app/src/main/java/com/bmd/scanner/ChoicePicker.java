package com.bmd.scanner;

import android.animation.*;
import android.app.*;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.*;
import android.widget.*;
import java.util.function.IntConsumer;

/** The approved light-trail wheel and flag ribbon, rendered with native controls. */
final class ChoicePicker extends Dialog {
    final LinearLayout panel,heading;
    final TextView title,apply;
    final ImageButton close;
    final Surface surface;
    private final Close closeArtwork=new Close();
    private ValueAnimator paletteAnimation;
    private int buttonTop=ThemeSkin.active==ThemeSkin.BMD?ThemeSkin.active.accent:ThemeSkin.active.top;
    private boolean closing,finished;
    ChoicePicker(Activity activity,String icon,String caption,String applyText,String closeText){
        super(activity);setOwnerActivity(activity);panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(16),dp(18),dp(16),dp(18));panel.setClipChildren(false);panel.setClipToPadding(false);
        surface=new Surface(ThemeSkin.active);panel.setBackground(surface);
        heading=new LinearLayout(activity);heading.setGravity(Gravity.CENTER_VERTICAL);
        View mark=MenuHeader.icon(activity,icon);heading.addView(mark,new LinearLayout.LayoutParams(dp(34),dp(34)));
        title=label(activity,caption,21);title.setTag("sheet-title");title.setMaxLines(2);title.setAutoSizeTextTypeUniformWithConfiguration(16,21,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(44),1);tp.setMargins(dp(9),0,dp(5),0);heading.addView(title,tp);
        close=new ImageButton(activity);close.setTag("picker-close");close.setImageDrawable(closeArtwork);close.setBackgroundColor(Color.TRANSPARENT);close.setContentDescription(closeText);close.setOnClickListener(v->dismiss());heading.addView(close,new LinearLayout.LayoutParams(dp(44),dp(44)));panel.addView(heading);
        apply=label(activity,applyText,17);apply.setGravity(Gravity.CENTER);apply.setBackground(UiArt.card(ThemeSkin.active.accent,ThemeSkin.active.bottom,ThemeSkin.active.accent,dp(18)));apply.setClickable(true);apply.setFocusable(true);
        setContentView(panel);setCanceledOnTouchOutside(true);
    }
    void body(View body,int preferredHeight){
        int usable=getContext().getResources().getDisplayMetrics().heightPixels,maximum=Math.max(dp(300),(int)(usable*.88f)-dp(144));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,Math.min(dp(preferredHeight),maximum));bp.setMargins(0,dp(8),0,dp(12));panel.addView(body,bp);panel.addView(apply,new LinearLayout.LayoutParams(-1,dp(48)));
    }
    void words(String caption,String action,String closeText,boolean rtl){title.setText(caption);apply.setText(action);apply.setContentDescription(action);close.setContentDescription(closeText);panel.setLayoutDirection(rtl?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);}
    void theme(ThemeSkin next,boolean animate){
        if(paletteAnimation!=null)paletteAnimation.cancel();
        final int fromBase=surface.baseColour,fromEdge=surface.edgeColour,fromText=title.getCurrentTextColor(),fromTop=buttonTop,targetTop=next==ThemeSkin.BMD?next.accent:next.top;
        final int[] before=surface.colours.clone(),after={next.halo,next.top,next.bottom};
        final Wheel wheel=(Wheel)panel.findViewWithTag("theme-wheel");
        java.util.function.DoubleConsumer frame=value->{
            float f=(float)value;int accent=UiArt.mix(fromEdge,next.accent,f),base=UiArt.mix(fromBase,next.bottom,f),ink=UiArt.mix(fromText,next.text,f);
            int[] colours=new int[3];for(int i=0;i<3;i++)colours[i]=UiArt.mix(before[i],after[i],f);
            surface.palette(base,accent,colours);title.setTextColor(ink);closeArtwork.colour(ink);apply.setTextColor(ink);
            buttonTop=UiArt.mix(fromTop,targetTop,f);apply.setBackground(UiArt.paletteCard(buttonTop,base,accent,base,dp(18)));if(wheel!=null)wheel.palette(accent,ink);
        };
        if(!animate||!ValueAnimator.areAnimatorsEnabled()){frame.accept(1);return;}
        paletteAnimation=ValueAnimator.ofFloat(0,1);paletteAnimation.setDuration(UiArt.TURN_MS);paletteAnimation.setInterpolator(new DecelerateInterpolator());paletteAnimation.addUpdateListener(a->frame.accept((float)a.getAnimatedValue()));paletteAnimation.start();
    }
    @Override public void show(){super.show();Window window=getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setDimAmount(.6f);window.setGravity(Gravity.CENTER);window.setLayout(getContext().getResources().getDisplayMetrics().widthPixels-dp(16),-2);}if(ValueAnimator.areAnimatorsEnabled()){panel.setAlpha(0);panel.setScaleX(.97f);panel.setScaleY(.97f);panel.setTranslationY(dp(14));panel.animate().alpha(1).scaleX(1).scaleY(1).translationY(0).setDuration(300).setInterpolator(new OvershootInterpolator(.6f)).start();}}
    @Override public void dismiss(){if(finished||!isShowing()){super.dismiss();return;}if(closing)return;closing=true;if(!ValueAnimator.areAnimatorsEnabled()){finishDismiss();return;}panel.animate().cancel();panel.animate().alpha(0).translationY(dp(10)).scaleX(.985f).scaleY(.985f).setDuration(180).setInterpolator(new AccelerateInterpolator()).withEndAction(this::finishDismiss).start();}
    private void stopPalette(){if(paletteAnimation!=null)paletteAnimation.cancel();if(surface.animation!=null)surface.animation.cancel();}
    private void finishDismiss(){stopPalette();finished=true;super.dismiss();}
    void dispose(){panel.animate().cancel();stopPalette();finished=true;super.dismiss();}
    private int dp(float value){return Math.round(value*getContext().getResources().getDisplayMetrics().density);}
    static TextView label(Context c,String value,int size){TextView t=new TextView(c);t.setText(value);t.setTextColor(ThemeSkin.active.text);t.setTypeface(ReferenceArt.face(c,false,850));t.setTextSize(size);t.setGravity(Gravity.CENTER_VERTICAL);t.setIncludeFontPadding(false);return t;}
    private static final class Close extends Drawable {
        private final Paint paint=new Paint(3);
        private int ink=ThemeSkin.active.text;
        void colour(int next){ink=next;invalidateSelf();}
        @Override public void draw(Canvas c){Rect b=getBounds();paint.setColor(ink);paint.setStrokeWidth(Math.max(2,b.width()*.055f));paint.setStrokeCap(Paint.Cap.ROUND);c.drawLine(b.left+b.width()*.3f,b.top+b.height()*.3f,b.left+b.width()*.7f,b.top+b.height()*.7f,paint);c.drawLine(b.left+b.width()*.7f,b.top+b.height()*.3f,b.left+b.width()*.3f,b.top+b.height()*.7f,paint);}
        @Override public void setAlpha(int a){paint.setAlpha(a);}@Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    static final class Surface extends Drawable {
        private final Paint p=new Paint(3);private int baseColour,edgeColour;private int[] colours;private ValueAnimator animation;
        Surface(ThemeSkin skin){baseColour=skin.bottom;edgeColour=skin.accent;colours=new int[]{skin.halo,skin.top,skin.bottom};}
        void palette(int base,int edge,int[] next){baseColour=base;edgeColour=edge;colours=next;invalidateSelf();}
        void colours(int[] next,boolean animate){if(animation!=null)animation.cancel();int[] before=colours.clone();if(!animate||!ValueAnimator.areAnimatorsEnabled()){colours=next.clone();invalidateSelf();return;}animation=ValueAnimator.ofFloat(0,1);animation.setDuration(UiArt.TURN_MS);animation.addUpdateListener(a->{float f=(float)a.getAnimatedValue();for(int i=0;i<3;i++)colours[i]=UiArt.mix(before[i],next[i],f);invalidateSelf();});animation.start();}
        @Override public void draw(Canvas c){Rect b=getBounds();float radius=23*c.getDensity()/160f;if(radius<20)radius=23;Path clip=new Path();clip.addRoundRect(new RectF(b),radius,radius,Path.Direction.CW);c.save();c.clipPath(clip);c.drawColor(baseColour);float[] x={.03f,.98f,.05f},y={.08f,.46f,1f},strength={.46f,.32f,.4f};for(int i=0;i<3;i++){int colour=(colours[i]&0x00ffffff)|(Math.round(255*strength[i])<<24);p.setShader(new RadialGradient(b.left+b.width()*x[i],b.top+b.height()*y[i],Math.max(b.width(),b.height())*.78f,colour,Color.TRANSPARENT,Shader.TileMode.CLAMP));c.drawRect(b,p);}p.setShader(null);c.restore();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(edgeColour);c.drawRoundRect(new RectF(b.left+1,b.top+1,b.right-1,b.bottom-1),radius,radius,p);p.setStyle(Paint.Style.FILL);}
        @Override public void setAlpha(int a){}@Override public void setColorFilter(ColorFilter f){}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    abstract static class Layout extends FrameLayout {
        final Paint paint=new Paint(3);int accent,ink;float angle;ValueAnimator trail;
        Layout(Context c){super(c);accent=ThemeSkin.active.accent;ink=ThemeSkin.active.text;setWillNotDraw(false);setClipChildren(false);setClipToPadding(false);setLayoutDirection(View.LAYOUT_DIRECTION_LTR);}
        int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
        void turn(float target,boolean animate){if(trail!=null)trail.cancel();if(!animate||!ValueAnimator.areAnimatorsEnabled()){angle=target;invalidate();return;}trail=ValueAnimator.ofFloat(angle,target);trail.setDuration(UiArt.TURN_MS);trail.setInterpolator(new DecelerateInterpolator());trail.addUpdateListener(a->{angle=(float)a.getAnimatedValue();invalidate();});trail.start();}
        void orbit(Canvas c,float cx,float cy,float radius){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor((accent&0xffffff)|0x33000000);c.drawCircle(cx,cy,radius,paint);c.save();c.rotate(angle,cx,cy);paint.setShader(new SweepGradient(cx,cy,new int[]{Color.TRANSPARENT,accent,Color.WHITE,Color.TRANSPARENT},new float[]{0,.28f,.34f,.36f}));paint.setStrokeWidth(dp(1.5f));c.drawCircle(cx,cy,radius,paint);paint.setShader(null);paint.setStyle(Paint.Style.FILL);c.restore();}
        void box(View v,int w,int h){v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,w),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,h),MeasureSpec.EXACTLY));}
        void at(View v,float x,float y){int w=v.getMeasuredWidth(),h=v.getMeasuredHeight();v.layout(Math.round(x-w/2f),Math.round(y-h/2f),Math.round(x-w/2f)+w,Math.round(y-h/2f)+h);}
        void pop(View v,boolean animate){v.animate().cancel();v.setScaleX(1);v.setScaleY(1);if(animate&&ValueAnimator.areAnimatorsEnabled()){v.setScaleX(.85f);v.setScaleY(.85f);v.animate().scaleX(1).scaleY(1).setDuration(320).setInterpolator(new OvershootInterpolator(.6f)).start();}}
        @Override protected void onDetachedFromWindow(){if(trail!=null)trail.cancel();super.onDetachedFromWindow();}
    }
    static final class Wheel extends Layout {
        private final FrameLayout[] choices=new FrameLayout[5];private final ImageView centre;private int selected;
        private final float[][] points={{.5f,.09f},{.845f,.37f},{.73f,.84f},{.27f,.84f},{.155f,.37f}};
        private final int[] art={R.drawable.picker_theme_blue,R.drawable.picker_theme_red,R.drawable.picker_theme_green,R.drawable.picker_theme_purple,R.drawable.picker_theme_dark};
        Wheel(Context c,String[] names,String selectedId,IntConsumer changed){super(c);setTag("theme-wheel");for(int i=0;i<5;i++){final int index=i;FrameLayout button=new FrameLayout(c);button.setTag("theme-"+ThemeSkin.IDS[i]);button.setClickable(true);button.setFocusable(true);button.setContentDescription(names[i]);ImageView image=new ImageView(c);image.setImageResource(art[i]);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setTag("theme-icon-"+ThemeSkin.IDS[i]);image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(-1,dp(84),Gravity.TOP|Gravity.CENTER_HORIZONTAL);button.addView(image,ip);TextView label=label(c,names[i],14);label.setGravity(Gravity.CENTER);label.setSingleLine();label.setAutoSizeTextTypeUniformWithConfiguration(11,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);label.setTag("theme-name-"+ThemeSkin.IDS[i]);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,dp(24),Gravity.BOTTOM);button.addView(label,lp);button.setOnClickListener(v->{select(index,true);changed.accept(index);});choices[i]=button;addView(button);if(ThemeSkin.IDS[i].equals(selectedId))selected=i;}
            centre=new ImageView(c);centre.setTag("theme-selected-icon");centre.setScaleType(ImageView.ScaleType.FIT_CENTER);centre.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);addView(centre);select(selected,false);
        }
        void select(int n,boolean animate){selected=n;for(int i=0;i<choices.length;i++){choices[i].setSelected(i==n);choices[i].setActivated(i==n);View image=choices[i].getChildAt(0);image.setScaleX(i==n?1.08f:1);image.setScaleY(i==n?1.08f:1);TextView label=(TextView)choices[i].getChildAt(1);label.setTextColor(i==n?accent:ink);label.setAlpha(i==n?1:.8f);}centre.setImageResource(art[n]);centre.setContentDescription(ThemeSkin.IDS[n]);pop(centre,animate);turn(n*72,animate);}
        void palette(int nextAccent,int nextInk){accent=nextAccent;ink=nextInk;for(int i=0;i<choices.length;i++)((TextView)choices[i].getChildAt(1)).setTextColor(i==selected?accent:ink);invalidate();}
        @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);setMeasuredDimension(w,h);float scale=Math.min(1,Math.min(w/(float)dp(310),h/(float)dp(420)));for(View v:choices)box(v,Math.round(dp(96)*scale),Math.round(dp(116)*scale));box(centre,Math.round(dp(84)*scale),Math.round(dp(84)*scale));}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){float scale=Math.min(1,Math.min(getWidth()/(float)dp(310),getHeight()/(float)dp(420))),w=dp(310)*scale,h=dp(350)*scale,x=(getWidth()-w)/2,y=(getHeight()-h)/2;for(int i=0;i<5;i++)at(choices[i],x+points[i][0]*w,y+points[i][1]*h);at(centre,getWidth()/2f,getHeight()/2f);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);orbit(c,getWidth()/2f,getHeight()/2f,Math.min(getWidth()*.21f,dp(65)));View chosen=choices[selected];float x=(chosen.getLeft()+chosen.getRight())/2f,y=chosen.getBottom()+dp(2);paint.setColor(accent);paint.setStrokeWidth(dp(2));paint.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x-dp(14),y,x+dp(14),y,paint);}
    }
    static final class Flags extends Layout {
        private final Flag[] choices=new Flag[15];private final ImageView centre;private final TextView name;private int selected;
        Flags(Context c,String language,IntConsumer changed){super(c);setTag("language-options");for(int i=0;i<15;i++){final int index=i;Flag button=new Flag(c,i);button.setTag("language-"+LanguageCatalog.CODES[i]);button.setOnClickListener(v->{select(index,true);changed.accept(index);});choices[i]=button;addView(button);}centre=new ImageView(c);centre.setTag("language-selected-flag");centre.setScaleType(ImageView.ScaleType.FIT_CENTER);centre.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);addView(centre);name=label(c,"",22);name.setTag("language-selected-name");name.setGravity(Gravity.CENTER);name.setSingleLine();name.setAutoSizeTextTypeUniformWithConfiguration(17,22,1,android.util.TypedValue.COMPLEX_UNIT_SP);name.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);addView(name);select(LanguageCatalog.index(language),false);}
        void select(int n,boolean animate){selected=n;for(int i=0;i<15;i++){choices[i].setSelected(i==n);choices[i].invalidate();}centre.setImageResource(LanguageCatalog.FLAGS[n]);name.setText(LanguageCatalog.NAMES[n]);name.setTextDirection(LanguageCatalog.rtl(LanguageCatalog.CODES[n])?TEXT_DIRECTION_RTL:TEXT_DIRECTION_LTR);pop(centre,animate);turn(n*24,animate);}
        @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);setMeasuredDimension(w,h);for(View v:choices)box(v,Math.min(dp(52),w/5),dp(48));box(centre,dp(104),dp(78));box(name,Math.min(dp(190),w),dp(34));}
        float top(){return Math.max(0,(getHeight()-dp(409))/2f);}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){float top=top(),hero=top+dp(88),rowTop=top+dp(229);at(centre,getWidth()/2f,hero);at(name,getWidth()/2f,top+dp(199));for(int i=0;i<15;i++)at(choices[i],(i%5+.5f)*getWidth()/5f,rowTop+dp(24)+(i/5)*dp(63));}
        @Override protected void onDraw(Canvas c){super.onDraw(c);orbit(c,getWidth()/2f,top()+dp(88),dp(87));}
        private static final class Flag extends View {
            final Bitmap art;final Paint p=new Paint(3);Flag(Context c,int index){super(c);art=ReferenceArt.bitmap(c,LanguageCatalog.FLAGS[index]);setClickable(true);setFocusable(true);setContentDescription(LanguageCatalog.NAMES[index]);}
            @Override protected void onDraw(Canvas c){float density=getResources().getDisplayMetrics().density,w=Math.min(42*density,getWidth()-8*density)*(isSelected()?1.08f:1),h=w*.75f,x=(getWidth()-w)/2,y=(getHeight()-h)/2;RectF rect=new RectF(x,y,x+w,y+h);Path path=new Path();path.addRoundRect(rect,3*density,3*density,Path.Direction.CW);c.save();c.clipPath(path);c.drawBitmap(art,null,rect,p);c.restore();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth((isSelected()?2:1)*density);p.setColor(isSelected()?ThemeSkin.active.accent:0xffbdcbd7);c.drawRoundRect(rect,3*density,3*density,p);p.setStyle(Paint.Style.FILL);if(isSelected()){p.setColor(ThemeSkin.active.accent);c.drawCircle(getWidth()/2f,getHeight()-2*density,2*density,p);}}
            @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.RadioButton");info.setCheckable(true);info.setChecked(isSelected());}
        }
    }
}
