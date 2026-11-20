package com.bmd.scanner;

import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

/** Captures the unchanged released app in the real stack route, in each palette. */
@RunWith(AndroidJUnit4.class)
public class StackThemeScreensTest {
    @Test public void captureAllSixRealStackScreens()throws Exception{
        DeviceSupport.permission();
        for(String theme:ThemeSkin.IDS){
            DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().clear().putString("language","nl").putString("colourTheme",theme).putString("codeMode","qr").putInt("sensitivity",2).commit();
            Intent intent=new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl").putExtra("flowMode","stack").putExtra("expected",3);
            try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(intent)){
                long end=SystemClock.elapsedRealtime()+50000;AtomicBoolean ready=new AtomicBoolean();
                while(SystemClock.elapsedRealtime()<end){scenario.onActivity(a->{ScanTracker live=(ScanTracker)DeviceSupport.get(a,"live");ready.set((Boolean)DeviceSupport.get(a,"ready")&&live.visible(SystemClock.elapsedRealtime()).size()==3);});if(ready.get())break;SystemClock.sleep(200);}
                assertTrue("Real camera must detect three separate identical QR codes in "+theme,ready.get());
                scenario.onActivity(a->{assertTrue(a.session.stacked);assertEquals(3,a.session.expected);View root=a.getWindow().getDecorView();assertNull(DeviceSupport.tag(root,"box-photo-guide"));assertNotNull(DeviceSupport.tag(root,"scanner-dock"));assertNull(DeviceSupport.tag(root,"horizontal-zoom"));assertNotNull(a.pinchZoom);assertNull(DeviceSupport.tag(root,"theme-brand"));});
                DeviceSupport.shot(null,"stack-theme-"+theme);
            }
        }
    }
}
