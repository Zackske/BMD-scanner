package com.bmd.scanner;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ScanWizardTest {
    @Before public void before()throws Exception{DeviceSupport.clear();DeviceSupport.permission();DeviceSupport.context().getSharedPreferences("BoxScanActivity",0).edit().clear().commit();}
    private static View wizard(MainActivity a){assertNotNull(a.scanWizard);return a.scanWizard.dialog().getWindow().getDecorView();}
    private static void cameraReady(ActivityScenario<BoxScanActivity> scenario)throws Exception{long end=SystemClock.elapsedRealtime()+40000;AtomicBoolean yes=new AtomicBoolean();while(SystemClock.elapsedRealtime()<end){scenario.onActivity(a->yes.set((Boolean)DeviceSupport.get(a,"ready")));if(yes.get())return;SystemClock.sleep(150);}fail("Camera must bind");}
    private static void captured(ActivityScenario<BoxScanActivity> scenario,String previous)throws Exception{long end=SystemClock.elapsedRealtime()+90000;AtomicBoolean yes=new AtomicBoolean();while(SystemClock.elapsedRealtime()<end){scenario.onActivity(a->{File photo=(File)DeviceSupport.get(a,"overviewFile");yes.set(!(Boolean)DeviceSupport.get(a,"busy")&&a.session.stacked&&a.session.shots.size()==1&&DeviceSupport.get(a,"overview")!=null&&photo!=null&&!photo.getName().equals(previous));});if(yes.get())return;SystemClock.sleep(200);}fail("Stack capture must finish");}

    @Test public void typeAndCountAreSharedAndSurviveBackAndRecreationInAllThemes()throws Exception{
        for(String theme:ThemeSkin.IDS){DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("colourTheme",theme).putString("language","nl").commit();
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                scenario.onActivity(a->{a.chooseScan();DeviceSupport.tag(a.getWindow().getDecorView(),"qr").performClick();View root=wizard(a);assertNotNull(DeviceSupport.tag(root,"wizard-step-type"));assertNull(DeviceSupport.tag(root,"box-expected-input"));assertFalse(DeviceSupport.tag(root,"wizard-next").isEnabled());DeviceSupport.tag(root,"wizard-type-box").performClick();});DeviceSupport.shot(scenario,"build3-wizard-type-"+theme);
                scenario.onActivity(a->{DeviceSupport.tag(wizard(a),"wizard-next").performClick();View root=wizard(a);assertNotNull(DeviceSupport.tag(root,"wizard-step-count"));((EditText)DeviceSupport.tag(root,"box-expected-input")).setText("95");});DeviceSupport.shot(scenario,"build3-wizard-count-"+theme);
                scenario.recreate();scenario.onActivity(a->{View root=wizard(a);assertEquals("95",((EditText)DeviceSupport.tag(root,"box-expected-input")).getText().toString());DeviceSupport.tag(root,"wizard-back").performClick();root=wizard(a);assertTrue(DeviceSupport.tag(root,"wizard-type-box").isSelected());DeviceSupport.tag(root,"wizard-type-stack").performClick();DeviceSupport.tag(wizard(a),"wizard-next").performClick();root=wizard(a);assertEquals("95",((EditText)DeviceSupport.tag(root,"box-expected-input")).getText().toString());assertTrue(DeviceSupport.hasText(root,a.s("wizard_stack")));assertNotNull(DeviceSupport.tag(root,"wizard-step-count"));});
            }
        }
    }
    @Test public void menuCompletionStartsTheChosenBoxRouteWithTheEnteredCount()throws Exception{
        Instrumentation inst=InstrumentationRegistry.getInstrumentation();Instrumentation.ActivityMonitor monitor=inst.addMonitor(BoxScanActivity.class.getName(),null,false);Activity launched=null;
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{a.chooseScan();DeviceSupport.tag(a.getWindow().getDecorView(),"qr").performClick();DeviceSupport.tag(wizard(a),"wizard-type-box").performClick();DeviceSupport.tag(wizard(a),"wizard-next").performClick();((EditText)DeviceSupport.tag(wizard(a),"box-expected-input")).setText("95");DeviceSupport.tag(wizard(a),"wizard-next").performClick();});
            launched=monitor.waitForActivityWithTimeout(10000);assertNotNull("Wizard must launch the scan route",launched);assertEquals(95,launched.getIntent().getIntExtra("expected",0));assertEquals("box",launched.getIntent().getStringExtra("flowMode"));Activity box=launched;inst.runOnMainSync(()->{assertNotNull(DeviceSupport.tag(box.getWindow().getDecorView(),"box-instructions"));assertFalse(((BoxScanActivity)box).session.stacked);assertTrue(((BoxScanActivity)box).session.tracker.items().isEmpty());});
        }finally{if(launched!=null){Activity close=launched;inst.runOnMainSync(close::finish);}inst.removeMonitor(monitor);}
    }
    @Test public void stackGoesStraightToCameraCapturesOnceAndRetakeDoesNotAppendDuplicates()throws Exception{
        DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("codeMode","qr").putInt("sensitivity",2).commit();
        try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl"))){
            DeviceSupport.startBoxWizard(scenario,"stack","3");cameraReady(scenario);scenario.onActivity(a->{assertTrue(a.session.stacked);assertEquals(3,a.session.expected);assertTrue(a.session.tracker.items().isEmpty());View root=a.getWindow().getDecorView();assertNotNull(DeviceSupport.tag(root,"box-camera-viewport"));assertNull(DeviceSupport.tag(root,"box-photo-guide"));assertNull(DeviceSupport.tag(root,"box-instructions"));});
            long end=SystemClock.elapsedRealtime()+45000;AtomicBoolean seesThree=new AtomicBoolean();while(SystemClock.elapsedRealtime()<end){scenario.onActivity(a->seesThree.set(((ScanTracker)DeviceSupport.get(a,"live")).visible(SystemClock.elapsedRealtime()).size()==3));if(seesThree.get())break;SystemClock.sleep(200);}assertTrue("Live camera must recognize three separate identical QR codes",seesThree.get());DeviceSupport.shot(null,"build3-stack-live-camera");
            scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"box-shutter").performClick());captured(scenario,"");String[] old={null};scenario.onActivity(a->{assertEquals(3,a.session.tracker.items().size());assertEquals(3,a.session.products.view(a.session.tracker.items()).size());assertEquals(1,a.session.shots.size());assertTrue(a.session.hasRequiredPhotos());assertFalse(a.session.ready());assertEquals(3,a.session.tracker.items().stream().filter(row->row.raw.equals("BMD-000123")).count());old[0]=((File)DeviceSupport.get(a,"overviewFile")).getName();DeviceSupport.tag(a.getWindow().getDecorView(),"stack-rescan").performClick();});cameraReady(scenario);long againEnd=SystemClock.elapsedRealtime()+45000;AtomicBoolean againSeen=new AtomicBoolean();while(SystemClock.elapsedRealtime()<againEnd){scenario.onActivity(a->againSeen.set(((ScanTracker)DeviceSupport.get(a,"live")).visible(SystemClock.elapsedRealtime()).size()==3));if(againSeen.get())break;SystemClock.sleep(200);}assertTrue(againSeen.get());
            scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"box-shutter").performClick());captured(scenario,old[0]);scenario.onActivity(a->{assertEquals("Retake must replace, never append",3,a.session.tracker.items().size());assertEquals(1,a.session.shots.size());File folder=(File)DeviceSupport.get(a,"folder"),reference=(File)DeviceSupport.get(a,"overviewFile");assertTrue(new File(folder,old[0]).isFile());try{org.json.JSONObject report=(org.json.JSONObject)DeviceSupport.call(a,"json",new Class<?>[]{});assertEquals("stack",report.getString("flowMode"));assertFalse(report.getBoolean("markersRequired"));assertEquals(reference.getName(),report.getString("referenceImage"));File archive=a.buildPackage(report,a.session.products.exportRows(a.session.tracker.items()),folder,false);try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(archive)){assertNotNull(zip.getEntry("photos/"+reference.getName()));assertEquals(1,zip.stream().filter(e->e.getName().startsWith("photos/")).count());assertNotNull(zip.getEntry("INCOMPLETE-NOT-SHIPMENT-APPROVAL.txt"));}}catch(Exception e){throw new AssertionError(e);}});DeviceSupport.shot(null,"build3-stack-captured-summary");
            scenario.recreate();scenario.onActivity(a->{assertTrue(a.session.stacked);assertEquals(3,a.session.expected);assertEquals(3,a.session.tracker.items().size());assertEquals(1,a.session.shots.size());assertFalse(a.session.reviewed);assertTrue(((File)DeviceSupport.get(a,"overviewFile")).isFile());});
        }
    }
}
