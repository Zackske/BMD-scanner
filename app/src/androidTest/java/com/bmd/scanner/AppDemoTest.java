package com.bmd.scanner;

import android.app.Dialog;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

/** Records interactions with the installed app, including the real Android file picker. */
@RunWith(AndroidJUnit4.class)
@androidx.test.filters.SdkSuppress(minSdkVersion=31)
public class AppDemoTest {
    private static final String PAYLOAD="BMD-SCANTOVENAAR-DEMO-OK";
    private static Object get(MainActivity activity,String name){try{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}catch(Exception e){throw new AssertionError(e);}}
    private static void call(MainActivity activity,String name,Class<?>[] types,Object...args){try{Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(activity,args);}catch(Exception e){throw new AssertionError(e);}}
    private static View tag(View view,String tag){if(tag.equals(view.getTag()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=tag(((ViewGroup)view).getChildAt(i),tag);if(found!=null)return found;}return null;}
    private static View desc(View view,String text){if(text.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=desc(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
    private static RadioButton radio(View view,String text){if(view instanceof RadioButton&&text.contentEquals(((RadioButton)view).getText()))return (RadioButton)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){RadioButton found=radio(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
    private static String shell(String command)throws Exception{
        try(ParcelFileDescriptor.AutoCloseInputStream in=new ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command))){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toString("UTF-8");}
    }
    private static void hold()throws Exception{InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(1800);}
    private static void ready(ActivityScenario<MainActivity> scenario)throws Exception{
        AtomicBoolean ready=new AtomicBoolean();long end=System.currentTimeMillis()+30000;
        while(!ready.get()&&System.currentTimeMillis()<end){Thread.sleep(100);scenario.onActivity(a->ready.set((Boolean)get(a,"cameraReady")));}assertTrue("The live camera preview must stream",ready.get());
    }
    private static void cancelDialog(UiDevice device)throws Exception{UiObject cancel=device.findObject(new UiSelector().resourceId("android:id/button2"));if(cancel.exists())cancel.click();else device.pressBack();}
    private static Uri seedPhoto(Context context)throws Exception{
        ContentValues values=new ContentValues();values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,"ScanTovenaar-demo.png");values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"image/png");values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,android.os.Environment.DIRECTORY_DOWNLOADS);
        Uri uri=context.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);assertNotNull(uri);
        BitMatrix qr=new QRCodeWriter().encode(PAYLOAD,BarcodeFormat.QR_CODE,640,640);int[] pixels=new int[640*640];for(int y=0;y<640;y++)for(int x=0;x<640;x++)pixels[y*640+x]=qr.get(x,y)?Color.BLACK:Color.WHITE;
        Bitmap bitmap=Bitmap.createBitmap(pixels,640,640,Bitmap.Config.ARGB_8888);try(java.io.OutputStream out=context.getContentResolver().openOutputStream(uri)){assertNotNull(out);assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();return uri;
    }
    @Test public void recordInstalledAppFromLauncherThroughCameraPhotoImportAndCsv()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();context.getSharedPreferences("MainActivity",0).edit().putString("language","nl").putString("scanMode","video").putInt("cameraResolution",0).putInt("sensitivity",1).putBoolean("vibrateAfterScan",true).putString("csvFilename","BMD-ScanTovenaar-results.csv").commit();
        shell("pm grant com.bmd.scanner android.permission.CAMERA");Uri seeded=seedPhoto(context);
        UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());device.pressHome();device.swipe(device.getDisplayWidth()/2,device.getDisplayHeight()*9/10,device.getDisplayWidth()/2,device.getDisplayHeight()/5,35);device.waitForIdle();
        shell("mkdir -p /data/local/tmp/bmd-demo");
        ParcelFileDescriptor recording=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("screenrecord --size 720x1280 --bit-rate 1200000 --time-limit 180 /data/local/tmp/bmd-demo/app-demo.mp4");
        try{
            hold();
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                hold();scenario.onActivity(a->tag(a.getWindow().getDecorView(),"settings").performClick());hold();
                scenario.onActivity(a->desc(a.getWindow().getDecorView(),"Taal / Language").performClick());hold();cancelDialog(device);
                scenario.onActivity(a->desc(a.getWindow().getDecorView(),"CSV-instellingen").performClick());hold();cancelDialog(device);
                scenario.onActivity(a->call(a,"showHome",new Class<?>[]{}));hold();scenario.onActivity(a->tag(a.getWindow().getDecorView(),"qr").performClick());ready(scenario);hold();
                scenario.onActivity(a->tag(a.getWindow().getDecorView(),"camera-start").performClick());hold();
                scenario.onActivity(a->call(a,"changeZoom",new Class<?>[]{float.class},1.5f));hold();
                scenario.onActivity(a->{View flash=tag(a.getWindow().getDecorView(),"camera-flash");if(flash.isEnabled())flash.performClick();});hold();
                scenario.onActivity(a->tag(a.getWindow().getDecorView(),"camera-stop").performClick());hold();
                scenario.onActivity(a->tag(a.getWindow().getDecorView(),"camera-settings").performClick());hold();
                scenario.onActivity(a->{Dialog dialog=(Dialog)get(a,"cameraSettingsDialog");desc(dialog.getWindow().getDecorView(),"Resolutie").performClick();});hold();
                UiScrollable list=new UiScrollable(new UiSelector().className("android.widget.ListView"));if(list.exists())list.scrollToEnd(10);hold();device.pressBack();
                scenario.onActivity(a->{Dialog dialog=(Dialog)get(a,"cameraSettingsDialog");View settings=dialog.getWindow().getDecorView();radio(settings,"Foto").performClick();((androidx.appcompat.widget.SwitchCompat)tag(settings,"scan-vibration")).setChecked(true);});hold();
                scenario.onActivity(a->{Dialog dialog=(Dialog)get(a,"cameraSettingsDialog");desc(dialog.getWindow().getDecorView(),"Toepassen").performClick();});ready(scenario);hold();
                scenario.onActivity(a->tag(a.getWindow().getDecorView(),"camera-capture").performClick());
                AtomicBoolean busy=new AtomicBoolean(true);long deadline=System.currentTimeMillis()+45000;while(busy.get()&&System.currentTimeMillis()<deadline){Thread.sleep(100);scenario.onActivity(a->busy.set((Boolean)get(a,"captureBusy")));}assertFalse(busy.get());hold();
                scenario.onActivity(a->call(a,"showHome",new Class<?>[]{}));scenario.onActivity(a->tag(a.getWindow().getDecorView(),"photo").performClick());device.waitForIdle();
                UiObject roots=device.findObject(new UiSelector().descriptionContains("Show roots"));if(roots.exists())roots.click();UiObject downloads=device.findObject(new UiSelector().text("Downloads"));if(downloads.exists())downloads.click();hold();
                UiObject file=device.findObject(new UiSelector().textContains("ScanTovenaar-demo"));if(file.exists())file.click();else{UiObject thumbnail=device.findObject(new UiSelector().resourceIdMatches(".*:id/(icon_thumb|icon_thumbnail|thumbnail)"));assertTrue("Demo image must be selectable",thumbnail.waitForExists(10000));thumbnail.click();}
                AtomicBoolean imported=new AtomicBoolean();deadline=System.currentTimeMillis()+30000;while(!imported.get()&&System.currentTimeMillis()<deadline){Thread.sleep(100);scenario.onActivity(a->imported.set(((List<?>)get(a,"results")).contains(PAYLOAD)));}assertTrue(imported.get());hold();device.pressBack();hold();
                scenario.onActivity(a->tag(a.getWindow().getDecorView(),"csv").performClick());hold();
                UiObject save=device.findObject(new UiSelector().resourceIdMatches(".*:id/button_save"));if(!save.exists())save=device.findObject(new UiSelector().textMatches("(?i)(save|opslaan)"));
                if(save.exists()&&!save.isEnabled()){roots=device.findObject(new UiSelector().descriptionContains("Show roots"));if(roots.exists())roots.click();downloads=device.findObject(new UiSelector().text("Downloads"));if(downloads.exists())downloads.click();}
                assertTrue("Android must offer saving the CSV",save.waitForExists(10000));save.click();
                UiObject replace=device.findObject(new UiSelector().textMatches("(?i)(replace|vervangen)"));if(replace.waitForExists(1000))replace.click();
                UiObject saved=device.findObject(new UiSelector().text("CSV opgeslagen"));assertTrue("CSV must be saved through the Android document picker",saved.waitForExists(10000));hold();
                UiObject send=device.findObject(new UiSelector().resourceId("android:id/button1"));assertTrue(send.exists());send.click();hold();device.pressBack();
                scenario.onActivity(a->call(a,"showHome",new Class<?>[]{}));hold();
            }
        }finally{
            String ids=shell("pidof screenrecord").trim();for(String id:ids.split("\\s+"))if(id.matches("[0-9]+"))shell("kill -2 "+id);
            for(int i=0;i<50&&!shell("pidof screenrecord").trim().isEmpty();i++)Thread.sleep(100);recording.close();context.getContentResolver().delete(seeded,null,null);
        }
    }
}
