package com.bmd.scanner;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ControlCodeLinksTest {
    @Test public void webLinksNeverBecomePhysicalProductsAndEqualLabelsRemainSeparate(){
        ScanTracker.Detection one=new ScanTracker.Detection("BMD-2610143","QR",.1f,.1f,.2f,.2f),two=new ScanTracker.Detection("BMD-2610143","QR",.1f,.4f,.2f,.5f),bar=new ScanTracker.Detection("BMD-2610143","Barcode",.5f,.1f,.8f,.2f),link=new ScanTracker.Detection("https://example.org/product","QR",.6f,.5f,.8f,.7f);
        List<ScanTracker.Detection> all=Arrays.asList(one,two,bar,link),products=ControlCodeLinks.products(all);
        assertEquals(Arrays.asList(one,two,bar),products);assertEquals(Collections.singletonList(link),ControlCodeLinks.links(all));assertEquals(4,all.size());
        ScanTracker tracker=new ScanTracker();tracker.update(products,1000,1);assertEquals(3,tracker.items().size());
    }
    @Test public void photoModeFiltersAndRegistrationMarkersStayIntact(){
        ScanTracker.Detection qr=new ScanTracker.Detection("BMD-A","QR",0,0,.2f,.2f),bar=new ScanTracker.Detection("BMD-B","Barcode",.3f,0,.6f,.2f),marker=new ScanTracker.Detection("BMD-BOX-A","QR",0,.4f,.2f,.6f),url=new ScanTracker.Detection("https://example.org/","QR",.5f,.4f,.7f,.6f);List<ScanTracker.Detection> all=Arrays.asList(qr,bar,marker,url);
        assertEquals(Arrays.asList(qr,marker),BoxSession.select(all,"qr"));assertEquals(Arrays.asList(bar,marker),BoxSession.select(all,"bar"));assertEquals(Arrays.asList(qr,bar,marker),BoxSession.select(all,"both"));
    }
    @Test public void onlyExplicitBrowserUrlsReceiveWebActions(){assertTrue(ControlCodeLinks.isLink("https://example.org/"));assertTrue(ControlCodeLinks.isLink("http://example.org/"));assertTrue(ControlCodeLinks.isLink("  HTTPS://example.org/product  "));assertFalse(ControlCodeLinks.isLink("https://user:password@example.org/"));assertFalse(ControlCodeLinks.isLink("javascript:alert(1)"));assertFalse(ControlCodeLinks.isLink("BMD-BOX-A"));assertFalse(ControlCodeLinks.isLink(null));assertFalse(ControlCodeLinks.isLink(""));}
}
