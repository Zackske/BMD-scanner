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
    @Test public void finalControlKeepsAllThreeFiltersAndResumesAfterALinkPreview()throws Exception{
        DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("codeMode","bar").commit();
        java.util.concurrent.atomic.AtomicReference<android.view.View> preview=new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<BoxSession> saved=new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger epoch=new java.util.concurrent.atomic.AtomicInteger();
        try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl").putExtra("expected",3).putExtra("flowMode","stack"))){
            scenario.onActivity(a->{
                assertEquals("bar",DeviceSupport.get(a,"mode"));
                a.session.tracker.importCode(new ScanTracker.Detection("BMD-2610143","Barcode",.1f,.1f,.3f,.2f),1000);
                a.session.products.colour("2610143");saved.set(a.session);
                DeviceSupport.tag(a.getWindow().getDecorView(),"box-camera-mode").performClick();
                android.widget.PopupWindow options=(android.widget.PopupWindow)DeviceSupport.get(a,"formatPopup");
                assertNotNull(DeviceSupport.tag(options.getContentView(),"box-format-qr"));
                assertNotNull(DeviceSupport.tag(options.getContentView(),"box-format-bar"));
                assertNotNull(DeviceSupport.tag(options.getContentView(),"box-format-both"));
                DeviceSupport.tag(options.getContentView(),"box-format-qr").performClick();
                assertSame(saved.get(),a.session);assertEquals(1,a.session.tracker.items().size());
                preview.set((android.view.View)DeviceSupport.get(a,"preview"));epoch.set((Integer)DeviceSupport.get(a,"epoch"));
                a.showCode(new GenericCode("https://example.org/product","QR"));
                Dialog result=(Dialog)DeviceSupport.get(a,"codeResult");assertTrue(result instanceof GenericResultSheet);
                android.view.View root=result.getWindow().getDecorView();
                assertNotNull(DeviceSupport.tag(root,"generic-open-link"));
                assertNull("A preview requires an explicit tap",DeviceSupport.tag(root,"generic-link-preview-web"));
                assertEquals(1,a.session.tracker.items().size());
            });
            DeviceSupport.shot(scenario,"build410-control-link-preview");
            scenario.onActivity(a->{Dialog result=(Dialog)DeviceSupport.get(a,"codeResult");DeviceSupport.tag(result.getWindow().getDecorView(),"generic-close").performClick();});
            android.os.SystemClock.sleep(350);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a->{
                assertNull(DeviceSupport.get(a,"codeResult"));assertEquals("qr",DeviceSupport.get(a,"mode"));
                assertEquals("qr",a.getSharedPreferences("MainActivity",0).getString("codeMode",""));
                assertSame("Closing a preview keeps the camera view",preview.get(),DeviceSupport.get(a,"preview"));
                assertEquals("Closing a preview does not rebind the camera",epoch.get(),DeviceSupport.get(a,"epoch"));
                assertEquals(Boolean.TRUE,DeviceSupport.get(a,"inCamera"));assertSame(saved.get(),a.session);
                assertEquals(3,a.session.expected);assertEquals(1,a.session.tracker.items().size());assertTrue(a.session.products.colours.containsKey("2610143"));
            });
        }
    }
    @Test public void sharedReaderComponentStillDecodesTheEmulatorCamera()throws Exception{
        try(ActivityScenario<GenericScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),GenericScanActivity.class).putExtra("language","nl"))){
            java.util.concurrent.atomic.AtomicBoolean detected=new java.util.concurrent.atomic.AtomicBoolean();long end=android.os.SystemClock.elapsedRealtime()+45000;
            while(android.os.SystemClock.elapsedRealtime()<end){scenario.onActivity(reader->{android.view.View root=reader.getWindow().getDecorView();assertNotNull(DeviceSupport.tag(root,"generic-preview"));assertNull(DeviceSupport.tag(root,"generic-mode"));detected.set(DeviceSupport.get(reader,"camera")!=null&&!((Map<?,?>)DeviceSupport.get(reader,"history")).isEmpty());});if(detected.get())break;android.os.SystemClock.sleep(150);}
            assertTrue(detected.get());DeviceSupport.shot(null,"build48-generic-reader-live-result");
        }
    }
    private Bitmap symbol(String text,BarcodeFormat format,int width,int height)throws Exception{BitMatrix bits=new MultiFormatWriter().encode(text,format,width,height);width=bits.getWidth();height=bits.getHeight();int[] rgb=new int[width*height];for(int y=0;y<height;y++)for(int x=0;x<width;x++)rgb[y*width+x]=bits.get(x,y)?Color.BLACK:Color.WHITE;return Bitmap.createBitmap(rgb,width,height,Bitmap.Config.ARGB_8888);}
    @Test public void ordinaryCodesAndMedicalDataMatrixAreReadableWithoutAPicklist()throws Exception{
        for(BarcodeFormat format:new BarcodeFormat[]{BarcodeFormat.QR_CODE,BarcodeFormat.DATA_MATRIX,BarcodeFormat.CODE_128,BarcodeFormat.AZTEC,BarcodeFormat.PDF_417}){Bitmap bitmap=symbol("BMD-TEST-2610143",format,600,400);try{List<GenericCode> decoded=GenericCodeReader.read(bitmap);assertEquals(format.name(),1,decoded.size());assertEquals("BMD-TEST-2610143",decoded.get(0).value);}finally{bitmap.recycle();}}
    }
    @Test public void mixedSceneReadsQrAndBarcodeTogetherWithoutChoosingAMode()throws Exception{
        Bitmap qr=symbol("https://example.org/product",BarcodeFormat.QR_CODE,400,400),bar=symbol("BMD-TEST-123",BarcodeFormat.CODE_128,600,200),scene=Bitmap.createBitmap(900,750,Bitmap.Config.ARGB_8888);try{Canvas c=new Canvas(scene);c.drawColor(Color.WHITE);c.drawBitmap(qr,50,30,null);c.drawBitmap(bar,150,500,null);List<GenericCode> decoded=GenericCodeReader.read(scene);assertEquals(2,decoded.size());Set<String> values=new HashSet<>();for(GenericCode code:decoded)values.add(code.value);assertEquals(new HashSet<>(Arrays.asList("https://example.org/product","BMD-TEST-123")),values);}finally{qr.recycle();bar.recycle();scene.recycle();}
    }
    @Test public void resultPopupDoesNotOpenLinksUntilUserChooses()throws Exception{
        android.app.Instrumentation inst=InstrumentationRegistry.getInstrumentation();
        try(ActivityScenario<GenericScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),GenericScanActivity.class).putExtra("language","nl"))){
            IntentFilter links=new IntentFilter(Intent.ACTION_VIEW);links.addCategory(Intent.CATEGORY_BROWSABLE);links.addDataScheme("http");links.addDataScheme("https");
            android.app.Instrumentation.ActivityMonitor monitor=inst.addMonitor(links,null,true);
            java.util.concurrent.atomic.AtomicReference<Dialog> shown=new java.util.concurrent.atomic.AtomicReference<>();
            try{
                scenario.onActivity(a->{a.showResults(Collections.singletonList(new GenericCode("https://example.org/product","QR CODE")));Dialog dialog=(Dialog)DeviceSupport.get(a,"resultDialog");shown.set(dialog);TextView value=(TextView)DeviceSupport.tag(dialog.getWindow().getDecorView(),"generic-result-value");assertEquals("https://example.org/product",value.getText().toString());assertTrue(value.isTextSelectable());assertEquals(0,monitor.getHits());DeviceSupport.tag(dialog.getWindow().getDecorView(),"generic-close").performClick();});
                android.os.SystemClock.sleep(350);inst.waitForIdleSync();
                scenario.onActivity(a->{assertFalse("The closed result is no longer visible",shown.get().isShowing());Dialog current=(Dialog)DeviceSupport.get(a,"resultDialog");assertNotSame("Dismiss callbacks must release the closed result",shown.get(),current);assertEquals("Scanning resumes, or pauses again for a fresh camera result",current!=null,(Boolean)DeviceSupport.get(a,"paused"));assertEquals(0,monitor.getHits());});
            }finally{inst.removeMonitor(monitor);}
        }
    }
    @Test public void pickingJobPersistsWithoutPretendingTwoHundredProductsFitOneCarton()throws Exception{
        PicklistJob job=PicklistJob.create(DeviceSupport.context());job.controlMode="picking";job.captureMode="stack";job.rows.add(new PicklistData.Row("PRO2520","2610143","2028-04-18",200,false));job.confirmed=true;job.save();PicklistJob loaded=PicklistJob.load(DeviceSupport.context(),job.directory.getPath());assertTrue(loaded.isPicking());assertEquals("stack",loaded.captureMode);assertEquals(1,loaded.plan().boxes);assertArrayEquals(new int[]{200},loaded.plan().quantities);assertFalse(loaded.complete());assertEquals(2,new CartonPlan(200,113).boxes);
    }
    @Test public void linkPreviewWaitsForTapAndReleasesItsWebView()throws Exception{
        java.util.concurrent.atomic.AtomicInteger requests=new java.util.concurrent.atomic.AtomicInteger();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{
                android.widget.FrameLayout host=new android.widget.FrameLayout(a);a.setContentView(host);
                LinkPreviewView card=new LinkPreviewView(a,"https://example.org/product",key->key,(web,url)->{assertEquals("https://example.org/product",url);requests.incrementAndGet();web.getSettings().setBlockNetworkLoads(true);web.loadDataWithBaseURL("https://example.org/","<html><head><title>Local preview test</title></head><body>Local test content</body></html>","text/html","UTF-8",null);});
                host.addView(card);assertEquals(0,requests.get());assertNull(DeviceSupport.tag(card,"generic-link-preview-web"));
                DeviceSupport.tag(card,"generic-preview-load").performClick();assertEquals(1,requests.get());
                android.webkit.WebView web=(android.webkit.WebView)DeviceSupport.tag(card,"generic-link-preview-web");assertNotNull(web);
                assertFalse(web.getSettings().getJavaScriptEnabled());assertFalse(web.getSettings().getAllowFileAccess());assertFalse(web.getSettings().getAllowContentAccess());assertTrue(web.getSettings().getSafeBrowsingEnabled());assertEquals(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW,web.getSettings().getMixedContentMode());
                DeviceSupport.tag(card,"generic-preview-load").performClick();assertNull(DeviceSupport.tag(card,"generic-link-preview-web"));
                DeviceSupport.tag(card,"generic-preview-load").performClick();assertEquals(2,requests.get());host.removeView(card);assertEquals(Boolean.TRUE,DeviceSupport.get(card,"disposed"));assertNull(DeviceSupport.get(card,"web"));
            });
        }
    }
    @Test public void browserButtonIsExplicitAndProductCodesHaveNoLinkActions()throws Exception{
        android.app.Instrumentation inst=InstrumentationRegistry.getInstrumentation();IntentFilter links=new IntentFilter(Intent.ACTION_VIEW);links.addCategory(Intent.CATEGORY_BROWSABLE);links.addDataScheme("https");android.app.Instrumentation.ActivityMonitor monitor=inst.addMonitor(links,null,true);
        try(ActivityScenario<GenericScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),GenericScanActivity.class).putExtra("language","nl"))){
            scenario.onActivity(a->{
                a.showResults(Collections.singletonList(new GenericCode("BMD-000123","QR CODE")));Dialog plain=(Dialog)DeviceSupport.get(a,"resultDialog");assertNull(DeviceSupport.tag(plain.getWindow().getDecorView(),"generic-open-link"));assertNull(DeviceSupport.tag(plain.getWindow().getDecorView(),"generic-link-preview"));
                a.showResults(Collections.singletonList(new GenericCode("https://example.org/product","QR CODE")));Dialog result=(Dialog)DeviceSupport.get(a,"resultDialog");android.view.View root=result.getWindow().getDecorView();assertNotNull(DeviceSupport.tag(root,"generic-link-preview"));assertNull(DeviceSupport.tag(root,"generic-link-preview-web"));assertEquals(0,monitor.getHits());
                DeviceSupport.tag(root,"generic-open-link").performClick();assertEquals(1,monitor.getHits());DeviceSupport.tag(root,"generic-close").performClick();
            });
        }finally{inst.removeMonitor(monitor);}
    }
    @Test public void previewFailureCanRetryWithoutOpeningTheBrowser()throws Exception{
        java.util.concurrent.atomic.AtomicInteger requests=new java.util.concurrent.atomic.AtomicInteger();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{
                android.widget.FrameLayout host=new android.widget.FrameLayout(a);a.setContentView(host);LinkPreviewView card=new LinkPreviewView(a,"https://example.org/",key->key,(web,url)->{if(requests.incrementAndGet()==1)throw new IllegalStateException("Local preview failure");web.getSettings().setBlockNetworkLoads(true);web.loadDataWithBaseURL("https://example.org/","<html><body>Retry succeeded</body></html>","text/html","UTF-8",null);});host.addView(card);
                DeviceSupport.tag(card,"generic-preview-load").performClick();assertEquals(1,requests.get());assertNull(DeviceSupport.tag(card,"generic-link-preview-web"));assertEquals("generic_preview_retry",((TextView)DeviceSupport.tag(card,"generic-preview-load")).getText().toString());
                DeviceSupport.tag(card,"generic-preview-load").performClick();assertEquals(2,requests.get());assertNotNull(DeviceSupport.tag(card,"generic-link-preview-web"));host.removeView(card);assertNull(DeviceSupport.get(card,"web"));
            });
        }
    }
    private static void assertTextFits(android.view.View view){
        if(view.getVisibility()!=android.view.View.VISIBLE)return;
        if(view instanceof TextView){TextView text=(TextView)view;android.text.Layout layout=text.getLayout();if(layout!=null)for(int line=0;line<layout.getLineCount();line++)assertTrue("Text fits: "+text.getText(),layout.getLineWidth(line)<=text.getWidth()-text.getCompoundPaddingLeft()-text.getCompoundPaddingRight()+2);}
        if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)assertTextFits(((android.view.ViewGroup)view).getChildAt(i));
    }
    @Test public void silverResultSheetFitsEveryThemeAndLanguage()throws Exception{
        try(ActivityScenario<GenericScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),GenericScanActivity.class).putExtra("language","nl"))){
            for(String theme:ThemeSkin.IDS)for(String language:MainActivity.LANGS){
                scenario.onActivity(a->{ThemeSkin.active=ThemeSkin.forId(theme);android.content.res.Configuration cfg=new android.content.res.Configuration(a.getResources().getConfiguration());cfg.setLocale(Locale.forLanguageTag(language));DeviceSupport.set(a,"words",a.createConfigurationContext(cfg));DeviceSupport.set(a,"language",language);a.showCode(new GenericCode("https://example.org/product","QR CODE"));});
                android.os.SystemClock.sleep(300);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a->{Dialog dialog=(Dialog)DeviceSupport.get(a,"resultDialog");assertTrue(dialog instanceof GenericResultSheet);android.view.View root=dialog.getWindow().getDecorView(),panel=DeviceSupport.tag(root,"generic-result-popup");assertTrue(panel.getWidth()>0);assertTextFits(panel);assertNotNull(DeviceSupport.tag(root,"generic-open-link"));assertNotNull(DeviceSupport.tag(root,"generic-copy"));assertNotNull(DeviceSupport.tag(root,"generic-share"));assertNull("No website request before a tap",DeviceSupport.tag(root,"generic-link-preview-web"));assertNull(DeviceSupport.tag(a.getWindow().getDecorView(),"generic-mode"));});
                if(language.equals("nl"))DeviceSupport.shot(null,"build49-qr-silver-sheet-"+theme);
            }
        }finally{ThemeSkin.active=ThemeSkin.BMD;}
    }
    @Test public void resultSheetAndAutomaticReaderFitLandscape()throws Exception{
        DeviceSupport.shell("touch /data/local/tmp/bmd-rotate-landscape");
        try(ActivityScenario<GenericScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),GenericScanActivity.class).putExtra("language","de"))){
            java.util.concurrent.atomic.AtomicBoolean landscape=new java.util.concurrent.atomic.AtomicBoolean();long end=android.os.SystemClock.elapsedRealtime()+15000;
            while(android.os.SystemClock.elapsedRealtime()<end){scenario.onActivity(a->landscape.set(a.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE));if(landscape.get())break;android.os.SystemClock.sleep(200);}assertTrue("Camera follows the landscape sensor",landscape.get());
            scenario.onActivity(a->a.showCode(new GenericCode("https://example.org/product","QR CODE")));android.os.SystemClock.sleep(350);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a->{Dialog dialog=(Dialog)DeviceSupport.get(a,"resultDialog");android.view.View panel=DeviceSupport.tag(dialog.getWindow().getDecorView(),"generic-result-popup");assertTextFits(panel);android.util.DisplayMetrics screen=a.getResources().getDisplayMetrics();assertTrue(panel.getWidth()<=Math.min(screen.widthPixels,screen.heightPixels));assertTrue(dialog.getWindow().getDecorView().getHeight()<=screen.heightPixels);assertNull(DeviceSupport.tag(a.getWindow().getDecorView(),"generic-mode"));});DeviceSupport.shot(null,"build49-qr-silver-sheet-landscape");
        }finally{DeviceSupport.shell("touch /data/local/tmp/bmd-rotate-portrait");android.os.SystemClock.sleep(1600);}
    }
}
