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
    @Test public void homeUsesAvailableWidthAndKeepsContentInsideViewport()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            Thread.sleep(800);scenario.onActivity(a->{
                View home=DeviceSupport.tag(a.getWindow().getDecorView(),"reference-home");assertNotNull(home);
                View scan=DeviceSupport.tag(home,"qr"),photo=DeviceSupport.tag(home,"photo"),brand=DeviceSupport.tag(home,"home-brand");
                float density=a.getResources().getDisplayMetrics().density;
                assertTrue("Tiles should fill portrait width",scan.getLeft()<=20*density);
                assertTrue(photo.getRight()>=home.getWidth()-20*density);
                assertEquals(home.getWidth()/2f,(brand.getLeft()+brand.getRight())/2f,1f);
                for(String tag:new String[]{"qr","photo","list","settings","home-identity","home-brand"}){View v=DeviceSupport.tag(home,tag);assertTrue(v.getLeft()>=0&&v.getTop()>=0&&v.getRight()<=home.getWidth()&&v.getBottom()<=home.getHeight());}
            });
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

