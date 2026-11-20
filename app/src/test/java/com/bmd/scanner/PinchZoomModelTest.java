package com.bmd.scanner;
import org.junit.Test;
import static org.junit.Assert.*;
public class PinchZoomModelTest {
    @Test public void spreadsAndPinchesContinuouslyWithinRealCameraBounds(){PinchZoomModel m=new PinchZoomModel();m.configure(.5f,8,1);assertEquals(1.5f,m.scale(1.5f),.001f);assertEquals(.75f,m.scale(.5f),.001f);assertEquals(.5f,m.scale(.01f),.001f);assertEquals(8,m.scale(100),.001f);}
    @Test public void fixedCameraCannotFakeZoom(){PinchZoomModel m=new PinchZoomModel();m.configure(1,1,1);assertFalse(m.available());assertEquals(1,m.scale(3),.001f);assertEquals(1,m.scale(.5f),.001f);}
    @Test public void invalidGestureFactorDoesNotChangeZoom(){PinchZoomModel m=new PinchZoomModel();m.configure(.5f,3,1);assertEquals(1,m.scale(Float.NaN),.001f);assertEquals(1,m.scale(-2),.001f);assertEquals(1,m.scale(Float.POSITIVE_INFINITY),.001f);}
}
