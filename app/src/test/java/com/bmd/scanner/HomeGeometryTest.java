package com.bmd.scanner;
import org.junit.Test;
import static org.junit.Assert.*;
public class HomeGeometryTest {
    @Test public void portraitPagesFillBothEdgesWithoutTopOrBottomBands(){
        for(int[] size:new int[][]{{720,1280},{720,1600},{1080,1920},{1080,2340},{1080,2400},{1440,3120}}){
            HomeGeometry g=new HomeGeometry(size[0],size[1]);
            assertEquals(0,g.left,.01f);assertEquals(size[0],g.right,.01f);
            assertEquals(0,g.y(0),.01f);assertEquals(size[1],g.y(1812),.01f);
            // Header, both tile rows and logo retain the same horizontal/vertical scale.
            assertEquals(205*g.scale,g.y(245)-g.y(40),.01f);
            assertEquals(422*g.scale,g.y(899)-g.y(477),.01f);
            assertEquals(422*g.scale,g.y(1331)-g.y(909),.01f);
            assertEquals(158*g.scale,g.y(1648)-g.y(1490),.01f);
            for(int i=0;i<7;i++)assertTrue(g.rows[i+1]>g.rows[i]);
        }
    }
    @Test public void approvedReferenceSizeReproducesExactVerticalPositions(){HomeGeometry g=new HomeGeometry(868,1812);assertEquals(1,g.scale,.001f);for(int y:HomeGeometry.EDGES)assertEquals(y,g.y(y),.001f);}
    @Test public void shortScreensKeepEverySectionVisible(){HomeGeometry g=new HomeGeometry(1600,720);assertEquals(0,g.y(0),.01f);assertEquals(720,g.y(1812),.01f);assertTrue(g.left>=0&&g.right<=1600);}
}

