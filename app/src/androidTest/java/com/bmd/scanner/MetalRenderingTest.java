package com.bmd.scanner;
import android.graphics.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class MetalRenderingTest {
    @Test public void sharedArtworkKeepsGoldSparkleAndRingColourAcrossThemes(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{ThemeSkin previous=ThemeSkin.active;try{
            int[] colours={0xff14aaff,0xffff3454,0xff18c46b,0xffb741ed,0xffffa51d,0xffffd75c,0xffff8fc5,0xffa0b6c7};
            assertEquals("Every theme has a rendering colour",ThemeSkin.IDS.length,colours.length);
            for(int n=0;n<ThemeSkin.IDS.length;n++){
                ThemeSkin.active=ThemeSkin.forId(ThemeSkin.IDS[n]);Bitmap image=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
                OrbitalArtwork.draw(new Canvas(image),DeviceSupport.context(),R.drawable.ref_scan_final_check,new RectF(0,0,256,256),colours[n]);
                int gold=0,ring=0;float[] hsv=new float[3],expected=new float[3];Color.colorToHSV(colours[n],expected);
                for(int y=0;y<256;y++)for(int x=0;x<256;x++){int pixel=image.getPixel(x,y);if(Color.alpha(pixel)<180)continue;Color.colorToHSV(pixel,hsv);
                    if(x>190&&y<65&&hsv[0]>25&&hsv[0]<65&&hsv[1]>.35f&&hsv[2]>.5f)gold++;
                    if(y>100&&(x<30||x>225||y>220)&&hsv[1]>.10f&&hsv[2]>.25f){float difference=Math.abs(hsv[0]-expected[0]);if(Math.min(difference,360-difference)<20)ring++;}
                }
                assertTrue("Gold sparkle must remain visible in "+ThemeSkin.IDS[n],gold>25);
                assertTrue("Shared ring must follow its tile colour in "+ThemeSkin.IDS[n],ring>25);
                assertEquals("Transparent corner must remain clean",0,Color.alpha(image.getPixel(0,0)));image.recycle();
            }
        }finally{ThemeSkin.active=previous;}});
    }
    @Test public void metalCannotLeakBrushTransparencyIntoTheNextControlFrame(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{ThemeSkin previous=ThemeSkin.active;try{ThemeSkin.active=ThemeSkin.BMD;Paint paint=new Paint(3);Path path=new Path();path.addRect(0,0,40,40,Path.Direction.CW);Bitmap sample=Bitmap.createBitmap(40,40,Bitmap.Config.ARGB_8888);ScannerControls.metal(new Canvas(sample),path,paint,new RectF(0,0,40,40));assertEquals(255,paint.getAlpha());assertNull(paint.getShader());sample.recycle();
            Bitmap image;Canvas c;
            ScannerControls.Save save=new ScannerControls.Save(DeviceSupport.context(),"Opslaan","Opslaan");save.layout(0,0,192,192);image=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);c=new Canvas(image);for(int n=0;n<3;n++){c.drawColor(Color.WHITE);save.draw(c);assertTrue("Save face must remain navy, not transparent",ThemeSkin.luminance(image.getPixel(48,110))<.15);}image.recycle();
            for(String theme:ThemeSkin.IDS){ThemeSkin.active=ThemeSkin.forId(theme);android.graphics.drawable.Drawable glass=ScannerControls.glass(DeviceSupport.context());int size=Math.round(54*DeviceSupport.context().getResources().getDisplayMetrics().density);glass.setBounds(0,0,size,size);image=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);c=new Canvas(image);for(int n=0;n<3;n++){image.eraseColor(Color.TRANSPARENT);glass.draw(c);assertEquals("Glass face must remain opaque: "+theme,255,Color.alpha(image.getPixel(size/2,size/2)));assertEquals("No external shadow outside glass: "+theme,0,Color.alpha(image.getPixel(0,0)));}image.recycle();}
        }finally{ThemeSkin.active=previous;}});
    }
}
