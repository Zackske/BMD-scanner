package com.bmd.scanner;
import org.junit.Test;
import static org.junit.Assert.*;

public class CameraResolutionOptionsTest {
    @Test public void allTenRequestedPortraitSizesArePresentWithoutChangingOlderSelections(){
        int[][] requested={{720,1600},{720,1612},{1080,1920},{1080,2340},{1080,2400},{1080,2424},{1220,2712},{1280,2856},{1440,3120},{1440,3200}};
        assertEquals(13,CameraResolutionOptions.TARGETS.length);
        assertEquals(1280,CameraResolutionOptions.TARGETS[0].width);assertEquals(1920,CameraResolutionOptions.TARGETS[1].width);assertNull(CameraResolutionOptions.TARGETS[2]);
        for(int i=0;i<requested.length;i++){CameraResolutionOptions.Target target=CameraResolutionOptions.TARGETS[i+3];assertEquals(requested[i][0],target.width);assertEquals(requested[i][1],target.height);assertEquals(requested[i][1],target.longEdge());}
    }
    @Test public void orientationDoesNotChangeResolutionAndClosestNativeSizeWins(){
        CameraResolutionOptions.Target fhd=CameraResolutionOptions.TARGETS[5];assertEquals(0,CameraResolutionOptions.distance(1920,1080,fhd),.00001);
        CameraResolutionOptions.Target custom=CameraResolutionOptions.TARGETS[7];assertTrue(CameraResolutionOptions.distance(1920,1080,custom)<CameraResolutionOptions.distance(640,480,custom));
        assertEquals(0,CameraResolutionOptions.distance(2400,1080,custom),.00001);
    }
}
