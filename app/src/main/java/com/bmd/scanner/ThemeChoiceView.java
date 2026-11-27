package com.bmd.scanner;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;

/** Native theme choice; the green selected badge never inherits the active palette. */
final class ThemeChoiceView extends FrameLayout {
    private final View emblem;
    private final TextView label;
    private final SelectionBadge marker;
    ThemeChoiceView(Context context,ThemeSkin skin,String name){
        super(context);setTag("theme-"+skin.id);setBackground(ThemeSkin.preview(skin.id,dp(18)));setFocusable(true);setClickable(true);
        emblem=skin==ThemeSkin.BMD||skin==ThemeSkin.TRANSLUMINA?new CompactBrand(context,skin):new UiArt.Magic(context,skin==ThemeSkin.DARK?"moon":skin==ThemeSkin.LIGHT?"sun":skin==ThemeSkin.LIME?"leaf":"gem",skin==ThemeSkin.DARK?0xffb7cbdc:skin==ThemeSkin.LIGHT?0xffffb826:skin==ThemeSkin.LIME?0xffc4ff42:0xffc184ff).unboxed().fixedRing();
        emblem.setTag("theme-icon-"+skin.id);emblem.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);addView(emblem);
        label=new TextView(context);label.setText(name);label.setTextColor(skin.text);label.setTypeface(ReferenceArt.face(context,false,850));label.setGravity(Gravity.CENTER);label.setSingleLine();label.setAutoSizeTextTypeUniformWithConfiguration(11,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);label.setTag("theme-name-"+skin.id);label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);addView(label);
        marker=new SelectionBadge(context);marker.setTag("theme-check-"+skin.id);marker.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);addView(marker);setContentDescription(name);
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    void choose(boolean selected,boolean animate){
        boolean changed=isSelected()!=selected;setSelected(selected);marker.animate().cancel();
        marker.setVisibility(selected?VISIBLE:INVISIBLE);marker.setAlpha(1);marker.setScaleX(1);marker.setScaleY(1);
        if(selected&&changed&&animate&&android.animation.ValueAnimator.areAnimatorsEnabled()){marker.setAlpha(0);marker.setScaleX(.72f);marker.setScaleY(.72f);marker.animate().alpha(1).scaleX(1).scaleY(1).setDuration(200).setInterpolator(new android.view.animation.OvershootInterpolator(.8f)).start();}
        requestLayout();
    }
    @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);setMeasuredDimension(w,h);int iconH=isSelected()?dp(28):dp(62);emblem.measure(MeasureSpec.makeMeasureSpec(Math.max(1,w-dp(22)),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(iconH,MeasureSpec.EXACTLY));label.measure(MeasureSpec.makeMeasureSpec(Math.max(1,w-dp(12)),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(dp(28),MeasureSpec.EXACTLY));int size=Math.min(dp(60),Math.round(h*.44f));marker.measure(MeasureSpec.makeMeasureSpec(size,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(size,MeasureSpec.EXACTLY));}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){int w=r-l,h=b-t,eh=emblem.getMeasuredHeight(),ey=isSelected()?dp(7):Math.max(dp(9),(h-dp(38)-eh)/2);emblem.layout(dp(11),ey,w-dp(11),ey+eh);label.layout(dp(6),h-dp(34),w-dp(6),h-dp(6));int s=marker.getMeasuredWidth();marker.layout((w-s)/2,(h-s)/2,(w+s)/2,(h+s)/2);}
    @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setCheckable(true);info.setChecked(isSelected());}

    private static final class CompactBrand extends View {
        private final ThemeSkin identity;private final Bitmap art;private final Rect source;private final Paint paint=new Paint(3);
        CompactBrand(Context c,ThemeSkin skin){super(c);identity=skin;Bitmap original=ReferenceArt.bitmap(c,skin==ThemeSkin.TRANSLUMINA?R.drawable.translumina_logo:R.drawable.blue_medical_logo);art=Bitmap.createBitmap(original.getWidth(),original.getHeight(),Bitmap.Config.ARGB_8888);paint.setColorFilter(skin.brandFilter());new Canvas(art).drawBitmap(original,0,0,paint);paint.setColorFilter(null);int w=art.getWidth(),h=art.getHeight(),left=w,top=h,right=0,bottom=0;int[] pixels=new int[w*h];art.getPixels(pixels,0,w,0,0,w,h);for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(Color.alpha(pixels[y*w+x])>2){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);}source=right>left?new Rect(left,top,right,bottom):new Rect(0,0,w,h);}
        @Override protected void onDraw(Canvas c){RectF frame=new RectF(0,0,getWidth(),getHeight());int ink=identity==ThemeSkin.TRANSLUMINA?0xffff3454:0xff14aaff;OrbitalArtwork.orbit(c,getContext(),frame,ink,false);float scale=Math.min(getWidth()*.73f/source.width(),getHeight()*.62f/source.height()),w=source.width()*scale,h=source.height()*scale;float y=getHeight()*.47f;c.drawBitmap(art,source,new RectF((getWidth()-w)/2,y-h/2,(getWidth()+w)/2,y+h/2),paint);OrbitalArtwork.orbit(c,getContext(),frame,ink,true);float size=Math.min(getWidth(),getHeight())*.20f;OrbitalArtwork.star(c,getContext(),new RectF(getWidth()*.85f-size/2,0,getWidth()*.85f+size/2,size));}
    }
    private static final class SelectionBadge extends View {
        private final Paint p=new Paint(3);
        SelectionBadge(Context c){super(c);}
        @Override protected void onDraw(Canvas c){float u=getWidth()/100f;c.save();c.scale(u,u);p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(32,22,80,new int[]{0xff10778c,0xff00334b,0xff000e20},new float[]{0,.5f,1},Shader.TileMode.CLAMP));c.drawCircle(50,50,46,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setShader(new LinearGradient(15,5,85,95,new int[]{0xffd8ffb3,0xff7dff38,0xff16c70a},null,Shader.TileMode.CLAMP));c.drawCircle(50,50,46,p);p.setShader(null);
            p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,7,0,55,new int[]{0x7cffffff,0x00ffffff},null,Shader.TileMode.CLAMP));c.drawOval(18,11,82,47,p);p.setShader(null);
            Path check=new Path();check.moveTo(27,51);check.lineTo(43,67);check.lineTo(75,33);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(11);p.setShader(new LinearGradient(0,27,0,78,new int[]{0xffb6ff6c,0xff4cf122,0xff1bd40c},null,Shader.TileMode.CLAMP));c.drawPath(check,p);p.setShader(null);p.setStyle(Paint.Style.FILL);c.restore();
        }
    }
}
