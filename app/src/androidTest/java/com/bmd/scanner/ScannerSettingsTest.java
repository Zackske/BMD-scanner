package com.bmd.scanner;

import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ScannerSettingsTest {
    private void ready(ActivityScenario<BoxScanActivity> scenario)throws Exception{
        AtomicBoolean ready=new AtomicBoolean();long end=SystemClock.elapsedRealtime()+45000;
        while(SystemClock.elapsedRealtime()<end){scenario.onActivity(a->{ScanTracker live=(ScanTracker)DeviceSupport.get(a,"live");ready.set((Boolean)DeviceSupport.get(a,"ready")&&live.visible(SystemClock.elapsedRealtime()).size()==3);});if(ready.get())return;SystemClock.sleep(200);}throw new AssertionError("Live camera must decode three separate products");
    }
    @Test public void closeResumesSameLiveCameraAndKeepsProductsInAllSixThemes()throws Exception{
        DeviceSupport.permission();
        for(String theme:ThemeSkin.IDS){
            DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().clear().putString("colourTheme",theme).putString("language","nl").putString("codeMode","qr").putInt("sensitivity",2).commit();
            Intent intent=new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl").putExtra("flowMode","stack").putExtra("expected",3);
            try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(intent)){
                ready(scenario);Object[] original=new Object[4];
                scenario.onActivity(a->{original[0]=a.session;original[1]=DeviceSupport.get(a,"preview");original[2]=DeviceSupport.get(a,"camera");original[3]=DeviceSupport.get(a,"epoch");a.session.tracker.importCode(new ScanTracker.Detection("settings-preservation-fixture","QR",.1f,.1f,.2f,.2f),0);DeviceSupport.tag(a.getWindow().getDecorView(),"box-settings").performClick();});SystemClock.sleep(900);
                scenario.onActivity(a->{View root=a.getWindow().getDecorView();assertNotNull(a.settingsPopup);View panel=DeviceSupport.tag(root,"scanner-settings-panel");assertNotNull(panel);assertEquals(1,panel.getScaleX(),.01);assertEquals(0,panel.getElevation(),0);assertTrue(panel.getWidth()>250*a.getResources().getDisplayMetrics().density);assertTrue(DeviceSupport.hasText(panel,"Scannerinstellingen"));assertNotNull(DeviceSupport.tag(panel,"scanner-settings-resolution"));assertTrue(DeviceSupport.tag(panel,"scanner-settings-sensitivity-2").isSelected());assertSame(original[2],DeviceSupport.get(a,"camera"));});
                DeviceSupport.shot(null,"build43-scanner-settings-"+theme);
                scenario.onActivity(a->{View root=a.getWindow().getDecorView();DeviceSupport.tag(root,"scanner-settings-sensitivity-1").performClick();((Switch)DeviceSupport.tag(root,"scanner-settings-vibration")).setChecked(false);DeviceSupport.tag(root,"scanner-settings-close").performClick();});SystemClock.sleep(450);
                scenario.onActivity(a->{assertNull(a.settingsPopup);assertNull(DeviceSupport.tag(a.getWindow().getDecorView(),"scanner-settings-overlay"));assertSame(original[0],a.session);assertSame(original[1],DeviceSupport.get(a,"preview"));assertSame("Closing settings cannot rebind the camera",original[2],DeviceSupport.get(a,"camera"));assertEquals(original[3],DeviceSupport.get(a,"epoch"));assertEquals(1,a.session.tracker.items().size());assertEquals(3,a.session.expected);assertEquals(1,a.getSharedPreferences("MainActivity",0).getInt("sensitivity",-1));assertFalse(a.getSharedPreferences("MainActivity",0).getBoolean("vibrateAfterScan",true));DeviceSupport.tag(a.getWindow().getDecorView(),"box-settings").performClick();});SystemClock.sleep(800);
                scenario.onActivity(a->{View root=a.getWindow().getDecorView();assertTrue(DeviceSupport.tag(root,"scanner-settings-sensitivity-1").isSelected());assertFalse(((Switch)DeviceSupport.tag(root,"scanner-settings-vibration")).isChecked());a.getOnBackPressedDispatcher().onBackPressed();});SystemClock.sleep(450);ready(scenario);
                scenario.onActivity(a->{assertNull(a.settingsPopup);assertSame(original[1],DeviceSupport.get(a,"preview"));assertEquals(1,a.session.tracker.items().size());});DeviceSupport.shot(null,"build43-scanner-resumed-"+theme);
            }
        }
    }
    @Test public void changingResolutionRebindsExistingPreviewWithoutResettingTheSession()throws Exception{
        DeviceSupport.permission();DeviceSupport.clear();DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("codeMode","qr").putInt("sensitivity",2).commit();
        try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl").putExtra("flowMode","stack").putExtra("expected",3))){
            ready(scenario);Object[] original=new Object[2];scenario.onActivity(a->{original[0]=a.session;original[1]=DeviceSupport.get(a,"preview");a.session.tracker.importCode(new ScanTracker.Detection("keep-this-saved-code","QR",.2f,.2f,.3f,.3f),0);DeviceSupport.call(a,"settings",new Class<?>[]{});});SystemClock.sleep(450);
            scenario.onActivity(a->{DeviceSupport.tag(a.getWindow().getDecorView(),"scanner-settings-resolution").performClick();android.widget.PopupWindow menu=(android.widget.PopupWindow)DeviceSupport.get(a.settingsPopup,"choices");DeviceSupport.tag(menu.getContentView(),"scanner-resolution-0").performClick();DeviceSupport.tag(a.getWindow().getDecorView(),"scanner-settings-done").performClick();});SystemClock.sleep(500);ready(scenario);
            scenario.onActivity(a->{assertNull(a.settingsPopup);assertSame(original[0],a.session);assertSame(original[1],DeviceSupport.get(a,"preview"));assertEquals(1,a.session.tracker.items().size());assertEquals("keep-this-saved-code",a.session.tracker.items().get(0).value);assertEquals(0,a.getSharedPreferences("MainActivity",0).getInt("cameraResolution",-1));});
        }
    }
}
