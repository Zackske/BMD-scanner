package com.bmd.scanner;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.*;

final class ScanOverlay extends View {
    private float photoRatio=0;void photoAspect(float ratio){photoRatio=ratio;invalidate();}
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private List<ScanTracker.Item> entries=new ArrayList<>();
    ScanOverlay(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    void update(List<ScanTracker.Item> items){entries=items;invalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight();c.save();if(photoRatio==0){float left=w*.10f,right=w*.82f,top=h*.16f,bottom=h*.78f,radius=Math.min(w*.065f,h*.06f),length=Math.min(w*.10f,h*.12f);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*d);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(0xff35f1ff);p.setShadowLayer(7*d,0,0,0xff00dcff);Path[] corners=new Path[4];for(int n=0;n<4;n++){boolean rightSide=n%2==1,bottomSide=n>1;float x=rightSide?right:left,y=bottomSide?bottom:top,sx=rightSide?-1:1,sy=bottomSide?-1:1;Path path=new Path();path.moveTo(x+sx*length,y);path.lineTo(x+sx*radius,y);path.quadTo(x,y,x,y+sy*radius);path.lineTo(x,y+sy*length);c.drawPath(path,p);}p.clearShadowLayer();}if(photoRatio>0){float originalW=w,originalH=h;if(w/h>photoRatio)w=h*photoRatio;else h=w/photoRatio;c.translate((originalW-w)/2,(originalH-h)/2);c.clipRect(0,0,w,h);}
        for(ScanTracker.Item i:entries){RectF r=new RectF(i.left*w,i.top*h,i.right*w,i.bottom*h);p.setStyle(Paint.Style.STROKE);p.setColor(UiArt.CYAN);p.setStrokeWidth(1.7f*d);c.drawRoundRect(r,5*d,5*d,p);String text="✓ "+i.id;p.setTextSize(12*d);p.setTypeface(Typeface.DEFAULT_BOLD);float width=p.measureText(text)+12*d,x=Math.max(0,Math.min(w-width,r.right-width*.6f)),y=Math.max(0,r.top-12*d);p.setStyle(Paint.Style.FILL);c.drawRoundRect(x,y,x+width,y+22*d,10*d,10*d,p);p.setColor(0xff002137);c.drawText(text,x+6*d,y+16*d,p);}c.restore();
    }
}
