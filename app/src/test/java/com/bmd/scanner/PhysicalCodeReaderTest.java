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
        for(int turn=0;turn<2;turn++){if(turn==1){byte[] rotated=new byte[pixels.length];for(int y=0;y<h;y++)for(int x=0;x<w;x++)rotated[x*h+(h-1-y)]=pixels[y*w+x];pixels=rotated;int old=w;w=h;h=old;}for(String mode:new String[]{"both","qr","bar"}){List<ScanTracker.Detection> codes=PhysicalCodeReader.decode(pixels,w,h,mode);assertEquals(mode,mode.equals("both")?6:3,codes.size());assertEquals(mode.equals("bar")?0:3,codes.stream().filter(d->d.kind.equals("QR")).count());for(ScanTracker.Detection d:codes){assertTrue(d.right>d.left);assertTrue(d.bottom>d.top);}}
        }
    }
    @Test public void unrelatedMatrixFormatsAreIgnored()throws Exception{
        for(BarcodeFormat format:new BarcodeFormat[]{BarcodeFormat.DATA_MATRIX,BarcodeFormat.AZTEC,BarcodeFormat.PDF_417}){
            BitMatrix matrix=new MultiFormatWriter().encode("BMD-12345678",format,320,240);
            int w=400,h=320;byte[] image=new byte[w*h];Arrays.fill(image,(byte)255);
            for(int y=0;y<matrix.getHeight();y++)for(int x=0;x<matrix.getWidth();x++)image[(y+40)*w+x+40]=(byte)(matrix.get(x,y)?0:255);
            for(String mode:new String[]{"both","qr","bar"})assertTrue(format+" "+mode,PhysicalCodeReader.decode(image,w,h,mode).isEmpty());
        }
    }
    @Test public void sixteenIdenticalBarcodesOnSeparateProductsAreNotCollapsed()throws Exception{
        int w=1200,h=1200;byte[] image=new byte[w*h];Arrays.fill(image,(byte)255);BitMatrix m=new MultiFormatWriter().encode("00123456",BarcodeFormat.CODE_128,240,120);
        for(int n=0;n<16;n++){int ox=(n%4)*300+30,oy=(n/4)*300+70;for(int y=0;y<m.getHeight();y++)for(int x=0;x<m.getWidth();x++)image[(oy+y)*w+ox+x]=(byte)(m.get(x,y)?0:255);}
        assertEquals(16,PhysicalCodeReader.decode(image,w,h,"bar").size());
    }
}
