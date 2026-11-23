package com.bmd.scanner;

import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class BoxCaptureFlowTest {
    private static void assertLabelsFit(View view){if(view instanceof android.widget.TextView){android.widget.TextView label=(android.widget.TextView)view;assertNotNull(label.getLayout());assertTrue("Translated label must stay within its line limit: "+label.getText(),label.getLineCount()<=label.getMaxLines());assertTrue("Translated label must not be cropped vertically: "+label.getText(),label.getLayout().getHeight()<=label.getHeight());}if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)assertLabelsFit(((android.view.ViewGroup)view).getChildAt(i));}
    @Test public void allTenLanguagesAndSixThemesFitEveryStepInBothOrientations(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{ThemeSkin original=ThemeSkin.active;try{for(String theme:ThemeSkin.IDS){ThemeSkin.active=ThemeSkin.forId(theme);for(String lang:new String[]{"nl","en","uk","tr","it","de","fr","es","pt","hi"})for(float scale:new float[]{1f,1.3f}){Configuration config=new Configuration(DeviceSupport.context().getResources().getConfiguration());config.setLocale(java.util.Locale.forLanguageTag(lang));config.fontScale=scale;Context words=DeviceSupport.context().createConfigurationContext(config);java.util.function.Function<String,String> s=key->{int id=words.getResources().getIdentifier("ui_"+key,"string",words.getPackageName());assertNotEquals("All new text must exist in "+lang,0,id);return words.getString(id);};for(int current=0;current<5;current++)for(boolean landscape:new boolean[]{false,true}){String title=s.apply(current==0?"overview":new String[]{"top_left","top_right","bottom_left","bottom_right"}[current-1]);String position=String.format(java.util.Locale.forLanguageTag(lang),s.apply("photo_step"),current+1);BoxPhotoSteps panel=new BoxPhotoSteps(words,current,landscape,s,position,title);float d=words.getResources().getDisplayMetrics().density;int width=Math.round((landscape?180:354)*d);panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));panel.layout(0,0,width,panel.getMeasuredHeight());assertLabelsFit(panel);assertTrue("Step guide should remain compact: "+lang+" "+theme,panel.getHeight()<Math.round((landscape?110:90)*d));Bitmap image=Bitmap.createBitmap(width,panel.getHeight(),Bitmap.Config.ARGB_8888);panel.draw(new Canvas(image));assertTrue(Color.alpha(image.getPixel(width/2,image.getHeight()/2))>0);image.recycle();String number=String.format(java.util.Locale.forLanguageTag(lang),s.apply("capture_step"),current+1);assertFalse(number.contains("%"));assertFalse(s.apply("capture_photo").isEmpty());assertFalse(s.apply("marker_setup").isEmpty());}}}}finally{ThemeSkin.active=original;}});
    }
    interface Check{boolean test(BoxScanActivity a);}
    static void waitFor(ActivityScenario<BoxScanActivity> scenario,Check check,String message)throws Exception{long end=SystemClock.elapsedRealtime()+45000;AtomicBoolean done=new AtomicBoolean();while(SystemClock.elapsedRealtime()<end){scenario.onActivity(a->done.set(check.test(a)));if(done.get())return;SystemClock.sleep(150);}throw new AssertionError(message);}
    static void shot(String name)throws Exception{DeviceSupport.shell("mkdir -p /data/local/tmp/bmd-screenshots");DeviceSupport.shell("screencap -p /data/local/tmp/bmd-screenshots/"+name+".png");}
    static void rotate(boolean landscape)throws Exception{UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).unfreezeRotation();DeviceSupport.shell("touch /data/local/tmp/bmd-rotate-"+(landscape?"landscape":"portrait"));}
    @Test public void allThemesKeepExactBarWidthAndFirstPhotoOnlyMarkerPopupAcrossRotation()throws Exception{
        DeviceSupport.permission();
        try{for(String theme:ThemeSkin.IDS){DeviceSupport.clear();DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("colourTheme",theme).commit();
            try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl").putExtra("flowMode","box").putExtra("expected",1))){
                scenario.onActivity(a->DeviceSupport.call(a,"cameraPage",new Class<?>[]{}));waitFor(scenario,a->(Boolean)DeviceSupport.get(a,"ready")&&a.markerPopup!=null,"Overview camera and marker instructions must open");shot("build44-markers-"+theme);
                scenario.onActivity(a->{assertFalse((Boolean)DeviceSupport.get(a,"markerAcknowledged"));a.markerPopup.close();});waitFor(scenario,a->a.markerPopup==null,"Marker instructions must close");SystemClock.sleep(300);
                int[] width={0};scenario.onActivity(a->{View root=a.getWindow().getDecorView(),top=DeviceSupport.tag(root,"box-toolbar"),dock=DeviceSupport.tag(root,"scanner-dock"),guide=DeviceSupport.tag(root,"box-photo-guide"),info=DeviceSupport.tag(root,"box-marker-info");width[0]=top.getWidth();assertEquals(width[0],dock.getWidth());assertEquals(width[0],guide.getWidth());assertTrue(info.getTop()>=guide.getBottom());assertTrue(DeviceSupport.tag(root,"box-shutter") instanceof ScannerControls.PhotoStep);assertTrue((Boolean)DeviceSupport.get(a,"markerAcknowledged"));});shot("build44-box-portrait-"+theme);
                rotate(true);waitFor(scenario,a->a.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE&&(Boolean)DeviceSupport.get(a,"ready"),"Camera must rotate to landscape");SystemClock.sleep(350);
                scenario.onActivity(a->{View root=a.getWindow().getDecorView(),top=DeviceSupport.tag(root,"box-toolbar"),dock=DeviceSupport.tag(root,"scanner-dock"),guide=DeviceSupport.tag(root,"box-photo-guide"),info=DeviceSupport.tag(root,"box-marker-info");assertEquals("Same physical bar length after rotation",width[0],top.getWidth());assertEquals(top.getWidth(),dock.getWidth());assertTrue(guide.getLeft()<top.getLeft());assertTrue(guide.getTop()<top.getBottom());assertTrue(info.getLeft()>top.getRight());assertNull("Acknowledged marker popup must not return on rotation",a.markerPopup);assertEquals(1,a.session.expected);});shot("build44-box-landscape-"+theme);
                rotate(false);waitFor(scenario,a->a.getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT&&(Boolean)DeviceSupport.get(a,"ready"),"Camera must return to portrait");
            }
        }}finally{rotate(false);}
    }
    private static File fixture(File folder,String name)throws Exception{Bitmap bitmap=Bitmap.createBitmap(1600,1600,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);String[] values={"BMD-BOX-A","BMD-BOX-B","BMD-BOX-C","BMD-BOX-D","(01)08720634200066(17)280418(10)2610143"};for(int n=0;n<5;n++){int x=n==4?600:60+(n%2)*1180,y=n==4?600:60+(n/2)*1180,size=n==4?400:300;BitMatrix qr=new MultiFormatWriter().encode(values[n],BarcodeFormat.QR_CODE,size,size);for(int yy=0;yy<size;yy++)for(int xx=0;xx<size;xx++)if(qr.get(xx,yy))bitmap.setPixel(x+xx,y+yy,Color.BLACK);}File file=new File(folder,name);try(FileOutputStream out=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();return file;}
    @Test public void realMarkerPhotosAdvanceOnlyAfterDecodeAndConfirmedPlacementThenOpenReview()throws Exception{
        DeviceSupport.permission();DeviceSupport.clear();
        try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl").putExtra("flowMode","box").putExtra("expected",1))){
            scenario.onActivity(a->DeviceSupport.call(a,"cameraPage",new Class<?>[]{}));waitFor(scenario,a->a.markerPopup!=null,"First photo needs marker instructions");scenario.onActivity(a->a.markerPopup.close());waitFor(scenario,a->a.markerPopup==null,"Instructions must dismiss");
            File[] folder={null};scenario.onActivity(a->folder[0]=(File)DeviceSupport.get(a,"folder"));File overview=fixture(folder[0],"fixture-overview.png");scenario.onActivity(a->DeviceSupport.call(a,"processPhoto",new Class<?>[]{File.class},overview));waitFor(scenario,a->DeviceSupport.get(a,"overview")!=null&&(Boolean)DeviceSupport.get(a,"ready"),"Decoded overview must advance to live photo 2");
            scenario.onActivity(a->{assertEquals(1,((Integer)DeviceSupport.call(a,"next",new Class<?>[]{})).intValue());assertNull(a.markerPopup);assertNull(DeviceSupport.tag(a.getWindow().getDecorView(),"box-marker-info"));assertEquals(4,((java.util.List<ScanTracker.Detection>)DeviceSupport.get(a,"reference")).stream().filter(d->BoxSession.marker(d.value)).count());assertTrue(a.session.tracker.items().isEmpty());});shot("build44-box-step-2");
            for(int n=1;n<=4;n++){File detail=fixture(folder[0],"fixture-detail-"+n+".png");final int count=n;scenario.onActivity(a->DeviceSupport.call(a,"processPhoto",new Class<?>[]{File.class},detail));waitFor(scenario,a->DeviceSupport.tag(a.getWindow().getDecorView(),"box-confirm-placement")!=null,"Marker-aligned photo must await operator confirmation");scenario.onActivity(a->{assertEquals(count-1,a.session.shots.size());assertNull(a.markerPopup);DeviceSupport.tag(a.getWindow().getDecorView(),"box-confirm-placement").performClick();});
                waitFor(scenario,a->a.session.shots.size()==count&&(count==4?DeviceSupport.tag(a.getWindow().getDecorView(),"continue-save")!=null:(Boolean)DeviceSupport.get(a,"ready")),"Only confirmed photo advances the guide");scenario.onActivity(a->{assertFalse(a.session.reviewed);assertEquals(1,a.session.products.view(a.session.tracker.items()).size());assertNull(a.markerPopup);});shot("build44-after-detail-"+n);
            }
            scenario.onActivity(a->{assertFalse(a.session.ready());assertNotNull(DeviceSupport.tag(a.getWindow().getDecorView(),"continue-save"));assertEquals(4,a.session.shots.size());assertFalse((Boolean)DeviceSupport.get(a,"inCamera"));});
        }
    }
}
