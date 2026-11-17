package com.bmd.scanner;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
final class DeviceSupport {
    static Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    static void clear(){context().getSharedPreferences("MainActivity",0).edit().clear().putString("language","nl").commit();}
    static void permission()throws Exception{shell("pm grant com.bmd.scanner android.permission.CAMERA");}
    static String shell(String command)throws Exception{try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command))){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))>0)out.write(b,0,n);return out.toString("UTF-8");}}
    static Object get(Object object,String field){try{Field f=object.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new AssertionError(e);}}
    static void set(Object object,String field,Object value){try{Field f=object.getClass().getDeclaredField(field);f.setAccessible(true);f.set(object,value);}catch(Exception e){throw new AssertionError(e);}}
    static Object call(Object object,String name,Class<?>[] types,Object...args){try{Method m=object.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(object,args);}catch(Exception e){throw new AssertionError(e);}}
    static View tag(View v,String tag){if(tag.equals(v.getTag()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=tag(((ViewGroup)v).getChildAt(i),tag);if(found!=null)return found;}return null;}
    static boolean hasText(View v,String value){if(v instanceof android.widget.TextView&&value.contentEquals(((android.widget.TextView)v).getText()))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(hasText(((ViewGroup)v).getChildAt(i),value))return true;return false;}
    interface Check{boolean test(MainActivity a);}
    static void await(ActivityScenario<MainActivity> scenario,Check check,int seconds)throws Exception{long end=SystemClock.elapsedRealtime()+seconds*1000L;while(SystemClock.elapsedRealtime()<end){AtomicBoolean done=new AtomicBoolean();scenario.onActivity(a->done.set(check.test(a)));if(done.get())return;Thread.sleep(200);}throw new AssertionError("Timed out waiting for Android app state");}
    static void shot(ActivityScenario<MainActivity> scenario,String name)throws Exception{CountDownLatch ready=new CountDownLatch(1);AtomicInteger result=new AtomicInteger(-1);Bitmap[] bitmap=new Bitmap[1];scenario.onActivity(a->{View decor=a.getWindow().getDecorView();decor.invalidate();decor.postOnAnimation(()->decor.postOnAnimation(()->{bitmap[0]=Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),Bitmap.Config.ARGB_8888);PixelCopy.request(a.getWindow(),bitmap[0],status->{result.set(status);ready.countDown();},new Handler(Looper.getMainLooper()));}));});assertTrue(ready.await(10,TimeUnit.SECONDS));assertEquals(PixelCopy.SUCCESS,result.get());File folder=new File(context().getExternalFilesDir(null),"screenshots");folder.mkdirs();try(OutputStream out=new FileOutputStream(new File(folder,name+".png"))){bitmap[0].compress(Bitmap.CompressFormat.PNG,100,out);}bitmap[0].recycle();}
}
