package com.bmd.scanner;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class BoxSessionTest {
    static final double[] I={1,0,0,0,1,0,0,0,1};
    private ScanTracker.Detection code(float x,float y){return new ScanTracker.Detection("(01)08720634200066(10)2610143","Data Matrix",x,y,x+.02f,y+.02f);}
    @Test public void samePhysicalCodeInOverlappingPhotosCountsOnceButAdjacentIdenticalProductsDoNotMerge(){BoxSession s=new BoxSession(2);s.add("a",Arrays.asList(code(.1f,.1f),code(.1f,.14f)),I);s.add("b",Arrays.asList(code(.101f,.101f),code(.101f,.141f)),I);assertEquals(2,s.tracker.items().size());assertEquals(2,s.shots.get(1).ids.size());assertFalse(s.ready());}
    @Test public void transformedDetailMapsToTheOverview(){BoxSession s=new BoxSession(1);s.add("a",Arrays.asList(code(.2f,.2f)),I);s.add("b",Arrays.asList(new ScanTracker.Detection("(01)08720634200066(10)2610143","Data Matrix",.4f,.4f,.44f,.44f)),new double[]{.5,0,0,0,.5,0,0,0,1});assertEquals(1,s.tracker.items().size());}
    @Test public void expectedCountNeverFillsMissingDetectionsAndChangesAreRetained(){BoxSession s=new BoxSession(95);s.add("a",Collections.emptyList(),I);assertEquals(0,s.tracker.items().size());s.setExpected(96);assertEquals(Arrays.asList(95,96),s.expectedHistory);assertFalse(s.ready());}
    @Test public void markerCodesNeverCountAsProducts(){BoxSession s=new BoxSession(1);s.add("a",Arrays.asList(new ScanTracker.Detection("BMD-BOX-A","QR",.1f,.1f,.2f,.2f)),I);assertEquals(0,s.tracker.items().size());}
    @Test public void invalidPhotoCannotPartiallyMutateSession(){BoxSession s=new BoxSession(1);try{s.add("a",Arrays.asList(code(.1f,.1f),code(1.2f,.1f)),I);fail();}catch(IllegalArgumentException expected){}assertTrue(s.tracker.items().isEmpty());assertTrue(s.shots.isEmpty());}
    @Test public void equalCountStillRequiresReviewAndFourPhotos(){BoxSession s=new BoxSession(1);s.add("a",Arrays.asList(code(.1f,.1f)),I);s.reviewed=true;assertFalse(s.ready());for(int n=0;n<3;n++)s.add("x"+n,Arrays.asList(code(.1f,.1f)),I);assertFalse(s.ready());s.reviewed=true;assertTrue(s.ready());}
    @Test public void manuallyExcludedCodeDoesNotReappearWhenAnotherPhotoSeesIt(){BoxSession s=new BoxSession(1);s.add("a",Arrays.asList(code(.1f,.1f)),I);s.tracker.remove(s.tracker.items().get(0).id);s.add("b",Arrays.asList(code(.101f,.101f)),I);assertTrue(s.tracker.items().isEmpty());assertEquals(1,s.tracker.all().size());assertFalse(s.ready());}

    @Test public void photoModeFiltersProductsButRetainsCalibrationMarkers(){List<ScanTracker.Detection> all=Arrays.asList(new ScanTracker.Detection("qr","QR",.1f,.1f,.2f,.2f),new ScanTracker.Detection("dm","Data Matrix",.3f,.1f,.4f,.2f),new ScanTracker.Detection("bar","Barcode",.5f,.1f,.6f,.2f),new ScanTracker.Detection("BMD-BOX-A","QR",.7f,.1f,.8f,.2f));assertEquals(4,BoxSession.select(all,"both").size());assertEquals(3,BoxSession.select(all,"qr").size());List<ScanTracker.Detection> bar=BoxSession.select(all,"bar");assertEquals(2,bar.size());assertEquals("Barcode",bar.get(0).kind);assertTrue(BoxSession.marker(bar.get(1).value));}

}
