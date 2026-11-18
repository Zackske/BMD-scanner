package com.bmd.scanner;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.*;

final class ScanOverlay extends View {
    private ProductCatalog catalog=new ProductCatalog();void catalog(ProductCatalog value){catalog=value;}
    private float photoRatio=0;void photoAspect(float ratio){photoRatio=ratio;invalidate();}
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private List<ScanTracker.Item> entries=new ArrayList<>();
    ScanOverlay(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    void update(List<ScanTracker.Item> items){entries=items;invalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight();c.save();if(photoRatio==0){float left=w*.115f,right=w*.885f,top=h*.10f,bottom=Math.max(h*.35f,h-140*d),radius=Math.min(w*.065f,h*.06f),length=Math.min(w*.10f,h*.12f);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*d);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(0xff35f1ff);p.setShadowLayer(2*d,0,0,0xff00dcff);Path[] corners=new Path[4];for(int n=0;n<4;n++){boolean rightSide=n%2==1,bottomSide=n>1;float x=rightSide?right:left,y=bottomSide?bottom:top,sx=rightSide?-1:1,sy=bottomSide?-1:1;Path path=new Path();path.moveTo(x+sx*length,y);path.lineTo(x+sx*radius,y);path.quadTo(x,y,x,y+sy*radius);path.lineTo(x,y+sy*length);c.drawPath(path,p);}p.clearShadowLayer();}if(photoRatio>0){float originalW=w,originalH=h;if(w/h>photoRatio)w=h*photoRatio;else h=w/photoRatio;c.translate((originalW-w)/2,(originalH-h)/2);c.clipRect(0,0,w,h);}
        for(ProductCatalog.Product product:catalog.view(entries)){ScanTracker.Item i=product.codes.get(0);String label=(product.conflict?"! ":"✓ ")+product.number;p.setTextSize(13*d);p.setTypeface(Typeface.DEFAULT_BOLD);float width=p.measureText(label)+14*d,x=Math.max(0,Math.min(w-width,i.right*w)),y=Math.max(0,Math.min(h-24*d,i.top*h));p.setStyle(Paint.Style.FILL);p.setColor(0xee001b2d);c.drawRoundRect(x,y,x+width,y+24*d,11*d,11*d,p);p.setColor(product.conflict?0xffabb8c4:product.colour);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.4f*d);c.drawRoundRect(x,y,x+width,y+24*d,11*d,11*d,p);p.setStyle(Paint.Style.FILL);c.drawText(product.conflict?"!":"✓",x+5*d,y+17*d,p);p.setColor(Color.WHITE);c.drawText(Integer.toString(product.number),x+23*d,y+17*d,p);}c.restore();
    }
}

