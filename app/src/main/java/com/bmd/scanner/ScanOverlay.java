package com.bmd.scanner;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.*;

final class ScanOverlay extends View {
    private ProductCatalog catalog=new ProductCatalog();void catalog(ProductCatalog value){catalog=value;}
    private List<ScanTracker.Detection> candidates=new ArrayList<>();void candidates(List<ScanTracker.Detection> value){candidates=value;invalidate();}
    private float photoRatio=0;void photoAspect(float ratio){photoRatio=ratio;invalidate();}
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private List<ScanTracker.Item> entries=new ArrayList<>();
    ScanOverlay(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    void update(List<ScanTracker.Item> items){entries=items;invalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight();c.save();if(photoRatio>0){float originalW=w,originalH=h;if(w/h>photoRatio)w=h*photoRatio;else h=w/photoRatio;c.translate((originalW-w)/2,(originalH-h)/2);c.clipRect(0,0,w,h);}
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f*d);p.setColor(Color.WHITE);p.setPathEffect(new DashPathEffect(new float[]{5*d,4*d},0));for(ScanTracker.Detection unknown:candidates)c.drawRoundRect(new RectF(unknown.left*w,unknown.top*h,unknown.right*w,unknown.bottom*h),3*d,3*d,p);p.setPathEffect(null);p.setStyle(Paint.Style.FILL);p.setTextSize(14*d);for(ScanTracker.Detection unknown:candidates)c.drawText("?",unknown.left*w,Math.max(14*d,unknown.top*h),p);
        for(ProductCatalog.Product product:catalog.view(entries)){for(ScanTracker.Item code:product.codes){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*d);p.setColor(product.conflict?0xffabb8c4:product.colour);c.drawRoundRect(new RectF(code.left*w,code.top*h,code.right*w,code.bottom*h),3*d,3*d,p);}ScanTracker.Item i=product.codes.get(0);String label=(product.conflict?"! ":"✓ ")+product.number;p.setTextSize(13*d);p.setTypeface(Typeface.DEFAULT_BOLD);float width=p.measureText(label)+14*d,x=Math.max(0,Math.min(w-width,i.right*w)),y=Math.max(0,Math.min(h-24*d,i.top*h));p.setStyle(Paint.Style.FILL);p.setColor(0xee001b2d);c.drawRoundRect(x,y,x+width,y+24*d,11*d,11*d,p);p.setColor(product.conflict?0xffabb8c4:product.colour);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.4f*d);c.drawRoundRect(x,y,x+width,y+24*d,11*d,11*d,p);p.setStyle(Paint.Style.FILL);c.drawText(product.conflict?"!":"✓",x+5*d,y+17*d,p);p.setColor(Color.WHITE);c.drawText(Integer.toString(product.number),x+23*d,y+17*d,p);}c.restore();
    }
}

