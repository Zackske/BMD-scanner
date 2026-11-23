package com.bmd.scanner;
import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
@RunWith(AndroidJUnit4.class)
public class AppDemoTest {
    @Test public void recordActualInstalledAppMenusAndLiveCamera()throws Exception{
        DeviceSupport.clear();DeviceSupport.permission();DeviceSupport.shell("mkdir -p /data/local/tmp/bmd-demo");ParcelFileDescriptor recording=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("screenrecord --size 720x1280 --bit-rate 1800000 --time-limit 85 /data/local/tmp/bmd-demo/app-demo.mp4");
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            Thread.sleep(1800);DeviceSupport.shot(scenario,"01-home-3d");scenario.onActivity(MainActivity::showSettings);Thread.sleep(1000);scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"language").performClick());Thread.sleep(1600);scenario.onActivity(a->((android.app.Dialog)DeviceSupport.get(a,"dialog")).dismiss());scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"document-settings").performClick());Thread.sleep(1400);scenario.onActivity(a->((android.app.Dialog)DeviceSupport.get(a,"dialog")).dismiss());scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"about").performClick());Thread.sleep(1800);scenario.onActivity(a->((android.app.Dialog)DeviceSupport.get(a,"dialog")).dismiss());scenario.onActivity(MainActivity::showScanner);DeviceSupport.await(scenario,a->a.tracker.items().size()>=6,40);Thread.sleep(1200);DeviceSupport.shot(scenario,"02-live-camera");scenario.onActivity(a->DeviceSupport.call(a,"formatMenu",new Class<?>[]{}));Thread.sleep(1300);scenario.onActivity(a->((android.widget.PopupWindow)DeviceSupport.get(a,"formatPopup")).dismiss());scenario.onActivity(a->DeviceSupport.call(a,"toggleTorch",new Class<?>[]{}));Thread.sleep(1000);scenario.onActivity(a->DeviceSupport.call(a,"changeZoom",new Class<?>[]{float.class},1.2f));Thread.sleep(1200);scenario.onActivity(MainActivity::showCodeList);Thread.sleep(1800);scenario.onActivity(a->((android.app.Dialog)DeviceSupport.get(a,"dialog")).dismiss());scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"photo-shutter").performClick());DeviceSupport.await(scenario,a->DeviceSupport.get(a,"capturedPhoto")!=null&&!Boolean.TRUE.equals(DeviceSupport.get(a,"capturing")),25);Thread.sleep(1600);DeviceSupport.shot(scenario,"03-photo-actions");
        }finally{DeviceSupport.shell("pkill -2 screenrecord");recording.close();Thread.sleep(900);}
    }
}
