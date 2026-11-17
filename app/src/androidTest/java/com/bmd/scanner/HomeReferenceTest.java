package com.bmd.scanner;
import android.graphics.*;
import android.os.*;
import android.view.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import java.io.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class HomeReferenceTest {
    @Before public void before(){DeviceSupport.clear();}
    @Test public void displayedDutchHomeMatchesOriginalReferencePixels()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            Thread.sleep(800);CountDownLatch ready=new CountDownLatch(1);Bitmap[] actual=new Bitmap[1];int[] result={-1},bounds=new int[4];
            scenario.onActivity(a->{View home=DeviceSupport.tag(a.getWindow().getDecorView(),"reference-home");assertNotNull(home);int[] xy=new int[2];home.getLocationInWindow(xy);bounds[0]=xy[0];bounds[1]=xy[1];bounds[2]=home.getWidth();bounds[3]=home.getHeight();View decor=a.getWindow().getDecorView();actual[0]=Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),Bitmap.Config.ARGB_8888);PixelCopy.request(a.getWindow(),actual[0],status->{result[0]=status;ready.countDown();},new Handler(Looper.getMainLooper()));});
            assertTrue(ready.await(15,TimeUnit.SECONDS));assertEquals(PixelCopy.SUCCESS,result[0]);Bitmap reference;try(InputStream in=DeviceSupport.context().getAssets().open("home-reference-2-3-2.png")){reference=BitmapFactory.decodeStream(in);}assertNotNull(reference);assertEquals(941,reference.getWidth());assertEquals(1672,reference.getHeight());int w=bounds[2],h=bounds[3];Bitmap expected=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(expected);canvas.drawColor(MainActivity.NAVY);float scale=Math.min(w/879f,h/1458f),width=879*scale,height=1458*scale;if(height<h){canvas.drawBitmap(reference,new Rect(31,106,910,107),new RectF((w-width)/2,0,(w+width)/2,(h-height)/2),new Paint(Paint.FILTER_BITMAP_FLAG));canvas.drawBitmap(reference,new Rect(31,1563,910,1564),new RectF((w-width)/2,(h+height)/2,(w+width)/2,h),new Paint(Paint.FILTER_BITMAP_FLAG));}canvas.drawBitmap(reference,new Rect(31,106,910,1564),new RectF((w-width)/2,(h-height)/2,(w+width)/2,(h+height)/2),new Paint(Paint.FILTER_BITMAP_FLAG));
            long error=0,samples=0;for(int y=2;y<h-2;y+=3)for(int x=2;x<w-2;x+=3){int a=actual[0].getPixel(x+bounds[0],y+bounds[1]),e=expected.getPixel(x,y);error+=Math.abs(Color.red(a)-Color.red(e))+Math.abs(Color.green(a)-Color.green(e))+Math.abs(Color.blue(a)-Color.blue(e));samples+=3;}double mean=error/(double)samples;android.util.Log.i("BMD-reference","Mean RGB error="+mean+" on "+w+"x"+h);assertTrue("Displayed reference mismatch; mean RGB error="+mean,mean<1.1);expected.recycle();reference.recycle();actual[0].recycle();DeviceSupport.shot(scenario,"home-2-3-pixel-verified");
        }
    }
    @Test public void translatedButtonsRemainAccessibleAndOpenTheirRealPages()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            String[] tags={"qr","photo","list","settings"},keys={"scan","photos","results","settings"};
            for(String lang:MainActivity.LANGS){DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("language",lang).commit();scenario.recreate();scenario.onActivity(a->{a.showHome();for(int i=0;i<tags.length;i++){View button=DeviceSupport.tag(a.getWindow().getDecorView(),tags[i]);assertTrue(button.isClickable());assertEquals(a.s(keys[i]),button.getContentDescription().toString());}DeviceSupport.tag(a.getWindow().getDecorView(),"settings").performClick();assertNotNull(DeviceSupport.tag(a.getWindow().getDecorView(),"language"));a.showHome();DeviceSupport.tag(a.getWindow().getDecorView(),"list").performClick();assertEquals("results",DeviceSupport.get(a,"screen"));a.showHome();});if(lang.equals("en")||lang.equals("hi"))DeviceSupport.shot(scenario,"home-2-3-"+lang);}
            scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"photo").performClick());UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());assertTrue(device.wait(Until.hasObject(By.pkg(java.util.regex.Pattern.compile("com\\.(google\\.android|android)\\.documentsui"))),10000));device.pressBack();device.waitForIdle();DeviceSupport.permission();scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"qr").performClick());DeviceSupport.await(scenario,a->Boolean.TRUE.equals(DeviceSupport.get(a,"cameraReady")),35);
        }
    }
}

