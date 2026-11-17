package com.bmd.scanner;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ScanTrackerTest {
    private ScanTracker.Detection d(String value,float x){return new ScanTracker.Detection(value,"QR",x,.2f,x+.1f,.3f);}
    @Test public void identicalPayloadsCountEachPhysicalLabelOnceAcrossReorderedFrames(){ScanTracker t=new ScanTracker();List<ScanTracker.Detection> a=Arrays.asList(d("SAME",.1f),d("SAME",.4f),d("SAME",.7f));assertEquals(0,t.update(a,100,2));assertEquals(3,t.update(a,200,2));assertEquals(0,t.update(Arrays.asList(a.get(2),a.get(0),a.get(1)),300,2));assertEquals(3,t.items().size());}
    @Test public void correctionsAndDeletedTracksDoNotChangeOtherDuplicatesOrReappear(){ScanTracker t=new ScanTracker();List<ScanTracker.Detection> a=Arrays.asList(d("SAME",.1f),d("SAME",.5f));t.update(a,100,1);t.edit(1,"CORRECTED");t.remove(2);assertEquals(0,t.update(a,200,1));assertEquals(1,t.items().size());assertEquals("CORRECTED",t.items().get(0).value);ScanTracker restored=new ScanTracker();restored.restore(t.all());restored.update(a,300,1);assertEquals(1,restored.items().size());}
    @Test public void zoomTransformsTracksAndResetStartsNewSession(){ScanTracker t=new ScanTracker();t.update(Arrays.asList(d("A",.4f)),100,1);t.rescale(2);assertEquals(0,t.update(Arrays.asList(new ScanTracker.Detection("A","QR",.3f,-.1f,.5f,.1f)),200,1));assertEquals(1,t.items().size());t.reset();assertEquals(0,t.items().size());assertEquals(1,t.update(Arrays.asList(d("A",.1f)),300,1));}
    @Test public void briefOcclusionAndCameraTranslationPreserveIds(){ScanTracker t=new ScanTracker();List<ScanTracker.Detection> a=Arrays.asList(d("DUP",.1f),d("DUP",.4f),d("ANCHOR",.7f));t.update(a,100,1);t.update(Collections.emptyList(),200,1);assertEquals(0,t.update(Arrays.asList(d("DUP",.2f),d("DUP",.5f),d("ANCHOR",.8f)),300,1));assertEquals(3,t.items().size());}
    @Test public void confirmationUsesConsecutiveProcessedFramesOnSlowDevices(){ScanTracker t=new ScanTracker();List<ScanTracker.Detection> codes=Arrays.asList(d("SAME",.1f),d("SAME",.4f),d("SAME",.7f));assertEquals(0,t.update(codes,100,2));assertEquals(3,t.update(codes,3500,2));assertEquals(3,t.items().size());t.reset();t.update(codes,100,2);t.update(Collections.emptyList(),3000,2);assertEquals(0,t.update(codes,6000,2));assertEquals(3,t.update(codes,9000,2));}
}
