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

@org.junit.FixMethodOrder(org.junit.runners.MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class ScannerSettingsTest {
    private static View settingsRoot(BoxScanActivity a){return a.settingsPopup.content();}
    private static void nativeShot(String name)throws Exception{
        SystemClock.sleep(450);android.util.Log.i("BMD-settings","screencap start "+name);
        DeviceSupport.shell("mkdir -p /data/local/tmp/bmd-screenshots");
        DeviceSupport.shell("screencap -p /data/local/tmp/bmd-screenshots/"+name+".png");
        String bytes=DeviceSupport.shell("stat -c %s /data/local/tmp/bmd-screenshots/"+name+".png").trim();assertTrue("Android screencap must write a real PNG: "+bytes,Long.parseLong(bytes)>0);
        android.util.Log.i("BMD-settings","screencap done "+name);
    }
    private void ready(ActivityScenario<BoxScanActivity> scenario)throws Exception{
        AtomicBoolean ready=new AtomicBoolean();long end=SystemClock.elapsedRealtime()+45000;
        while(SystemClock.elapsedRealtime()<end){scenario.onActivity(a->{ScanTracker live=(ScanTracker)DeviceSupport.get(a,"live");ready.set((Boolean)DeviceSupport.get(a,"ready")&&live.visible(SystemClock.elapsedRealtime()).size()==3);});if(ready.get())return;SystemClock.sleep(200);}throw new AssertionError("Live camera must decode three separate products");
    }
    @Test public void aPopupOpensAndClosesWithoutCamera()throws Exception{
        DeviceSupport.clear();final ScannerSettingsPopup[] popup={null};final android.widget.FrameLayout[] frame={null};final View[] anchor={null};
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{frame[0]=new android.widget.FrameLayout(a);frame[0].setBackgroundColor(0xff001b32);anchor[0]=new View(a);android.widget.FrameLayout.LayoutParams lp=new android.widget.FrameLayout.LayoutParams(88,88,android.view.Gravity.TOP|android.view.Gravity.END);frame[0].addView(anchor[0],lp);a.setContentView(frame[0]);});SystemClock.sleep(350);
            scenario.onActivity(a->popup[0]=new ScannerSettingsPopup(frame[0],anchor[0],key->a.s(key),()->popup[0]=null));SystemClock.sleep(700);
            try{assertNotNull(popup[0]);nativeShot("build43-settings-without-camera");scenario.onActivity(a->DeviceSupport.tag(popup[0].content(),"scanner-settings-close").performClick());AtomicBoolean closed=new AtomicBoolean();long deadline=SystemClock.elapsedRealtime()+4000;while(SystemClock.elapsedRealtime()<deadline){scenario.onActivity(a->closed.set(popup[0]==null));if(closed.get())break;SystemClock.sleep(50);}assertTrue("Animated X must finish closing",closed.get());}finally{scenario.onActivity(a->{if(popup[0]!=null)popup[0].dispose();});}
        }
    }
    @Test public void closeResumesSameLiveCameraAndKeepsProductsInAllSixThemes()throws Exception{
        DeviceSupport.permission();
        for(String theme:ThemeSkin.IDS){android.util.Log.i("BMD-settings","start theme="+theme);
            DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().clear().putString("colourTheme",theme).putString("language","nl").putString("codeMode","qr").putInt("sensitivity",2).commit();
            Intent intent=new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl").putExtra("flowMode","stack").putExtra("expected",3);
            try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(intent)){
                ready(scenario);android.util.Log.i("BMD-settings","camera ready theme="+theme);Object[] original=new Object[4];
                scenario.onActivity(a->{original[0]=a.session;original[1]=DeviceSupport.get(a,"preview");original[2]=DeviceSupport.get(a,"camera");original[3]=DeviceSupport.get(a,"epoch");a.session.tracker.importCode(new ScanTracker.Detection("settings-preservation-fixture","QR",.1f,.1f,.2f,.2f),0);DeviceSupport.tag(a.getWindow().getDecorView(),"box-settings").performClick();});SystemClock.sleep(1200);
                scenario.onActivity(a->{assertNotNull(a.settingsPopup);View root=settingsRoot(a);View panel=DeviceSupport.tag(root,"scanner-settings-panel");assertNotNull(panel);assertEquals(1,panel.getScaleX(),.01);assertEquals(0,panel.getElevation(),0);assertTrue(panel.getWidth()>250*a.getResources().getDisplayMetrics().density);assertTrue(DeviceSupport.hasText(panel,"Scannerinstellingen"));assertNotNull(DeviceSupport.tag(panel,"scanner-settings-resolution"));assertTrue(DeviceSupport.tag(panel,"scanner-settings-sensitivity-2").isSelected());assertNull("Camera stream pauses behind settings",DeviceSupport.get(a,"camera"));});
                android.util.Log.i("BMD-settings","popup measured theme="+theme);nativeShot("build43-scanner-settings-"+theme);android.util.Log.i("BMD-settings","popup screenshot theme="+theme);
                scenario.onActivity(a->{View root=settingsRoot(a);DeviceSupport.tag(root,"scanner-settings-sensitivity-1").performClick();((Switch)DeviceSupport.tag(root,"scanner-settings-vibration")).setChecked(false);DeviceSupport.tag(root,"scanner-settings-close").performClick();});SystemClock.sleep(450);ready(scenario);
                scenario.onActivity(a->{android.util.Log.i("BMD-settings","closed theme="+theme);assertNull(a.settingsPopup);assertNull(DeviceSupport.tag(a.getWindow().getDecorView(),"scanner-settings-overlay"));assertSame(original[0],a.session);assertSame(original[1],DeviceSupport.get(a,"preview"));assertTrue("Settings preserve scan generation",(Integer)DeviceSupport.get(a,"epoch")>=(Integer)original[3]);assertEquals(1,a.session.tracker.items().size());assertEquals(3,a.session.expected);assertEquals(1,a.getSharedPreferences("MainActivity",0).getInt("sensitivity",-1));assertFalse(a.getSharedPreferences("MainActivity",0).getBoolean("vibrateAfterScan",true));DeviceSupport.tag(a.getWindow().getDecorView(),"box-settings").performClick();});SystemClock.sleep(1100);
                scenario.onActivity(a->{View root=settingsRoot(a);assertTrue(DeviceSupport.tag(root,"scanner-settings-sensitivity-1").isSelected());assertFalse(((Switch)DeviceSupport.tag(root,"scanner-settings-vibration")).isChecked());a.getOnBackPressedDispatcher().onBackPressed();});SystemClock.sleep(450);ready(scenario);
                scenario.onActivity(a->{assertNull(a.settingsPopup);assertSame(original[1],DeviceSupport.get(a,"preview"));assertEquals(1,a.session.tracker.items().size());});nativeShot("build43-scanner-resumed-"+theme);
                scenario.onActivity(a->{DeviceSupport.call(a,"settings",new Class<?>[]{});});SystemClock.sleep(220);scenario.onActivity(a->{DeviceSupport.tag(settingsRoot(a),"scanner-settings-close").performClick();});SystemClock.sleep(450);scenario.onActivity(a->{assertNull("A fast X press cannot reopen the popup",a.settingsPopup);assertSame(original[1],DeviceSupport.get(a,"preview"));});
            }
        }
    }
    @Test public void changingResolutionRebindsExistingPreviewWithoutResettingTheSession()throws Exception{
        DeviceSupport.permission();DeviceSupport.clear();DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("codeMode","qr").putInt("sensitivity",2).commit();
        try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl").putExtra("flowMode","stack").putExtra("expected",3))){
            ready(scenario);Object[] original=new Object[2];scenario.onActivity(a->{original[0]=a.session;original[1]=DeviceSupport.get(a,"preview");a.session.tracker.importCode(new ScanTracker.Detection("keep-this-saved-code","QR",.2f,.2f,.3f,.3f),0);DeviceSupport.call(a,"settings",new Class<?>[]{});});SystemClock.sleep(800);
            scenario.onActivity(a->{DeviceSupport.tag(settingsRoot(a),"scanner-settings-resolution").performClick();android.widget.PopupWindow menu=(android.widget.PopupWindow)DeviceSupport.get(a.settingsPopup,"choices");DeviceSupport.tag(menu.getContentView(),"scanner-resolution-0").performClick();DeviceSupport.tag(settingsRoot(a),"scanner-settings-done").performClick();});SystemClock.sleep(500);ready(scenario);
            scenario.onActivity(a->{assertNull(a.settingsPopup);assertSame(original[0],a.session);assertSame(original[1],DeviceSupport.get(a,"preview"));assertEquals(1,a.session.tracker.items().size());assertEquals("keep-this-saved-code",a.session.tracker.items().get(0).value);assertEquals(0,a.getSharedPreferences("MainActivity",0).getInt("cameraResolution",-1));});
        }
    }
}
