package com.bmd.scanner;
import org.junit.Test;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import static org.junit.Assert.*;
public class CameraFixtureTest {
    @Test public void createActualCodeCameraInput()throws Exception{
        BufferedImage image=new BufferedImage(1600,1200,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,1600,1200);g.setColor(new Color(0,30,60));g.setFont(new Font("SansSerif",Font.BOLD,24));
        for(int n=0;n<6;n++){int x=350+(n%2)*450,y=150+(n/2)*300;boolean qr=n%2==0;String value=qr?"BMD-000123":new String[]{"123456","234567","345678"}[n/2];BitMatrix matrix=new MultiFormatWriter().encode(value,qr?BarcodeFormat.QR_CODE:BarcodeFormat.CODE_128,qr?210:350,qr?210:160);int ox=x+(450-matrix.getWidth())/2,oy=y+35;g.setColor(new Color(0,30,60));g.drawString("BMD "+(n+1),x+155,y+22);for(int py=0;py<matrix.getHeight();py++)for(int px=0;px<matrix.getWidth();px++)image.setRGB(ox+px,oy+py,matrix.get(px,py)?Color.BLACK.getRGB():Color.WHITE.getRGB());g.drawString(value,x+105,y+280);}
        g.dispose();String workspace=System.getenv("GITHUB_WORKSPACE");File file=workspace==null?new File("../camera-fixture.png"):new File(workspace,"camera-fixture.png");assertTrue(ImageIO.write(image,"png",file));assertTrue(file.length()>1000);
    }
}
