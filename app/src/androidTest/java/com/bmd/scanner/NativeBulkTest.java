package com.bmd.scanner;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import java.util.*;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NativeBulkTest {
    private Bitmap fixture(int count)throws Exception{
        int cols=count>20?20:4,cell=112,rows=(count+cols-1)/cols;Bitmap image=Bitmap.createBitmap(cols*cell,rows*cell,Bitmap.Config.ARGB_8888);image.eraseColor(Color.WHITE);
        for(int n=0;n<count;n++){int index=n+1;String lot=count==13?(Arrays.asList(2,6,11).contains(index)?"2026144":Arrays.asList(4,9).contains(index)?"2610134":"2610143"):"2610143";String value="(01)08720634200066(17)280418(10)"+lot;
            BitMatrix matrix=new MultiFormatWriter().encode(value,BarcodeFormat.DATA_MATRIX,96,96);int ox=(n%cols)*cell+8,oy=(n/cols)*cell+8;for(int y=0;y<matrix.getHeight();y++)for(int x=0;x<matrix.getWidth();x++)image.setPixel(ox+x,oy+y,matrix.get(x,y)?Color.BLACK:Color.WHITE);
        }return image;
    }
    @Test public void thirteenRealSymbolsHaveTheExpectedMixedLotCounts()throws Exception{
        Bitmap image=fixture(13);try{List<ScanTracker.Detection> found=NativeCodes.decode(image,"both");assertEquals(13,found.size());ScanTracker tracker=new ScanTracker();tracker.update(found,1,1);ProductCatalog catalog=new ProductCatalog();assertEquals(13,catalog.project(tracker.items()).size());assertEquals(Integer.valueOf(8),catalog.lotCounts(tracker.items()).get("2610143"));assertEquals(Integer.valueOf(2),catalog.lotCounts(tracker.items()).get("2610134"));assertEquals(Integer.valueOf(3),catalog.lotCounts(tracker.items()).get("2026144"));assertTrue(NativeCodes.decode(image,"bar").isEmpty());assertEquals(13,NativeCodes.decode(image,"qr").size());}finally{image.recycle();}
    }
    @Test public void decodesTwoHundredSeparateDataMatrixSymbolsInSyntheticImage()throws Exception{
        Bitmap image=fixture(200);long start=android.os.SystemClock.elapsedRealtime();try{List<ScanTracker.Detection> found=NativeCodes.decode(image,"both");android.util.Log.i("BMD-bulk","Synthetic 200 read="+found.size()+" elapsedMs="+(android.os.SystemClock.elapsedRealtime()-start));assertEquals(200,found.size());ScanTracker tracker=new ScanTracker();tracker.update(found,1,1);tracker.update(found,2,1);assertEquals(200,tracker.items().size());}finally{image.recycle();}
    }
    @Test public void inspectRealOwnerExamplesWithoutInventingPayloads()throws Exception{
        for(String name:new String[]{"stacked-example.png","single-package-example.png"}){Bitmap image;try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name)){image=BitmapFactory.decodeStream(in);}assertNotNull(image);try{List<ScanTracker.Detection> found=NativeCodes.decode(image,"both");android.util.Log.i("BMD-real",name+" decoded="+found.size());for(ScanTracker.Detection d:found)android.util.Log.i("BMD-real",d.kind+" raw="+d.value.replace((char)29,'|')+" lot="+LotData.parse(d.value).lot+" box="+d.left+","+d.top+","+d.right+","+d.bottom);assertFalse("Real reference must produce readable codes",found.isEmpty());if(name.equals("single-package-example.png"))assertEquals("Two matrix codes and one physical barcode",3,found.size());}finally{image.recycle();}}
    }
    private static TextView button(View view,String label){if(view instanceof TextView&&label.contentEquals(((TextView)view).getText()))return (TextView)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){TextView found=button(((ViewGroup)view).getChildAt(i),label);if(found!=null)return found;}return null;}
    private static void checks(View view,List<CheckBox> found){if(view instanceof CheckBox)found.add((CheckBox)view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)checks(((ViewGroup)view).getChildAt(i),found);}
    @Test public void groupingReviewActuallyMergesAndSplitsProductRecords()throws Exception{
        DeviceSupport.clear();try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{a.tracker.reset();for(int n=0;n<3;n++)a.tracker.importCode(new ScanTracker.Detection("(01)08720634200066(10)2610143",n==0?"Data Matrix":"Barcode",.1f+n*.2f,.2f,.2f+n*.2f,.3f),0);DeviceSupport.call(a,"reviewProducts",new Class<?>[]{Runnable.class},(Object)null);android.app.Dialog d=(android.app.Dialog)DeviceSupport.get(a,"dialog");List<CheckBox> boxes=new ArrayList<>();checks(d.getWindow().getDecorView(),boxes);assertEquals(3,boxes.size());for(CheckBox box:boxes)box.setChecked(true);button(d.getWindow().getDecorView(),a.s("group_selected")).performClick();assertEquals(1,a.products.view(a.tracker.items()).size());
                d=(android.app.Dialog)DeviceSupport.get(a,"dialog");boxes.clear();checks(d.getWindow().getDecorView(),boxes);boxes.get(0).setChecked(true);button(d.getWindow().getDecorView(),a.s("split_selected")).performClick();assertEquals(3,a.products.view(a.tracker.items()).size());((android.app.Dialog)DeviceSupport.get(a,"dialog")).dismiss();
            });
        }
    }
    @Test public void nativeScreenShowsThirteenDecodedProductsAndLotLegend()throws Exception{
        DeviceSupport.clear();DeviceSupport.permission();Bitmap image=fixture(13);List<ScanTracker.Detection> found=NativeCodes.decode(image,"both");assertEquals(13,found.size());
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(MainActivity::showScanner);DeviceSupport.await(scenario,a->Boolean.TRUE.equals(DeviceSupport.get(a,"cameraReady")),35);
            scenario.onActivity(a->{DeviceSupport.set(a,"scanning",false);a.tracker.reset();a.accept(found,1);List<ScanTracker.Item> rows=a.tracker.items();DeviceSupport.set(a,"captureRows",rows);Set<Integer> visible=(Set<Integer>)DeviceSupport.get(a,"captureVisibleIds");for(ScanTracker.Item row:rows)visible.add(row.id);DeviceSupport.call(a,"freezePhoto",new Class<?>[]{Bitmap.class},image);assertEquals(13,a.products.view(rows).size());assertTrue(DeviceSupport.tag(a.getWindow().getDecorView(),"lot-legend") instanceof TextView);});
            DeviceSupport.shot(scenario,"build3-bulk-synthetic-lots");
        }finally{if(!image.isRecycled())image.recycle();}
    }
    @Test public void overlappingBarcodeBandsMergeButAdjacentBoxesRemainSeparate(){
        ScanTracker.Detection a=new ScanTracker.Detection("same","Barcode",.22f,.66f,.75f,.74f),b=new ScanTracker.Detection("same","Barcode",.22f,.72f,.75f,.81f),nextBox=new ScanTracker.Detection("same","Barcode",.22f,.84f,.75f,.93f);
        assertEquals(1,NativeCodes.mergeLinear(Arrays.asList(a,b),765,300).size());assertEquals(2,NativeCodes.mergeLinear(Arrays.asList(a,b,nextBox),765,300).size());
    }
}

