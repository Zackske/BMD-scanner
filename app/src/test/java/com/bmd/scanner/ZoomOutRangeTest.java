package com.bmd.scanner;
import org.junit.Test;
import static org.junit.Assert.*;
public class ZoomOutRangeTest {
    @Test public void ultrawideUsesRealQuarterStopsAndNeverZoomsPastOne(){ZoomOutRange r=new ZoomOutRange(.5f,10);assertArrayEquals(new float[]{.5f,.75f,1},r.stops,.001f);assertEquals(.75f,r.step(1,-1),.001f);assertEquals(.5f,r.step(.75f,-1),.001f);assertEquals(.5f,r.step(.5f,-1),.001f);assertEquals(1,r.step(1,1),.001f);assertEquals(1,r.clamp(3),.001f);}
    @Test public void cameraWithoutUltrawideCannotAdvertisePointFive(){ZoomOutRange r=new ZoomOutRange(1,8);assertFalse(r.available());assertArrayEquals(new float[]{1},r.stops,.001f);assertEquals(1,r.clamp(.5f),.001f);}
    @Test public void nonstandardHardwareMinimumIsRespected(){ZoomOutRange r=new ZoomOutRange(.8f,5);assertArrayEquals(new float[]{.8f,1},r.stops,.001f);assertEquals(.8f,r.clamp(.5f),.001f);assertEquals(.8f,r.step(1,-1),.001f);}
    @Test public void corruptHardwareBoundsDoNotCreateFakeZoom(){ZoomOutRange r=new ZoomOutRange(Float.NaN,8);assertFalse(r.available());assertEquals(1,r.clamp(Float.NaN),.001f);}
}
