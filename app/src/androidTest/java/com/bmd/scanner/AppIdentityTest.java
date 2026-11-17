package com.bmd.scanner;
import android.content.res.Configuration;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class AppIdentityTest {
    @Before public void before(){DeviceSupport.clear();}
    @Test public void allTenLanguagesTranslateEveryMenuAndKeepCodesUnchanged()throws Exception{
        String[] keys={"scan","photos","results","settings","language","scanner_settings","document_type","about","capture","local_save","share","reset","sensitivity","vibrate","both","qr_only","bar_only","saved","edit"};Set<String> translatedTitles=new HashSet<>();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            for(String lang:MainActivity.LANGS){DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("language",lang).commit();scenario.recreate();scenario.onActivity(a->{translatedTitles.add(a.s("settings"));for(String key:keys){assertNotEquals(key,a.s(key));assertFalse(a.s(key).isEmpty());}a.tracker.reset();a.tracker.importCode(new ScanTracker.Detection("00123 SAME हिन्दी","QR",.1f,.1f,.2f,.2f),0);assertEquals("00123 SAME हिन्दी",a.tracker.items().get(0).value);a.showSettings();View root=a.getWindow().getDecorView();assertNotNull(DeviceSupport.tag(root,"language"));assertNotNull(DeviceSupport.tag(root,"document-settings"));assertNull(DeviceSupport.tag(root,"share-settings"));});DeviceSupport.shot(scenario,"language-"+lang);}
            assertEquals(10,translatedTitles.size());
            scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"language").performClick());DeviceSupport.shot(scenario,"language-picker-ten-flags");scenario.onActivity(a->((android.app.Dialog)DeviceSupport.get(a,"dialog")).dismiss());
            scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"about").performClick());DeviceSupport.shot(scenario,"about-jack-heart");
        }
    }
}
