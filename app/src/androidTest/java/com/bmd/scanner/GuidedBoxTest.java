package com.bmd.scanner;
import android.content.*;
import android.graphics.*;
import android.os.SystemClock;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class GuidedBoxTest {
    @Test public void expectedCountIsAskedPersistedAndCannotCreateFakeDetections()throws Exception{
        DeviceSupport.context().getSharedPreferences("BoxScanActivity",0).edit().clear().commit();Intent intent=new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl");
        try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(intent)){
            UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());UiObject2 field=device.wait(Until.findObject(By.clazz(EditText.class)),5000);assertNotNull(field);field.setText("0");device.findObject(By.res("android:id/button1")).click();scenario.onActivity(a->assertNull(a.session));field.setText("95");device.findObject(By.res("android:id/button1")).click();SystemClock.sleep(400);scenario.onActivity(a->{assertNotNull(a.session);assertEquals(95,a.session.expected);assertTrue(a.session.tracker.items().isEmpty());});DeviceSupport.shot(null,"build3-box-count-guide");scenario.recreate();scenario.onActivity(a->{assertEquals(95,a.session.expected);assertTrue(a.session.tracker.items().isEmpty());assertFalse(a.session.reviewed);});
        }
    }
    private List<ScanTracker.Detection> markers(){List<ScanTracker.Detection> m=new ArrayList<>();for(int n=0;n<4;n++){float x=.1f+(n%2)*.7f,y=.1f+(n/2)*.7f;m.add(new ScanTracker.Detection("BMD-BOX-"+(char)('A'+n),"QR",x,y,x+.05f,y+.05f));}return m;}
    @Test public void uniqueMarkersAlignButMissingOrDuplicateMarkersCannotClaimAlignment(){List<ScanTracker.Detection> m=markers();BoxRegistration.Result r=BoxRegistration.markers(m,m);assertNotNull(r);assertTrue(r.markers);float[] p=BoxSession.point(r.map,.4f,.6f);assertEquals(.4,p[0],.001);assertEquals(.6,p[1],.001);m.add(m.get(0));assertNull(BoxRegistration.markers(m,markers()));assertNull(BoxRegistration.markers(Collections.emptyList(),markers()));assertFalse(BoxRegistration.valid(new double[]{-1,0,1,0,1,0,0,0,1}));}
    @Test public void naturalFeatureRegistrationRecoversRealCropAndRejectsBlankImage(){Bitmap overview=Bitmap.createBitmap(900,900,Bitmap.Config.ARGB_8888);overview.eraseColor(Color.WHITE);Canvas c=new Canvas(overview);Paint p=new Paint();Random rand=new Random(29);for(int n=0;n<700;n++){p.setColor(Color.rgb(rand.nextInt(255),rand.nextInt(255),rand.nextInt(255)));c.drawRect(rand.nextInt(850),rand.nextInt(850),rand.nextInt(850)+20,rand.nextInt(850)+20,p);}for(int n=0;n<100;n++){p.setColor(Color.BLACK);p.setTextSize(18);c.drawText("BMD "+n,rand.nextInt(800),rand.nextInt(850)+20,p);}Bitmap detail=Bitmap.createBitmap(overview,180,180,600,600),blank=Bitmap.createBitmap(600,600,Bitmap.Config.ARGB_8888);blank.eraseColor(Color.WHITE);try{BoxRegistration.Result r=BoxRegistration.align(detail,overview,Collections.emptyList(),Collections.emptyList());assertNotNull("Textured crop should align",r);float[] centre=BoxSession.point(r.map,.5f,.5f);assertEquals(480f/900,centre[0],.02);assertEquals(480f/900,centre[1],.02);assertNull(BoxRegistration.align(blank,overview,Collections.emptyList(),Collections.emptyList()));}finally{overview.recycle();detail.recycle();blank.recycle();}}
    @Test public void realCartonPhotoNeverFillsTheExpectedNinetyFiveProducts()throws Exception{Bitmap b;try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("carton-95.jpg")){b=BitmapFactory.decodeStream(in);}try{List<ScanTracker.Detection> decoded=NativeCodes.decode(b,"both");BoxSession s=new BoxSession(95);s.add("real",decoded,new double[]{1,0,0,0,1,0,0,0,1});android.util.Log.i("BMD-box","real carton decoded symbols="+decoded.size()+" expected products=95; no synthetic filling");assertEquals(decoded.size(),s.tracker.items().size());assertFalse(s.ready());}finally{b.recycle();}}
    @Test public void guidedCameraActuallyCapturesOverviewAndPackageContainsOriginalAndTruthfulManifest()throws Exception{
        DeviceSupport.permission();DeviceSupport.context().getSharedPreferences("BoxScanActivity",0).edit().clear().commit();
        try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl"))){
            UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());UiObject2 field=device.wait(Until.findObject(By.clazz(EditText.class)),5000);assertNotNull(field);field.setText("95");device.findObject(By.res("android:id/button1")).click();SystemClock.sleep(350);
            scenario.onActivity(a->DeviceSupport.call(a,"cameraPage",new Class<?>[]{}));long end=SystemClock.elapsedRealtime()+40000;java.util.concurrent.atomic.AtomicBoolean ready=new java.util.concurrent.atomic.AtomicBoolean();while(SystemClock.elapsedRealtime()<end){scenario.onActivity(a->ready.set((Boolean)DeviceSupport.get(a,"ready")));if(ready.get())break;SystemClock.sleep(200);}assertTrue("Guided camera must bind",ready.get());SystemClock.sleep(1000);DeviceSupport.shot(null,"build3-box-live-camera");scenario.onActivity(a->DeviceSupport.tag(a.getWindow().getDecorView(),"box-shutter").performClick());
            end=SystemClock.elapsedRealtime()+60000;java.util.concurrent.atomic.AtomicBoolean saved=new java.util.concurrent.atomic.AtomicBoolean();while(SystemClock.elapsedRealtime()<end){scenario.onActivity(a->saved.set(DeviceSupport.get(a,"overview")!=null&&!(Boolean)DeviceSupport.get(a,"busy")));if(saved.get())break;SystemClock.sleep(200);}assertTrue("Overview must finish decoding",saved.get());
            scenario.onActivity(a->{assertEquals(95,a.session.expected);assertEquals(0,a.session.shots.size());assertTrue(a.session.tracker.items().isEmpty());File folder=(File)DeviceSupport.get(a,"folder");assertTrue(new File(folder,"overview.jpg").length()>1000);try{org.json.JSONObject json=(org.json.JSONObject)DeviceSupport.call(a,"json",new Class<?>[]{});File archive=a.buildPackage(json,a.session.products.exportRows(a.session.tracker.items()),folder,false);try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(archive)){assertNotNull(zip.getEntry("photos/overview.jpg"));assertNotNull(zip.getEntry("INCOMPLETE-NOT-SHIPMENT-APPROVAL.txt"));try(InputStream in=zip.getInputStream(zip.getEntry("session.json"))){org.json.JSONObject manifest=new org.json.JSONObject(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));assertEquals(95,manifest.getInt("expected"));assertFalse(manifest.getBoolean("shipmentApproved"));assertFalse(manifest.getBoolean("complete"));}}}catch(Exception e){throw new AssertionError(e);}});DeviceSupport.shot(null,"build3-box-overview-summary");
        }
    }

}

