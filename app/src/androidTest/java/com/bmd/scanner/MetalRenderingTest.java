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
            HorizontalZoom zoom=new HorizontalZoom(DeviceSupport.context());zoom.setup(.5f,10,1,java.util.Locale.ENGLISH,v->{});zoom.layout(0,0,540,92);Bitmap image=Bitmap.createBitmap(540,92,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);for(int n=0;n<3;n++){c.drawColor(Color.WHITE);zoom.draw(c);int colour=image.getPixel(270,65);assertTrue("Navy surface must stay opaque after repeated brushed-metal draws",ThemeSkin.luminance(colour)<.15);}image.recycle();
            ScannerControls.Save save=new ScannerControls.Save(DeviceSupport.context(),"Opslaan","Opslaan");save.layout(0,0,192,192);image=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);c=new Canvas(image);for(int n=0;n<3;n++){c.drawColor(Color.WHITE);save.draw(c);assertTrue("Save face must remain navy, not transparent",ThemeSkin.luminance(image.getPixel(48,110))<.15);}image.recycle();
        }finally{ThemeSkin.active=previous;}});
    }
}
