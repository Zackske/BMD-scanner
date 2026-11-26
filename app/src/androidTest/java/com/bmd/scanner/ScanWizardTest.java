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

    private static void chooseFormat(ActivityScenario<BoxScanActivity> scenario,String selected)throws Exception{
        scenario.onActivity(a->{DeviceSupport.tag(a.getWindow().getDecorView(),"box-camera-mode").performClick();android.widget.PopupWindow popup=(android.widget.PopupWindow)DeviceSupport.get(a,"formatPopup");assertTrue(popup.isShowing());assertEquals(3,((android.view.ViewGroup)popup.getContentView()).getChildCount());assertFalse(DeviceSupport.hasText(popup.getContentView(),"QR"));assertFalse(DeviceSupport.hasText(popup.getContentView(),"Barcode"));DeviceSupport.tag(popup.getContentView(),"box-format-"+selected).performClick();});cameraReady(scenario);
    }
    private static void recognized(ActivityScenario<BoxScanActivity> scenario,int expected,String kind)throws Exception{
        long end=SystemClock.elapsedRealtime()+45000;AtomicBoolean yes=new AtomicBoolean();while(SystemClock.elapsedRealtime()<end){scenario.onActivity(a->{java.util.List<ScanTracker.Item> rows=((ScanTracker)DeviceSupport.get(a,"live")).visible(SystemClock.elapsedRealtime());yes.set(rows.size()==expected&&(kind==null||rows.stream().allMatch(row->row.kind.equals(kind))));});if(yes.get())return;SystemClock.sleep(200);}DeviceSupport.shot(null,"build3-format-failure");fail("Live format selection must recognize "+expected+" "+kind);
    }

    @Test public void controlChoiceSurvivesRecreationInAllThemes()throws Exception{
        for(String theme:ThemeSkin.IDS){DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("colourTheme",theme).putString("language","nl").commit();
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                scenario.onActivity(a->{a.chooseScan();DeviceSupport.tag(a.getWindow().getDecorView(),"qr").performClick();View root=wizard(a);assertNotNull(DeviceSupport.tag(root,"wizard-step-type"));assertFalse(DeviceSupport.tag(root,"wizard-next").isEnabled());DeviceSupport.tag(root,"wizard-type-box").performClick();assertTrue(DeviceSupport.tag(wizard(a),"wizard-type-box").isSelected());assertTrue(DeviceSupport.hasText(wizard(a),a.s("wizard_box")));});
                scenario.recreate();scenario.onActivity(a->{assertTrue(DeviceSupport.tag(wizard(a),"wizard-type-box").isSelected());assertNotNull(DeviceSupport.tag(wizard(a),"wizard-step-type"));assertTrue(DeviceSupport.tag(wizard(a),"wizard-next").isEnabled());});
            }
        }
    }
    @Test public void palletAndBoxesRemainWorkInProgressWithoutCreatingAPicklist()throws Exception{
        Instrumentation inst=InstrumentationRegistry.getInstrumentation();Instrumentation.ActivityMonitor monitor=inst.addMonitor(PicklistActivity.class.getName(),null,true);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{a.chooseScan();View root=wizard(a);assertNull(DeviceSupport.tag(root,"wizard-type-stack"));assertNotNull(DeviceSupport.tag(root,"wizard-type-pallet"));DeviceSupport.tag(root,"wizard-type-pallet").performClick();android.app.Dialog pending=(android.app.Dialog)DeviceSupport.get(a.scanWizard,"palletPending");assertTrue(pending.isShowing());assertTrue(DeviceSupport.hasText(pending.getWindow().getDecorView(),a.s("in_development")));assertFalse(DeviceSupport.tag(wizard(a),"wizard-next").isEnabled());assertEquals(0,monitor.getHits());pending.dismiss();assertTrue(a.scanWizard.isShowing());});
        }finally{inst.removeMonitor(monitor);}
    }
    @Test public void productChoiceArtworkIsTransparentAndKeepsItsWideAspect(){
        for(int resource:new int[]{R.drawable.wizard_full_box,R.drawable.wizard_stack}){
            android.graphics.Bitmap image=ReferenceArt.bitmap(DeviceSupport.context(),resource);
            assertTrue("Product art needs real alpha",image.hasAlpha());
            assertTrue("Wide carton and stack must not be stretched",image.getWidth()>=image.getHeight()*1.8f);
            assertEquals(0,android.graphics.Color.alpha(image.getPixel(0,0)));
            assertTrue(android.graphics.Color.alpha(image.getPixel(image.getWidth()/2,image.getHeight()/2))>200);
        }
    }

    @Test public void finalCheckOpensTheNewPicklistStage()throws Exception{
        Instrumentation inst=InstrumentationRegistry.getInstrumentation();Instrumentation.ActivityMonitor monitor=inst.addMonitor(PicklistActivity.class.getName(),null,false);Activity opened=null;
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{a.chooseScan();DeviceSupport.tag(a.getWindow().getDecorView(),"qr").performClick();DeviceSupport.tag(wizard(a),"wizard-type-box").performClick();DeviceSupport.tag(wizard(a),"wizard-next").performClick();});
            opened=monitor.waitForActivityWithTimeout(10000);assertNotNull("Final check must open picklist intake before count",opened);
        }finally{if(opened!=null){Activity a=opened;inst.runOnMainSync(a::finish);}inst.removeMonitor(monitor);}
    }
    @Test public void stackGoesStraightToCameraCapturesOnceAndRetakeDoesNotAppendDuplicates()throws Exception{
        DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("codeMode","qr").putInt("sensitivity",2).commit();
        try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl"))){
            DeviceSupport.startBoxWizard(scenario,"stack","3");cameraReady(scenario);scenario.onActivity(a->{assertTrue(a.session.stacked);assertEquals(3,a.session.expected);assertTrue(a.session.tracker.items().isEmpty());View root=a.getWindow().getDecorView();assertNotNull(DeviceSupport.tag(root,"box-camera-viewport"));assertNull(DeviceSupport.tag(root,"box-photo-guide"));assertNull(DeviceSupport.tag(root,"box-instructions"));});
            long end=SystemClock.elapsedRealtime()+45000;AtomicBoolean seesThree=new AtomicBoolean();while(SystemClock.elapsedRealtime()<end){scenario.onActivity(a->seesThree.set(((ScanTracker)DeviceSupport.get(a,"live")).visible(SystemClock.elapsedRealtime()).size()==3));if(seesThree.get())break;SystemClock.sleep(200);}if(!seesThree.get())DeviceSupport.shot(null,"build3-stack-live-failure");assertTrue("Live camera must recognize three separate identical QR codes",seesThree.get());DeviceSupport.shot(null,"build3-stack-live-camera");scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"box-camera-mode").performClick());DeviceSupport.shot(null,"build3-stack-icon-selector");scenario.onActivity(a->((android.widget.PopupWindow)DeviceSupport.get(a,"formatPopup")).dismiss());chooseFormat(scenario,"bar");recognized(scenario,3,"Barcode");DeviceSupport.shot(null,"build3-stack-barcode-mode");chooseFormat(scenario,"both");recognized(scenario,6,null);DeviceSupport.shot(null,"build3-stack-mixed-mode");chooseFormat(scenario,"qr");recognized(scenario,3,"QR");
            scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"box-shutter").performClick());captured(scenario,"");String[] old={null};scenario.onActivity(a->{assertEquals(3,a.session.tracker.items().size());assertEquals(3,a.session.products.view(a.session.tracker.items()).size());assertEquals(1,a.session.shots.size());assertTrue(a.session.hasRequiredPhotos());assertFalse(a.session.ready());assertEquals(3,a.session.tracker.items().stream().filter(row->row.raw.equals("BMD-000123")).count());old[0]=((File)DeviceSupport.get(a,"overviewFile")).getName();DeviceSupport.tag(a.getWindow().getDecorView(),"stack-rescan").performClick();});cameraReady(scenario);long againEnd=SystemClock.elapsedRealtime()+45000;AtomicBoolean againSeen=new AtomicBoolean();while(SystemClock.elapsedRealtime()<againEnd){scenario.onActivity(a->againSeen.set(((ScanTracker)DeviceSupport.get(a,"live")).visible(SystemClock.elapsedRealtime()).size()==3));if(againSeen.get())break;SystemClock.sleep(200);}assertTrue(againSeen.get());
            scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"box-shutter").performClick());captured(scenario,old[0]);scenario.onActivity(a->{assertEquals("Retake must replace, never append",3,a.session.tracker.items().size());assertEquals(1,a.session.shots.size());File folder=(File)DeviceSupport.get(a,"folder"),reference=(File)DeviceSupport.get(a,"overviewFile");assertTrue(new File(folder,old[0]).isFile());try{org.json.JSONObject report=(org.json.JSONObject)DeviceSupport.call(a,"json",new Class<?>[]{});assertEquals("stack",report.getString("flowMode"));assertFalse(report.getBoolean("markersRequired"));assertEquals(reference.getName(),report.getString("referenceImage"));File archive=a.buildPackage(report,a.session.products.exportRows(a.session.tracker.items()),folder,false);try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(archive)){assertNotNull(zip.getEntry("photos/"+reference.getName()));assertEquals(1,zip.stream().filter(e->e.getName().startsWith("photos/")).count());assertNotNull(zip.getEntry("INCOMPLETE-NOT-SHIPMENT-APPROVAL.txt"));}}catch(Exception e){throw new AssertionError(e);}});DeviceSupport.shot(null,"build3-stack-captured-summary");
            scenario.recreate();scenario.onActivity(a->{assertTrue(a.session.stacked);assertEquals(3,a.session.expected);assertEquals(3,a.session.tracker.items().size());assertEquals(1,a.session.shots.size());assertFalse(a.session.reviewed);assertTrue(((File)DeviceSupport.get(a,"overviewFile")).isFile());});
        }
    }
}
