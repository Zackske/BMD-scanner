package com.bmd.scanner;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@androidx.test.filters.SdkSuppress(minSdkVersion=31)
@RunWith(AndroidJUnit4.class)
public class ScannerDeviceTest {
    private static View tagged(View view,String tag){
        if(tag.equals(view.getTag()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=tagged(((ViewGroup)view).getChildAt(i),tag);if(found!=null)return found;}
        return null;
    }
    private static boolean hasText(View view,String text){
        if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return true;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasText(((ViewGroup)view).getChildAt(i),text))return true;
        return false;
    }
    private static Object field(MainActivity activity,String name){try{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}catch(Exception error){throw new AssertionError(error);}}
    private static void call(MainActivity activity,String name,Class<?>[] types,Object...args){try{Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(activity,args);}catch(Exception error){throw new AssertionError(error);}}
    private static String shell(String command)throws Exception{
        try(android.os.ParcelFileDescriptor.AutoCloseInputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command))){
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
            while((count=in.read(buffer))!=-1)out.write(buffer,0,count);return out.toString("UTF-8");
        }
    }
    private static void screenshot(String name)throws Exception{
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        shell("mkdir -p /data/local/tmp/bmd-screenshots");
        shell("screencap -p /data/local/tmp/bmd-screenshots/"+name+".png");
    }
    @Test public void homeShowsOnlyFourActionsAndSettingsOpen()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(activity->{View root=activity.getWindow().getDecorView();for(String name:Arrays.asList("qr","photo","list","settings"))assertNotNull(tagged(root,name));assertNull(tagged(root,"csv"));assertFalse(hasText(root,"Verzenden"));});
            screenshot("01-home");
            scenario.onActivity(activity->tagged(activity.getWindow().getDecorView(),"settings").performClick());screenshot("03-settings");
            scenario.onActivity(activity->{assertEquals("settings",field(activity,"screen"));activity.getOnBackPressedDispatcher().onBackPressed();assertEquals("home",field(activity,"screen"));});
            scenario.onActivity(activity->tagged(activity.getWindow().getDecorView(),"list").performClick());
            scenario.onActivity(activity->{assertNotNull(tagged(activity.getWindow().getDecorView(),"csv"));assertFalse(hasText(activity.getWindow().getDecorView(),"Verzenden"));});
        }
    }
    private static Bitmap qr(String value)throws Exception{
        BitMatrix matrix=new QRCodeWriter().encode(value,BarcodeFormat.QR_CODE,480,480,Collections.singletonMap(EncodeHintType.CHARACTER_SET,"UTF-8"));
        int[] pixels=new int[480*480];for(int y=0;y<480;y++)for(int x=0;x<480;x++)pixels[y*480+x]=matrix.get(x,y)?Color.BLACK:Color.WHITE;
        return Bitmap.createBitmap(pixels,480,480,Bitmap.Config.ARGB_8888);
    }
    private static Uri photo(Bitmap image,String name)throws Exception{
        File file=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),name);
        try(FileOutputStream stream=new FileOutputStream(file)){image.compress(Bitmap.CompressFormat.PNG,100,stream);}return Uri.fromFile(file);
    }
    @Test public void photoBatchDecodesMultipleCodesPreservesStateAndSavesCsv()throws Exception{
        String first="SCN-000123456789",second="SCN-000123456790",third="BMD,\"test\"";
        Bitmap pair=Bitmap.createBitmap(1100,600,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(pair);canvas.drawColor(Color.WHITE);canvas.drawBitmap(qr(first),30,60,null);canvas.drawBitmap(qr(second),590,60,null);
        ArrayDeque<Uri> queue=new ArrayDeque<>();queue.add(photo(pair,"pair.png"));queue.add(photo(qr(third),"single.png"));
        Bitmap blank=Bitmap.createBitmap(480,480,Bitmap.Config.ARGB_8888);blank.eraseColor(Color.WHITE);queue.add(photo(blank,"blank.png"));
        File invalid=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"broken.png");try(FileOutputStream out=new FileOutputStream(invalid)){out.write(new byte[]{1,2,3});}queue.add(Uri.fromFile(invalid));
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(activity->call(activity,"scanPhotos",new Class<?>[]{ArrayDeque.class,int.class,int.class},queue,0,0));
            AtomicBoolean busy=new AtomicBoolean(true);long end=System.currentTimeMillis()+60000;
            while(busy.get()&&System.currentTimeMillis()<end){Thread.sleep(100);scenario.onActivity(activity->busy.set((Boolean)field(activity,"photoBusy")));}assertFalse("Photo import timed out",busy.get());
            scenario.onActivity(activity->{List<?> values=(List<?>)field(activity,"results");assertEquals(3,values.size());assertTrue(values.containsAll(Arrays.asList(first,second,third)));});
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);scenario.recreate();screenshot("02-results");
            File exported=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"export.csv");
            scenario.onActivity(activity->{assertEquals(3,((List<?>)field(activity,"results")).size());call(activity,"writeCsv",new Class<?>[]{Uri.class},Uri.fromFile(exported));});
            long timeout=System.currentTimeMillis()+10000;while((!exported.exists()||exported.length()<3)&&System.currentTimeMillis()<timeout)Thread.sleep(100);
            scenario.onActivity(activity->{@SuppressWarnings("unchecked") List<String> values=(List<String>)field(activity,"results");try{assertArrayEquals(CsvData.encode(values),Files.readAllBytes(exported.toPath()));}catch(Exception error){throw new AssertionError(error);}});
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();screenshot("04-csv-saved");
        }
    }
    @Test public void photoButtonAndAndroidPickerImportRealImage()throws Exception{
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"BMD-virtual-photo.png");
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png");
        values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,android.os.Environment.DIRECTORY_DOWNLOADS);
        values.put(android.provider.MediaStore.Images.Media.IS_PENDING,1);
        Uri media=context.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        assertNotNull(media);
        try{
            try(java.io.OutputStream output=context.getContentResolver().openOutputStream(media)){
                assertNotNull(output);assertTrue(qr("BMD-PHOTO-PICKER-OK").compress(Bitmap.CompressFormat.PNG,100,output));
            }
            values.clear();values.put(android.provider.MediaStore.Images.Media.IS_PENDING,0);context.getContentResolver().update(media,values,null,null);
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                scenario.onActivity(activity->tagged(activity.getWindow().getDecorView(),"photo").performClick());
                androidx.test.uiautomator.UiDevice device=androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
                androidx.test.uiautomator.UiObject file=device.findObject(new androidx.test.uiautomator.UiSelector().textContains("BMD-virtual-photo"));
                if(!file.waitForExists(5000)){
                    androidx.test.uiautomator.UiObject roots=device.findObject(new androidx.test.uiautomator.UiSelector().descriptionContains("Show roots"));
                    if(roots.exists())roots.click();
                    androidx.test.uiautomator.UiObject downloads=device.findObject(new androidx.test.uiautomator.UiSelector().text("Downloads"));
                    if(downloads.waitForExists(5000))downloads.click();
                }
                screenshot("05-android-photo-picker");
                java.io.ByteArrayOutputStream hierarchy=new java.io.ByteArrayOutputStream();device.dumpWindowHierarchy(hierarchy);System.out.println("PICKER_HIERARCHY: "+hierarchy.toString("UTF-8"));
                android.os.ParcelFileDescriptor[] pipe=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommandRw("tee /data/local/tmp/bmd-picker.xml");
                try(android.os.ParcelFileDescriptor.AutoCloseOutputStream out=new android.os.ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])){out.write(hierarchy.toByteArray());}
                try(android.os.ParcelFileDescriptor.AutoCloseInputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(pipe[0])){while(in.read()!=-1){}}
                if(file.waitForExists(5000))file.click();
                else{
                    androidx.test.uiautomator.UiObject thumbnail=device.findObject(new androidx.test.uiautomator.UiSelector().resourceIdMatches(".*:id/(icon_thumb|icon_thumbnail|thumbnail)"));
                    assertTrue("The seeded image must be visible in the Android picker",thumbnail.waitForExists(5000));thumbnail.click();
                }
                AtomicBoolean imported=new AtomicBoolean(false);long end=System.currentTimeMillis()+45000;
                while(!imported.get()&&System.currentTimeMillis()<end){
                    Thread.sleep(100);scenario.onActivity(activity->imported.set(((List<?>)field(activity,"results")).contains("BMD-PHOTO-PICKER-OK")));
                }
                assertTrue("Selecting a photo must add its QR code to Results",imported.get());
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
                screenshot("06-photo-import-result");
            }
        }finally{context.getContentResolver().delete(media,null,null);}
    }

}
