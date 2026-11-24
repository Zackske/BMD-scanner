package com.bmd.scanner;

import org.junit.Test;
import static org.junit.Assert.*;

public class CodeFocusPointTest {
    @Test public void fullFrameFocusTracksAllFourRotations(){
        assertArrayEquals(new float[]{.25f,.3f},CodeFocusPoint.map(200,180,0,0,0,800,600,800,600),.0001f);
        assertArrayEquals(new float[]{.25f,.3f},CodeFocusPoint.map(419,200,90,0,0,800,600,800,600),.0001f);
        assertArrayEquals(new float[]{.25f,.3f},CodeFocusPoint.map(599,419,180,0,0,800,600,800,600),.0001f);
        assertArrayEquals(new float[]{.25f,.3f},CodeFocusPoint.map(180,599,270,0,0,800,600,800,600),.0001f);
    }
    @Test public void viewportCropIsAddedBackBeforeMetering(){assertArrayEquals(new float[]{.3f,.4f},CodeFocusPoint.map(239,200,90,100,80,600,400,1000,600),.0001f);}
    @Test public void invalidAndOffImageLocationsDoNotFocus(){
        assertNull(CodeFocusPoint.map(Float.NaN,10,0,0,0,800,600,800,600));
        assertNull(CodeFocusPoint.map(800,10,0,0,0,800,600,800,600));
        assertNull(CodeFocusPoint.map(10,10,45,0,0,800,600,800,600));
        assertNull(CodeFocusPoint.map(10,10,0,900,0,800,600,800,600));
    }
}
