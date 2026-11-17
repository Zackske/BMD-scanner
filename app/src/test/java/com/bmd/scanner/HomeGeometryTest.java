package com.bmd.scanner;
import org.junit.Test;
import static org.junit.Assert.*;
public class HomeGeometryTest {
    @Test public void portraitPagesFillBothEdgesWithoutTopOrBottomBands(){
        for(int[] size:new int[][]{{720,1280},{720,1600},{1080,1920},{1080,2340},{1080,2400},{1440,3120}}){
            HomeGeometry g=new HomeGeometry(size[0],size[1]);
            assertEquals(0,g.left,.01f);assertEquals(size[0],g.right,.01f);
            assertEquals(0,g.y(106),.01f);assertEquals(size[1],g.y(1564),.01f);
            // Header, both tile rows and logo retain the same horizontal/vertical scale.
            assertEquals(216*g.scale,g.y(380)-g.y(164),.01f);
            assertEquals(358*g.scale,g.y(828)-g.y(470),.01f);
            assertEquals(358*g.scale,g.y(1199)-g.y(841),.01f);
            assertEquals(153*g.scale,g.y(1408)-g.y(1255),.01f);
            for(int i=0;i<7;i++)assertTrue(g.rows[i+1]>g.rows[i]);
        }
    }
    @Test public void shortScreensKeepEverySectionVisible(){HomeGeometry g=new HomeGeometry(1600,720);assertEquals(0,g.y(106),.01f);assertEquals(720,g.y(1564),.01f);assertTrue(g.left>=0&&g.right<=1600);}
}

