package com.bmd.scanner;
import android.os.SystemClock;
import android.view.*;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class PinchCameraZoomTest {
    private static MotionEvent event(long start,int offset,int action,float span,int count){MotionEvent.PointerProperties[] props=new MotionEvent.PointerProperties[count];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[count];for(int n=0;n<count;n++){props[n]=new MotionEvent.PointerProperties();props[n].id=n;props[n].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[n]=new MotionEvent.PointerCoords();coords[n].x=n==0?360-span/2:360+span/2;coords[n].y=400;coords[n].pressure=1;coords[n].size=1;}return MotionEvent.obtain(start,start+offset,action,count,props,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);}
    private static void gesture(View view,float from,float to){long now=SystemClock.uptimeMillis();for(int n=0;n<14;n++){int action=n==0?MotionEvent.ACTION_DOWN:n==1?MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT):n==12?MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT):n==13?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;int count=n==0||n==13?1:2;float fraction=n<2?0:n>11?1:(n-1)/10f;MotionEvent e=event(now,n*40,action,from+(to-from)*fraction,count);try{assertTrue("Preview must retain the initial touch so the second finger reaches it",view.dispatchTouchEvent(e));}finally{e.recycle();}}}
    @Test public void nativeTwoFingerGesturesDriveZoomAndTemporaryAnimatedReadout(){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{View preview=new View(DeviceSupport.context());preview.layout(0,0,720,1000);TextView hud=new TextView(DeviceSupport.context());List<Float> requests=new ArrayList<>();PinchCameraZoom zoom=new PinchCameraZoom(DeviceSupport.context(),preview,hud,new PinchCameraZoom.Listener(){public boolean ready(){return true;}public void begin(){}public void change(float n){requests.add(n);}public void end(){}});zoom.configure(.5f,3,1,new Locale("nl"));gesture(preview,240,680);assertFalse("Spreading fingers must request real zoom changes",requests.isEmpty());float larger=requests.get(requests.size()-1);assertTrue(larger>1&&larger<=3);assertEquals(View.VISIBLE,hud.getVisibility());assertTrue(hud.getText().toString().contains("×"));requests.clear();gesture(preview,680,180);assertFalse(requests.isEmpty());assertTrue(requests.get(requests.size()-1)<larger);zoom.dispose();assertEquals(View.GONE,hud.getVisibility());});}
}
