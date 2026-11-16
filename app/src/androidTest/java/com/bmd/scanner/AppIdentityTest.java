package com.bmd.scanner;

import android.content.*;
import android.content.res.Configuration;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.Locale;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@androidx.test.filters.SdkSuppress(minSdkVersion=31)
public class AppIdentityTest {
    private static Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private static void language(String code){context().getSharedPreferences("MainActivity",0).edit().putString("language",code).commit();}
    private static Object invoke(MainActivity activity,String name,Class<?>[] types,Object...args){try{Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(activity,args);}catch(Exception e){throw new AssertionError(e);}}
    private static View description(View view,String text){if(text.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=description(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
    private static boolean text(View view,String value){if(view instanceof TextView&&value.contentEquals(((TextView)view).getText()))return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(text(((ViewGroup)view).getChildAt(i),value))return true;return false;}
    private static void shell(String cmd)throws Exception{try(android.os.ParcelFileDescriptor.AutoCloseInputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(cmd))){while(in.read()!=-1){}}}
    private static void shot(String name)throws Exception{InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(1000);shell("mkdir -p /data/local/tmp/bmd-screenshots");shell("screencap -p /data/local/tmp/bmd-screenshots/"+name+".png");}
    private static void dismiss()throws Exception{UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());UiObject cancel=device.findObject(new UiSelector().resourceId("android:id/button2"));if(cancel.exists())cancel.click();else device.pressBack();device.waitForIdle();}
    @Test public void appNamesAndInstalledAdaptiveIconMatchTheRequestedIdentity()throws Exception{
        String[][] names={{"nl","BMD ScanTovenaar","ScanTovenaar"},{"en","BMD ScanMagician","ScanMagician"},{"uk","BMD СканЧарівник","СканЧарівник"},{"tr","BMD Tarama Büyücüsü","Tarama Büyücüsü"}};
        assertTrue(context().getPackageManager().getApplicationIcon("com.bmd.scanner") instanceof AdaptiveIconDrawable);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            for(String[] name:names){
                Configuration config=new Configuration(context().getResources().getConfiguration());config.setLocale(new Locale(name[0]));assertEquals(name[1],context().createConfigurationContext(config).getString(R.string.app_name));
                language(name[0]);scenario.recreate();scenario.onActivity(activity->{assertEquals(name[1],invoke(activity,"t",new Class<?>[]{String.class},"title"));assertTrue(text(activity.getWindow().getDecorView(),name[2]));});
                shot("18-home-"+name[0]);
            }
        }finally{language("nl");}
    }
    @Test public void settingsDialogsAndLauncherAreRecordedFromTheInstalledApp()throws Exception{
        language("nl");
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(activity->invoke(activity,"showSettings",new Class<?>[]{}));
            scenario.onActivity(activity->description(activity.getWindow().getDecorView(),"Taal / Language").performClick());shot("13-language-settings");dismiss();
            scenario.onActivity(activity->description(activity.getWindow().getDecorView(),"Scanner").performClick());shot("14-repeat-interval");dismiss();
            scenario.onActivity(activity->description(activity.getWindow().getDecorView(),"CSV-instellingen").performClick());shot("15-csv-filename");dismiss();
        }
        UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());device.pressHome();device.waitForIdle();device.swipe(device.getDisplayWidth()/2,device.getDisplayHeight()*9/10,device.getDisplayWidth()/2,device.getDisplayHeight()/5,35);device.waitForIdle();Thread.sleep(1000);
        String label=context().getString(R.string.app_name);UiObject installed=device.findObject(new UiSelector().text(label));
        if(installed.waitForExists(5000))shot("19-android-launcher");
    }
}
