package com.bmd.scanner;
import android.graphics.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class MetalRenderingTest {
    @Test public void metalCannotLeakBrushTransparencyIntoTheNextControlFrame(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{ThemeSkin previous=ThemeSkin.active;try{ThemeSkin.active=ThemeSkin.BMD;Paint paint=new Paint(3);Path path=new Path();path.addRect(0,0,40,40,Path.Direction.CW);Bitmap sample=Bitmap.createBitmap(40,40,Bitmap.Config.ARGB_8888);ScannerControls.metal(new Canvas(sample),path,paint,new RectF(0,0,40,40));assertEquals(255,paint.getAlpha());assertNull(paint.getShader());sample.recycle();
            Bitmap image;Canvas c;
            ScannerControls.Save save=new ScannerControls.Save(DeviceSupport.context(),"Opslaan","Opslaan");save.layout(0,0,192,192);image=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);c=new Canvas(image);for(int n=0;n<3;n++){c.drawColor(Color.WHITE);save.draw(c);assertTrue("Save face must remain navy, not transparent",ThemeSkin.luminance(image.getPixel(48,110))<.15);}image.recycle();
            for(String theme:ThemeSkin.IDS){ThemeSkin.active=ThemeSkin.forId(theme);android.graphics.drawable.Drawable glass=ScannerControls.glass(DeviceSupport.context());int size=Math.round(54*DeviceSupport.context().getResources().getDisplayMetrics().density);glass.setBounds(0,0,size,size);image=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);c=new Canvas(image);for(int n=0;n<3;n++){image.eraseColor(Color.TRANSPARENT);glass.draw(c);assertEquals("Glass face must remain opaque: "+theme,255,Color.alpha(image.getPixel(size/2,size/2)));assertEquals("No external shadow outside glass: "+theme,0,Color.alpha(image.getPixel(0,0)));}image.recycle();}
        }finally{ThemeSkin.active=previous;}});
    }
}
