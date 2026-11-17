package com.bmd.scanner;
import android.os.*;
import android.view.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class CameraControlsTest {
    @Before public void before()throws Exception{DeviceSupport.clear();DeviceSupport.permission();}
    @Test public void actualCameraDetectsSixPhysicalCodesIncludingThreeIdenticalQrCodes()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(MainActivity::showScanner);DeviceSupport.await(scenario,a->Boolean.TRUE.equals(DeviceSupport.get(a,"cameraReady")),35);DeviceSupport.await(scenario,a->a.tracker.items().size()>=6,40);Thread.sleep(1600);
            scenario.onActivity(a->{assertEquals(6,a.tracker.items().size());long qr=a.tracker.items().stream().filter(i->i.kind.equals("QR")).count();assertEquals(3,qr);assertEquals(3,a.tracker.items().stream().filter(i->i.value.equals("BMD-000123")).count());});DeviceSupport.shot(scenario,"camera-live-six-codes");
            scenario.onActivity(a->DeviceSupport.call(a,"changeZoom",new Class<?>[]{float.class},1.2f));DeviceSupport.await(scenario,a->Math.abs((Float)DeviceSupport.get(a,"zoomRatio")-1.2f)<.05,10);Thread.sleep(1200);scenario.onActivity(a->assertEquals("Zoom must not recount tracked products",6,a.tracker.items().size()));DeviceSupport.shot(scenario,"camera-vertical-zoom");
            scenario.onActivity(a->DeviceSupport.call(a,"toggleTorch",new Class<?>[]{}));DeviceSupport.await(scenario,a->Boolean.TRUE.equals(DeviceSupport.get(a,"torch")),10);DeviceSupport.shot(scenario,"camera-lamp-on");scenario.onActivity(a->DeviceSupport.call(a,"toggleTorch",new Class<?>[]{}));DeviceSupport.await(scenario,a->!Boolean.TRUE.equals(DeviceSupport.get(a,"torch")),10);
            scenario.onActivity(a->DeviceSupport.call(a,"formatMenu",new Class<?>[]{}));DeviceSupport.shot(scenario,"camera-format-dropdown");scenario.onActivity(a->((android.widget.PopupWindow)DeviceSupport.get(a,"formatPopup")).dismiss());
            scenario.onActivity(a->DeviceSupport.set(a,"scanning",false));scenario.onActivity(a->DeviceSupport.call(a,"resetScan",new Class<?>[]{}));scenario.onActivity(a->{DeviceSupport.set(a,"scanning",false);assertEquals(0,a.tracker.items().size());});
        }
    }
    @Test public void formatFiltersUseRealCameraAndCaptureIsUserControlled()throws Exception{
        for(String mode:new String[]{"qr","bar"}){
            DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("codeMode",mode).putString("scanRows","[]").commit();
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                scenario.onActivity(MainActivity::showScanner);DeviceSupport.await(scenario,a->a.tracker.items().size()>=3,45);scenario.onActivity(a->{assertEquals(3,a.tracker.items().size());assertTrue(a.tracker.items().stream().allMatch(i->i.kind.equals(mode.equals("qr")?"QR":"Barcode")));assertNull(DeviceSupport.get(a,"capturedPhoto"));});DeviceSupport.shot(scenario,"camera-filter-"+mode);
                scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"photo-shutter").performClick());DeviceSupport.await(scenario,a->DeviceSupport.get(a,"capturedPhoto")!=null&&!Boolean.TRUE.equals(DeviceSupport.get(a,"capturing")),25);scenario.onActivity(a->{assertFalse(Boolean.TRUE.equals(DeviceSupport.get(a,"scanning")));assertTrue(((java.io.File)DeviceSupport.get(a,"capturedPhoto")).length()>1000);});DeviceSupport.shot(scenario,"photo-choice-"+mode);
            }
        }
    }
    @Test public void everyRequestedResolutionBindsWithANativeFallbackAndSettingsPersist()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            for(int resolution=0;resolution<CameraResolutionOptions.TARGETS.length;resolution++){final int choice=resolution;scenario.onActivity(a->{DeviceSupport.set(a,"resolutionChoice",choice);a.showScanner();});DeviceSupport.await(scenario,a->Boolean.TRUE.equals(DeviceSupport.get(a,"cameraReady")),25);scenario.onActivity(a->assertFalse(((String)DeviceSupport.get(a,"actualResolution")).isEmpty()));}
            scenario.onActivity(MainActivity::cameraSettings);DeviceSupport.shot(scenario,"camera-settings-no-mode");scenario.onActivity(a->{android.app.Dialog dialog=(android.app.Dialog)DeviceSupport.get(a,"dialog");assertFalse(DeviceSupport.hasText(dialog.getWindow().getDecorView(),"Video"));assertNotNull(DeviceSupport.tag(dialog.getWindow().getDecorView(),"vibration-toggle"));dialog.dismiss();a.showHome();a.showScanner();});DeviceSupport.await(scenario,a->Boolean.TRUE.equals(DeviceSupport.get(a,"cameraReady")),25);
        }
    }
}
