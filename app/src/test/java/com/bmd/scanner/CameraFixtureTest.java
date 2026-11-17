package com.bmd.scanner;
import org.junit.Test;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import java.io.*;
import java.util.Arrays;
import java.util.zip.*;
import static org.junit.Assert.*;
public class CameraFixtureTest {
    private static void chunk(DataOutputStream out,String type,byte[] bytes)throws IOException{byte[] name=type.getBytes("US-ASCII");out.writeInt(bytes.length);out.write(name);out.write(bytes);CRC32 crc=new CRC32();crc.update(name);crc.update(bytes);out.writeInt((int)crc.getValue());}
    @Test public void createActualCodeCameraInput()throws Exception{
        int width=1024,height=1024;byte[] pixels=new byte[width*height];Arrays.fill(pixels,(byte)255);
        for(int n=0;n<6;n++){int x=218+(n%2)*256,y=267+(n/2)*170;boolean qr=n%2==0;String value=qr?"BMD-000123":new String[]{"123456","234567","345678"}[n/2];BitMatrix matrix=new MultiFormatWriter().encode(value,qr?BarcodeFormat.QR_CODE:BarcodeFormat.CODE_128,qr?150:300,qr?150:110);int ox=x+(256-matrix.getWidth())/2,oy=y;for(int py=0;py<matrix.getHeight();py++)for(int px=0;px<matrix.getWidth();px++)pixels[(oy+py)*width+ox+px]=(byte)(matrix.get(px,py)?0:255);}
        assertEquals(6,PhysicalCodeReader.decode(pixels,width,height,"both").size());
        String workspace=System.getenv("GITHUB_WORKSPACE");File file=workspace==null?new File("../camera-fixture.png"):new File(workspace,"camera-fixture.png");
        try(DataOutputStream out=new DataOutputStream(new FileOutputStream(file))){out.writeLong(0x89504e470d0a1a0aL);ByteArrayOutputStream header=new ByteArrayOutputStream();DataOutputStream h=new DataOutputStream(header);h.writeInt(width);h.writeInt(height);h.write(new byte[]{8,2,0,0,0});chunk(out,"IHDR",header.toByteArray());ByteArrayOutputStream compressed=new ByteArrayOutputStream();try(DeflaterOutputStream deflate=new DeflaterOutputStream(compressed)){for(int y=0;y<height;y++){deflate.write(0);for(int x=0;x<width;x++){int pixel=pixels[y*width+x]&255;deflate.write(pixel);deflate.write(pixel);deflate.write(pixel);}}}chunk(out,"IDAT",compressed.toByteArray());chunk(out,"IEND",new byte[0]);}assertTrue(file.length()>1000);
    }
}
