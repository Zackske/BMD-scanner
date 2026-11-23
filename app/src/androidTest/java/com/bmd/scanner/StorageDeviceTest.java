package com.bmd.scanner;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class StorageDeviceTest {
    @Test public void userChosenFolderReceivesPhotoAndDocumentAndBothCanBeShared()throws Exception{
        DeviceSupport.clear();DeviceSupport.shell("mkdir -p /sdcard/Documents/BMD-Test");
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            File dir=new File(DeviceSupport.context().getCacheDir(),"exports");dir.mkdirs();File photo=new File(dir,"BMD-scan.jpg"),doc=new File(dir,"folder-export.csv");Bitmap bitmap=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.CYAN);try(OutputStream out=new FileOutputStream(photo)){bitmap.compress(Bitmap.CompressFormat.JPEG,90,out);}bitmap.recycle();ScanTracker tracker=new ScanTracker();tracker.importCode(new ScanTracker.Detection("BMD-SAVED","QR",0,0,1,1),0);try(OutputStream out=new FileOutputStream(doc)){out.write(DocumentData.csv(tracker.items(),new String[]{"Nr.","Type","Code"}));}
            scenario.onActivity(a->{DeviceSupport.set(a,"preparedDocument",doc);DeviceSupport.set(a,"exportPhoto",true);Intent share=a.buildShareIntent(doc,photo);assertEquals(Intent.ACTION_SEND_MULTIPLE,share.getAction());assertEquals(2,share.getClipData().getItemCount());ArrayList<Uri> streams=share.getParcelableArrayListExtra(Intent.EXTRA_STREAM);assertEquals(2,streams.size());for(Uri uri:streams)try(InputStream in=a.getContentResolver().openInputStream(uri)){assertNotNull(in);assertTrue(in.read()!=-1);}catch(IOException e){throw new AssertionError(e);}((ActivityResultLauncher<Uri>)DeviceSupport.get(a,"saveFolder")).launch(Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2FBMD-Test"));});
            UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());UiObject use=device.findObject(new UiSelector().textMatches("(?i)use this folder"));assertTrue("Android folder chooser must be shown",use.waitForExists(10000));use.click();UiObject allow=device.findObject(new UiSelector().textMatches("(?i)allow"));if(allow.waitForExists(3000))allow.click();
            long end=android.os.SystemClock.elapsedRealtime()+15000;String files="";while(android.os.SystemClock.elapsedRealtime()<end){files=DeviceSupport.shell("ls /sdcard/Documents/BMD-Test");if(files.contains("folder-export")&&files.contains("BMD-scan-"))break;Thread.sleep(250);}assertTrue(files,files.contains("folder-export"));assertTrue(files,files.contains("BMD-scan-"));DeviceSupport.shot(scenario,"local-photo-document-saved");
        }
    }
}
