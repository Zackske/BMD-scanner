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
    @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight();c.save();if(photoRatio>0){float originalW=w,originalH=h;if(w/h>photoRatio)w=h*photoRatio;else h=w/photoRatio;c.translate((originalW-w)/2,(originalH-h)/2);c.clipRect(0,0,w,h);}
        for(ScanTracker.Item i:entries){RectF r=new RectF(i.left*w,i.top*h,i.right*w,i.bottom*h);p.setStyle(Paint.Style.STROKE);p.setColor(UiArt.CYAN);p.setStrokeWidth(1.7f*d);c.drawRoundRect(r,5*d,5*d,p);String text="✓ "+i.id;p.setTextSize(12*d);p.setTypeface(Typeface.DEFAULT_BOLD);float width=p.measureText(text)+12*d,x=Math.max(0,Math.min(w-width,r.right-width*.6f)),y=Math.max(0,r.top-12*d);p.setStyle(Paint.Style.FILL);c.drawRoundRect(x,y,x+width,y+22*d,10*d,10*d,p);p.setColor(0xff002137);c.drawText(text,x+6*d,y+16*d,p);}c.restore();
    }
}
