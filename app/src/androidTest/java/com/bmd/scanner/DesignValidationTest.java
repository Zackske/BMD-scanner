package com.bmd.scanner;
import android.view.*;
import android.widget.Spinner;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DesignValidationTest {
    @Before public void before()throws Exception{DeviceSupport.clear();DeviceSupport.permission();}
    @Test public void nativeHomeLabelsAndInstalledNameWorkInEveryLanguage()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            for(String lang:MainActivity.LANGS){
                DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("language",lang).commit();scenario.recreate();
                scenario.onActivity(a->{a.showHome();assertEquals("BMD Scan",a.getApplicationInfo().loadLabel(a.getPackageManager()).toString());for(String key:new String[]{"camera_mode","save_scan"}){assertNotEquals(key,a.s(key));assertFalse(a.s(key).isEmpty());}View home=DeviceSupport.tag(a.getWindow().getDecorView(),"reference-home");assertNotNull(home);for(String tag:new String[]{"qr","photo","list","settings"})assertTrue(DeviceSupport.tag(home,tag).isClickable());});
                if(lang.equals("nl")||lang.equals("it"))DeviceSupport.shot(scenario,"build3-home-"+lang);
            }
        }
    }
    @Test public void scannerModesSettingsAndRaisedSaveControlAreReal()throws Exception{
        DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("language","en").putString("codeMode","qr").commit();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(MainActivity::showScanner);DeviceSupport.await(scenario,a->Boolean.TRUE.equals(DeviceSupport.get(a,"cameraReady")),35);
            scenario.onActivity(a->{View root=a.getWindow().getDecorView();assertNull(DeviceSupport.tag(root,"gallery-scan"));assertNull(DeviceSupport.tag(root,"list-button"));assertNull(DeviceSupport.tag(root,"reset-button"));assertNull(DeviceSupport.tag(root,"code-filter"));assertEquals("Save scan",DeviceSupport.tag(root,"photo-shutter").getContentDescription().toString());
                View mode=DeviceSupport.tag(root,"camera-mode");assertNotNull(mode);assertEquals("qr",DeviceSupport.get(a,"codeMode"));mode.performClick();assertEquals("bar",DeviceSupport.get(a,"codeMode"));mode.performClick();assertEquals("both",DeviceSupport.get(a,"codeMode"));mode.performClick();assertEquals("qr",DeviceSupport.get(a,"codeMode"));
                View viewport=DeviceSupport.tag(root,"camera-viewport"),zoom=DeviceSupport.tag(root,"vertical-zoom"),counter=DeviceSupport.tag(root,"scan-counter");assertEquals(viewport.getHeight()/2f,(zoom.getTop()+zoom.getBottom())/2f,2f);assertEquals(viewport.getWidth()/2f,(counter.getLeft()+counter.getRight())/2f,2f);
            });DeviceSupport.shot(scenario,"build3-scanner-en");
            scenario.onActivity(MainActivity::cameraSettings);scenario.onActivity(a->{android.app.Dialog d=(android.app.Dialog)DeviceSupport.get(a,"dialog");Spinner format=(Spinner)DeviceSupport.tag(d.getWindow().getDecorView(),"scan-format-choice");assertNotNull(format);assertEquals(3,format.getCount());assertEquals("2D (QR / Data Matrix)",format.getItemAtPosition(0));assertEquals("Barcode",format.getItemAtPosition(1));assertEquals("2D + barcode",format.getItemAtPosition(2));});DeviceSupport.shot(scenario,"build3-scanner-settings");scenario.onActivity(a->((android.app.Dialog)DeviceSupport.get(a,"dialog")).dismiss());
        }
    }
}

