package com.bmd.scanner;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@androidx.test.filters.SdkSuppress(minSdkVersion=31)
public class CameraControlsTest {
    private static Object get(MainActivity activity,String name){try{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}catch(Exception e){throw new AssertionError(e);}}
    private static void set(MainActivity activity,String name,Object value){try{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);field.set(activity,value);}catch(Exception e){throw new AssertionError(e);}}
    private static void call(MainActivity activity,String name,Class<?>[] types,Object...args){try{Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(activity,args);}catch(Exception e){throw new AssertionError(e);}}
    private static View tagged(View view,String tag){if(tag.equals(view.getTag()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=tagged(((ViewGroup)view).getChildAt(i),tag);if(found!=null)return found;}return null;}
    private static View description(View view,String text){if(text.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=description(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
    private static RadioButton radio(View view,String text){if(view instanceof RadioButton&&text.contentEquals(((RadioButton)view).getText()))return (RadioButton)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){RadioButton found=radio(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
    private static void prepare()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("MainActivity",0).edit().putString("language","nl").putString("scanMode","video").putInt("cameraResolution",0).putInt("sensitivity",1).putBoolean("vibrateAfterScan",true).commit();
        shell("pm grant com.bmd.scanner android.permission.CAMERA");
    }
    private static void shell(String cmd)throws Exception{try(android.os.ParcelFileDescriptor.AutoCloseInputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(cmd))){while(in.read()!=-1){}}}
    private static void shot(String name)throws Exception{InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(1000);shell("mkdir -p /data/local/tmp/bmd-screenshots");shell("screencap -p /data/local/tmp/bmd-screenshots/"+name+".png");}
    private static void ready(ActivityScenario<MainActivity> scenario)throws Exception{
        AtomicBoolean ready=new AtomicBoolean();long end=System.currentTimeMillis()+30000;
        while(!ready.get()&&System.currentTimeMillis()<end){Thread.sleep(100);scenario.onActivity(activity->ready.set((Boolean)get(activity,"cameraReady")));}
        assertTrue("Emulated camera must bind successfully",ready.get());
    }
    @Test public void videoStartStopAndRoundPhotoCaptureUseTheEmulatedCamera()throws Exception{
        prepare();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(activity->call(activity,"showScanner",new Class<?>[]{}));ready(scenario);
            scenario.onActivity(activity->{View root=activity.getWindow().getDecorView();assertNotNull(tagged(root,"camera-start"));assertNotNull(tagged(root,"camera-stop"));assertNull(tagged(root,"camera-capture"));assertNull(description(root,"Foto’s uploaden"));assertFalse((Boolean)get(activity,"scanning"));tagged(root,"camera-start").performClick();assertTrue((Boolean)get(activity,"scanning"));});
            shot("08-video-scanning");
            scenario.onActivity(activity->{tagged(activity.getWindow().getDecorView(),"camera-stop").performClick();assertFalse((Boolean)get(activity,"scanning"));assertTrue(tagged(activity.getWindow().getDecorView(),"camera-start").isEnabled());
                tagged(activity.getWindow().getDecorView(),"camera-settings").performClick();Dialog dialog=(Dialog)get(activity,"cameraSettingsDialog");View settings=dialog.getWindow().getDecorView();radio(settings,"Foto").performClick();description(settings,"Toepassen").performClick();});
            ready(scenario);
            scenario.onActivity(activity->{View root=activity.getWindow().getDecorView();assertNull(tagged(root,"camera-start"));assertNull(tagged(root,"camera-stop"));assertNotNull(tagged(root,"camera-capture"));});
            shot("09-photo-shutter");
            scenario.onActivity(activity->{View shutter=tagged(activity.getWindow().getDecorView(),"camera-capture");assertEquals(shutter.getWidth(),shutter.getHeight());shutter.performClick();assertTrue((Boolean)get(activity,"captureBusy"));});
            AtomicBoolean busy=new AtomicBoolean(true);long until=System.currentTimeMillis()+45000;
            while(busy.get()&&System.currentTimeMillis()<until){Thread.sleep(100);scenario.onActivity(activity->busy.set((Boolean)get(activity,"captureBusy")));}
            assertFalse("One-shot capture must complete",busy.get());
            scenario.onActivity(activity->{String status=((TextView)get(activity,"resultText")).getText().toString();assertTrue("The emulated scene has no QR, but image capture and decoding must succeed: "+status,status.startsWith("Geen QR-code gevonden"));assertEquals(0,((List<?>)get(activity,"results")).size());assertTrue(tagged(activity.getWindow().getDecorView(),"camera-capture").isEnabled());});
            shot("10-photo-capture-completed");
        }
    }
    @Test public void gearPersistsSettingsAndCameraControlsRespectHardwareLimits()throws Exception{
        prepare();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(activity->call(activity,"showScanner",new Class<?>[]{}));ready(scenario);
            scenario.onActivity(activity->{androidx.camera.core.Camera camera=(androidx.camera.core.Camera)get(activity,"activeCamera");assertEquals(camera.getCameraInfo().hasFlashUnit(),((View)get(activity,"flashButton")).isEnabled());call(activity,"changeZoom",new Class<?>[]{float.class},1000f);});
            Thread.sleep(500);
            scenario.onActivity(activity->{androidx.camera.core.Camera camera=(androidx.camera.core.Camera)get(activity,"activeCamera");androidx.camera.core.ZoomState zoom=camera.getCameraInfo().getZoomState().getValue();assertNotNull(zoom);assertTrue((Float)get(activity,"zoomRatio")<=zoom.getMaxZoomRatio());
                tagged(activity.getWindow().getDecorView(),"camera-settings").performClick();Dialog dialog=(Dialog)get(activity,"cameraSettingsDialog");View settings=dialog.getWindow().getDecorView();radio(settings,"Foto").performClick();assertFalse(radio(settings,"Laag").isEnabled());radio(settings,"Video").performClick();radio(settings,"Laag").performClick();((Spinner)description(settings,"Resolutie")).setSelection(0);((androidx.appcompat.widget.SwitchCompat)tagged(settings,"scan-vibration")).setChecked(false);description(settings,"Toepassen").performClick();
                assertEquals("video",get(activity,"scanMode"));assertEquals(0,get(activity,"resolutionChoice"));assertEquals(2,get(activity,"sensitivity"));assertEquals(false,get(activity,"vibrateAfterScan"));});
            scenario.recreate();ready(scenario);
            scenario.onActivity(activity->{assertEquals(2,get(activity,"sensitivity"));assertEquals(false,get(activity,"vibrateAfterScan"));tagged(activity.getWindow().getDecorView(),"camera-settings").performClick();});
            shot("11-camera-settings-video");
            scenario.onActivity(activity->{Dialog dialog=(Dialog)get(activity,"cameraSettingsDialog");View settings=dialog.getWindow().getDecorView();radio(settings,"Foto").performClick();((androidx.appcompat.widget.SwitchCompat)tagged(settings,"scan-vibration")).setChecked(true);});
            shot("12-camera-settings-photo");
        }
    }
    @Test public void eachAddedCodeGetsFeedbackAndDisabledFeedbackIsSilent()throws Exception{
        prepare();AtomicInteger pulses=new AtomicInteger();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(activity->{ScanFeedback feedback=new ScanFeedback(new Handler(Looper.getMainLooper()),pulses::incrementAndGet);set(activity,"scanFeedback",feedback);call(activity,"appendScanResults",new Class<?>[]{List.class},Arrays.asList("A","B","C"));});
            long end=System.currentTimeMillis()+3000;while(pulses.get()<3&&System.currentTimeMillis()<end)Thread.sleep(20);assertEquals(3,pulses.get());
            scenario.onActivity(activity->{((ScanFeedback)get(activity,"scanFeedback")).setEnabled(false);call(activity,"appendScanResults",new Class<?>[]{List.class},Arrays.asList("D",null));call(activity,"appendScanResults",new Class<?>[]{List.class},Collections.emptyList());});Thread.sleep(400);assertEquals(3,pulses.get());
            scenario.onActivity(activity->assertEquals(4,((List<?>)get(activity,"results")).size()));
        }
    }
}
