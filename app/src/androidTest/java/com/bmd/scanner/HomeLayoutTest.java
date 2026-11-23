package com.bmd.scanner;

import android.content.Context;
import android.graphics.Rect;
import android.view.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HomeLayoutTest {
    private static View tagged(View view,String tag){
        if(tag.equals(view.getTag()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=tagged(((ViewGroup)view).getChildAt(i),tag);if(found!=null)return found;}
        return null;
    }
    private static Rect bounds(View view){int[] xy=new int[2];view.getLocationOnScreen(xy);return new Rect(xy[0],xy[1],xy[0]+view.getWidth(),xy[1]+view.getHeight());}
    @Test public void identityTilesAndFooterAreBalancedAndDoNotOverlap()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("MainActivity",0).edit().putString("language","nl").commit();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(700);
            scenario.onActivity(activity->{
                View root=activity.getWindow().getDecorView();
                Rect hero=bounds(tagged(root,"home-identity")),scan=bounds(tagged(root,"qr")),photo=bounds(tagged(root,"photo")),results=bounds(tagged(root,"list")),settings=bounds(tagged(root,"settings")),brand=bounds(tagged(root,"home-brand"));
                assertTrue(hero.bottom<scan.top);assertTrue(scan.bottom<results.top);assertTrue(results.bottom<brand.top);
                assertEquals(scan.width(),photo.width(),1);assertEquals(scan.height(),results.height(),1);
                assertEquals(results.top,settings.top);assertEquals(scan.top,photo.top);
                assertEquals((scan.left+photo.right)/2f,brand.exactCenterX(),2f);
                assertTrue("Photo button must be actionable",tagged(root,"photo").isClickable());
                assertTrue("Results button must be actionable",tagged(root,"list").isClickable());
                assertTrue("Brand must have visible dimensions",brand.width()>100&&brand.height()>40);
                assertTrue("Brand must stay within viewport",brand.bottom<root.getHeight());

            });
            java.util.concurrent.CountDownLatch ready=new java.util.concurrent.CountDownLatch(1);
            android.graphics.Bitmap[] captured=new android.graphics.Bitmap[1];int[] status={-1};
            scenario.onActivity(activity->{
                View decor=activity.getWindow().getDecorView();decor.invalidate();
                decor.postOnAnimation(()->decor.postOnAnimation(()->{
                    captured[0]=android.graphics.Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
                    android.view.PixelCopy.request(activity.getWindow(),captured[0],result->{status[0]=result;ready.countDown();},new android.os.Handler(android.os.Looper.getMainLooper()));
                }));
            });
            assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS));assertEquals(android.view.PixelCopy.SUCCESS,status[0]);
            scenario.onActivity(activity->{
                View footer=tagged(activity.getWindow().getDecorView(),"home-brand");Rect rect=bounds(footer);int[] origin=new int[2];activity.getWindow().getDecorView().getLocationOnScreen(origin);rect.offset(-origin[0],-origin[1]);int bright=0;
                for(int y=rect.top;y<rect.bottom;y+=5)for(int x=rect.left;x<rect.right;x+=5){int color=captured[0].getPixel(x,y);if(android.graphics.Color.red(color)>120&&android.graphics.Color.green(color)>120)bright++;}
                assertTrue("Footer must be visible in the actual displayed app window",bright>100);
            });
            java.io.File dir=new java.io.File(context.getExternalFilesDir(null),"screenshots");dir.mkdirs();
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,"20-home-verified.png"))){captured[0].compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}captured[0].recycle();
        }
    }
}
