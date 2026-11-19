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
    static void await(ActivityScenario<MainActivity> scenario,Check check,int seconds)throws Exception{long end=SystemClock.elapsedRealtime()+seconds*1000L;while(SystemClock.elapsedRealtime()<end){AtomicBoolean done=new AtomicBoolean();scenario.onActivity(a->done.set(check.test(a)));if(done.get())return;Thread.sleep(200);}scenario.onActivity(a->android.util.Log.e("BMD-test","Timeout ready="+get(a,"cameraReady")+" scanning="+get(a,"scanning")+" rows="+a.tracker.items().size()+" codes="+a.tracker.items().stream().map(i->i.value+":"+i.kind+":"+i.left+","+i.top).collect(java.util.stream.Collectors.joining(";"))));shot(scenario,"timeout-"+System.currentTimeMillis());throw new AssertionError("Timed out waiting for Android app state");}
    static void captureCameraFrame(ActivityScenario<MainActivity> scenario,String name){scenario.onActivity(a->{androidx.camera.core.ImageAnalysis analysis=(androidx.camera.core.ImageAnalysis)get(a,"analysisUseCase");java.util.concurrent.Executor executor=(java.util.concurrent.Executor)get(a,"worker");int generation=(Integer)get(a,"generation");AtomicBoolean saved=new AtomicBoolean();analysis.setAnalyzer(executor,proxy->{try{if(saved.compareAndSet(false,true)){int w=proxy.getWidth(),h=proxy.getHeight();androidx.camera.core.ImageProxy.PlaneProxy plane=proxy.getPlanes()[0];java.nio.ByteBuffer buffer=plane.getBuffer();int[] colors=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int c=buffer.get(y*plane.getRowStride()+x*plane.getPixelStride())&255;colors[y*w+x]=Color.rgb(c,c,c);}Bitmap original=Bitmap.createBitmap(colors,w,h,Bitmap.Config.ARGB_8888);Matrix matrix=new Matrix();matrix.setRotate(proxy.getImageInfo().getRotationDegrees());Bitmap upright=Bitmap.createBitmap(original,0,0,w,h,matrix,false);File folder=new File(a.getExternalFilesDir(null),"screenshots");folder.mkdirs();File file=new File(folder,name+".png");try(OutputStream out=new FileOutputStream(file)){upright.compress(Bitmap.CompressFormat.PNG,100,out);}if(upright!=original)upright.recycle();original.recycle();shell("mkdir -p /data/local/tmp/bmd-screenshots");shell("cp "+file.getAbsolutePath()+" /data/local/tmp/bmd-screenshots/");}}catch(Exception error){android.util.Log.e("BMD-test","Capture analysis image",error);}finally{call(a,"analyze",new Class<?>[]{androidx.camera.core.ImageProxy.class,int.class},proxy,generation);}});});}
    static void shot(ActivityScenario<MainActivity> scenario,String name)throws Exception{
        Thread.sleep(450);InstrumentationRegistry.getInstrumentation().waitForIdleSync();if(scenario!=null&&Build.VERSION.SDK_INT>=29){CountDownLatch committed=new CountDownLatch(1);scenario.onActivity(a->{View decor=a.getWindow().getDecorView();if(decor.isHardwareAccelerated()){decor.getViewTreeObserver().registerFrameCommitCallback(committed::countDown);decor.invalidate();}else decor.postOnAnimation(committed::countDown);});assertTrue("Current screen frame committed before screenshot",committed.await(5,TimeUnit.SECONDS));Thread.sleep(120);}Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull("Android screenshot",bitmap);File folder=new File(context().getExternalFilesDir(null),"screenshots");folder.mkdirs();try(OutputStream out=new FileOutputStream(new File(folder,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();shell("mkdir -p /data/local/tmp/bmd-screenshots");shell("cp "+new File(folder,name+".png").getAbsolutePath()+" /data/local/tmp/bmd-screenshots/");
    }
}

