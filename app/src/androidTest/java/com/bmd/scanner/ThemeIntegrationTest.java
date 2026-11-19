package com.bmd.scanner;

import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ThemeIntegrationTest {
    @Before public void before()throws Exception{DeviceSupport.clear();DeviceSupport.permission();}
    @Test public void allSkinsKeepMenuStructureOriginalTransparentBrandAndPersist()throws Exception{
        for(String theme:ThemeSkin.IDS){DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("colourTheme",theme).putString("language","nl").commit();
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                scenario.onActivity(a->{assertEquals(theme,ThemeSkin.active.id);assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,a.getRequestedOrientation());a.showHome();assertNotNull(DeviceSupport.tag(a.getWindow().getDecorView(),"reference-home"));});DeviceSupport.shot(scenario,"build3-theme-home-"+theme);
                scenario.onActivity(a->{a.chooseScan();});DeviceSupport.shot(scenario,"build3-theme-scan-menu-"+theme);
                scenario.onActivity(a->{a.showSettings();View decor=a.getWindow().getDecorView();assertNotNull(DeviceSupport.tag(decor,"language"));assertNotNull(DeviceSupport.tag(decor,"themes"));assertNotNull(DeviceSupport.tag(decor,"about"));assertNull(DeviceSupport.tag(decor,"scanner-settings"));assertNull(DeviceSupport.tag(decor,"document-settings"));View brand=DeviceSupport.tag(decor,"theme-brand");assertNotNull(brand);assertNull(brand.getBackground());assertEquals(0,brand.getElevation(),0);assertTrue(brand.getContentDescription().toString().contains(theme.equals("translumina")?"Translumina":"Blue Medical"));});DeviceSupport.shot(scenario,"build3-theme-settings-"+theme);
                scenario.recreate();scenario.onActivity(a->{assertEquals(theme,ThemeSkin.active.id);a.themes();android.app.Dialog dialog=(android.app.Dialog)DeviceSupport.get(a,"dialog");for(String id:ThemeSkin.IDS)assertNotNull(DeviceSupport.tag(dialog.getWindow().getDecorView(),"theme-"+id));dialog.dismiss();});
            }
        }
    }
    @Test public void choosingAnyLanguageImmediatelyTranslatesHeadingAndApplyBeforeSaving()throws Exception{
        for(String theme:ThemeSkin.IDS){DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("colourTheme",theme).putString("language","nl").commit();
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                scenario.onActivity(a->{a.showSettings();a.languages();});
                for(int n=0;n<MainActivity.LANGS.length;n++){final int index=n;scenario.onActivity(a->{android.app.Dialog d=(android.app.Dialog)DeviceSupport.get(a,"dialog");View root=d.getWindow().getDecorView();RadioGroup choices=(RadioGroup)DeviceSupport.tag(root,"language-options");choices.check(choices.getChildAt(index).getId());assertEquals(a.s("choose_language"),((TextView)DeviceSupport.tag(root,"sheet-title")).getText().toString());assertEquals(a.s("apply"),((TextView)DeviceSupport.tag(root,"language-apply")).getText().toString());assertEquals("nl",a.getSharedPreferences("MainActivity",0).getString("language",""));if(index==1)assertEquals("Apply",((TextView)DeviceSupport.tag(root,"language-apply")).getText().toString());});}
                scenario.onActivity(a->{android.app.Dialog d=(android.app.Dialog)DeviceSupport.get(a,"dialog");RadioGroup choices=(RadioGroup)DeviceSupport.tag(d.getWindow().getDecorView(),"language-options");choices.check(choices.getChildAt(1).getId());});DeviceSupport.shot(scenario,"build3-theme-english-preview-"+theme);
                scenario.onActivity(a->{android.app.Dialog d=(android.app.Dialog)DeviceSupport.get(a,"dialog");DeviceSupport.tag(d.getWindow().getDecorView(),"language-apply").performClick();assertEquals("en",a.getSharedPreferences("MainActivity",0).getString("language",""));assertTrue(DeviceSupport.hasText(a.getWindow().getDecorView(),"Themes"));});
            }
        }
    }
    @Test public void nativeZoomAnimatesInEveryPaletteAndCameraAloneMayRotate()throws Exception{
        for(String theme:ThemeSkin.IDS){DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().putString("colourTheme",theme).commit();DeviceSupport.context().getSharedPreferences("BoxScanActivity",0).edit().clear().commit();
            try(ActivityScenario<BoxScanActivity> scenario=ActivityScenario.launch(new Intent(DeviceSupport.context(),BoxScanActivity.class).putExtra("language","nl"))){UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());UiObject2 input=device.wait(Until.findObject(By.clazz(EditText.class)),5000);assertNotNull(input);input.setText("13");device.findObject(By.res("android:id/button1")).click();SystemClock.sleep(250);scenario.onActivity(a->DeviceSupport.call(a,"cameraPage",new Class<?>[]{}));long deadline=SystemClock.elapsedRealtime()+35000;AtomicBoolean ready=new AtomicBoolean();while(SystemClock.elapsedRealtime()<deadline){scenario.onActivity(a->ready.set((Boolean)DeviceSupport.get(a,"ready")));if(ready.get())break;SystemClock.sleep(150);}assertTrue("Camera must bind",ready.get());
                scenario.onActivity(a->{View root=a.getWindow().getDecorView();assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,a.getRequestedOrientation());assertNull(DeviceSupport.tag(root,"theme-brand"));View viewport=DeviceSupport.tag(root,"box-camera-viewport");assertEquals(0,viewport.getTop());assertEquals(((View)viewport.getParent()).getHeight(),viewport.getHeight());VerticalZoom zoom=(VerticalZoom)DeviceSupport.tag(root,"vertical-zoom");assertEquals(viewport.getHeight()/2f,(zoom.getTop()+zoom.getBottom())/2f,2);VerticalZoom synthetic=new VerticalZoom(a);synthetic.setup(1,3,1,Locale.ENGLISH,synthetic::value);synthetic.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,null);assertEquals(2f,(Float)DeviceSupport.get(synthetic,"value"),.001f);if(android.animation.ValueAnimator.areAnimatorsEnabled()){assertNotNull(DeviceSupport.get(synthetic,"motion"));assertNotNull(DeviceSupport.get(synthetic,"effect"));}assertTrue(a.session.tracker.items().isEmpty());});DeviceSupport.shot(null,"build3-theme-camera-"+theme);
                if(theme.equals("translumina")){device.setOrientationLeft();SystemClock.sleep(650);scenario.onActivity(a->{assertEquals(13,a.session.expected);assertTrue(a.session.tracker.items().isEmpty());assertNotNull(DeviceSupport.tag(a.getWindow().getDecorView(),"box-camera-viewport"));});DeviceSupport.shot(null,"build3-theme-camera-landscape");device.setOrientationNatural();SystemClock.sleep(500);device.unfreezeRotation();}
                scenario.onActivity(a->{a.summary();assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,a.getRequestedOrientation());assertEquals(13,a.session.expected);});
            }
        }
    }
}
