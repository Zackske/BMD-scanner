package com.bmd.scanner;

import android.app.Dialog;
import android.content.*;
import android.graphics.*;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GenericReaderDeviceTest {
    @Before public void before()throws Exception{DeviceSupport.clear();DeviceSupport.permission();}
    @Test public void qrTileStartsCameraWithoutAControlWizard()throws Exception{
        android.app.Instrumentation inst=InstrumentationRegistry.getInstrumentation();android.app.Instrumentation.ActivityMonitor monitor=inst.addMonitor(GenericScanActivity.class.getName(),null,false);android.app.Activity child=null;
        try(ActivityScenario<MainActivity> parent=ActivityScenario.launch(MainActivity.class)){
            parent.onActivity(a->{a.chooseScan();DeviceSupport.tag(a.getWindow().getDecorView(),"settings").performClick();});
            child=monitor.waitForActivityWithTimeout(15000);assertNotNull("QR tile must open the live reader directly",child);final android.app.Activity reader=child;
            inst.runOnMainSync(()->{android.view.View root=reader.getWindow().getDecorView();assertNotNull(DeviceSupport.tag(root,"generic-preview"));assertNotNull(DeviceSupport.tag(root,"generic-toolbar"));assertNull(DeviceSupport.tag(root,"wizard-step-type"));assertEquals("both",DeviceSupport.get(reader,"mode"));});
        }finally{if(child!=null){android.app.Activity reader=child;inst.runOnMainSync(reader::finish);}inst.removeMonitor(monitor);}
    }
    private Bitmap symbol(String text,BarcodeFormat format,int width,int height)throws Exception{BitMatrix bits=new MultiFormatWriter().encode(text,format,width,height);int[] rgb=new int[width*height];for(int y=0;y<height;y++)for(int x=0;x<width;x++)rgb[y*width+x]=bits.get(x,y)?Color.BLACK:Color.WHITE;return Bitmap.createBitmap(rgb,width,height,Bitmap.Config.ARGB_8888);}
    @Test public void ordinaryCodesAndMedicalDataMatrixAreReadableWithoutAPicklist()throws Exception{
        for(BarcodeFormat format:new BarcodeFormat[]{BarcodeFormat.QR_CODE,BarcodeFormat.DATA_MATRIX,BarcodeFormat.CODE_128,BarcodeFormat.AZTEC,BarcodeFormat.PDF_417}){Bitmap bitmap=symbol("BMD-TEST-2610143",format,600,400);try{List<GenericCode> decoded=GenericCodeReader.read(bitmap,"both");assertEquals(format.name(),1,decoded.size());assertEquals("BMD-TEST-2610143",decoded.get(0).value);}finally{bitmap.recycle();}}
    }
    @Test public void mixedSceneHonoursMatrixAndLinearMode()throws Exception{
        Bitmap qr=symbol("https://example.org/product",BarcodeFormat.QR_CODE,400,400),bar=symbol("BMD-TEST-123",BarcodeFormat.CODE_128,600,200),scene=Bitmap.createBitmap(900,750,Bitmap.Config.ARGB_8888);try{Canvas c=new Canvas(scene);c.drawColor(Color.WHITE);c.drawBitmap(qr,50,30,null);c.drawBitmap(bar,150,500,null);assertEquals(2,GenericCodeReader.read(scene,"both").size());List<GenericCode> matrix=GenericCodeReader.read(scene,"qr"),linear=GenericCodeReader.read(scene,"bar");assertEquals(1,matrix.size());assertEquals("https://example.org/product",matrix.get(0).value);assertEquals(1,linear.size());assertEquals("BMD-TEST-123",linear.get(0).value);}finally{qr.recycle();bar.recycle();scene.recycle();}
    }
    @Test public void resultPopupDoesNotOpenLinksUntilUserChooses()throws Exception{
        android.app.Instrumentation inst=InstrumentationRegistry.getInstrumentation();android.app.Instrumentation.ActivityMonitor monitor=inst.addMonitor(new IntentFilter(Intent.ACTION_VIEW),null,true);
        try(ActivityScenario<GenericScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),GenericScanActivity.class).putExtra("language","nl"))){scenario.onActivity(a->{a.showResults(Collections.singletonList(new GenericCode("https://example.org/product","QR CODE")));Dialog dialog=(Dialog)DeviceSupport.get(a,"resultDialog");TextView value=(TextView)DeviceSupport.tag(dialog.getWindow().getDecorView(),"generic-result-value");assertEquals("https://example.org/product",value.getText().toString());assertTrue(value.isTextSelectable());assertEquals(0,monitor.getHits());DeviceSupport.tag(dialog.getWindow().getDecorView(),"generic-close").performClick();assertNull(DeviceSupport.get(a,"resultDialog"));assertFalse((Boolean)DeviceSupport.get(a,"paused"));});}finally{inst.removeMonitor(monitor);}
    }
    @Test public void pickingJobPersistsWithoutPretendingTwoHundredProductsFitOneCarton()throws Exception{
        PicklistJob job=PicklistJob.create(DeviceSupport.context());job.controlMode="picking";job.captureMode="stack";job.rows.add(new PicklistData.Row("PRO2520","2610143","2028-04-18",200,false));job.confirmed=true;job.save();PicklistJob loaded=PicklistJob.load(DeviceSupport.context(),job.directory.getPath());assertTrue(loaded.isPicking());assertEquals("stack",loaded.captureMode);assertEquals(1,loaded.plan().boxes);assertArrayEquals(new int[]{200},loaded.plan().quantities);assertFalse(loaded.complete());assertEquals(2,new CartonPlan(200,113).boxes);
    }
}
