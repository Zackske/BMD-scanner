package com.bmd.scanner;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PhysicalCodeReaderTest {
    @Test public void equalPayloadsOnSixPhysicalLabelsRemainSix()throws Exception{
        int w=1600,h=1200;byte[] pixels=new byte[w*h];Arrays.fill(pixels,(byte)255);
        for(int n=0;n<6;n++){boolean qr=n%2==0;BitMatrix m=new MultiFormatWriter().encode(qr?"BMD-000123":"00123456",qr?BarcodeFormat.QR_CODE:BarcodeFormat.CODE_128,qr?210:350,qr?210:160);int ox=350+(n%2)*450+(450-m.getWidth())/2,oy=185+(n/2)*300;for(int y=0;y<m.getHeight();y++)for(int x=0;x<m.getWidth();x++)pixels[(oy+y)*w+ox+x]=(byte)(m.get(x,y)?0:255);}
        for(String mode:new String[]{"both","qr","bar"}){List<ScanTracker.Detection> codes=PhysicalCodeReader.decode(pixels,w,h,mode);assertEquals(mode,mode.equals("both")?6:3,codes.size());assertEquals(mode.equals("bar")?0:3,codes.stream().filter(d->d.kind.equals("QR")).count());for(ScanTracker.Detection d:codes){assertTrue(d.right>d.left);assertTrue(d.bottom>d.top);}}
    }
}
