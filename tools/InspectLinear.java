package com.bmd.scanner;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
public class InspectLinear {
 public static void main(String[] args)throws Exception{File[] files=new File("test-data/real-box").listFiles((d,n)->n.endsWith(".jpg"));Arrays.sort(files);for(File f:files){BufferedImage image=ImageIO.read(f);int w=image.getWidth(),h=image.getHeight();byte[] gray=new byte[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int c=image.getRGB(x,y);gray[y*w+x]=(byte)((((c>>16)&255)*306+((c>>8)&255)*601+(c&255)*117)>>10);}List<ScanTracker.Detection> codes=PhysicalCodeReader.decode(gray,w,h,"bar");System.out.println("LINEAR "+f.getName()+" count="+codes.size());for(ScanTracker.Detection d:codes)System.out.println("RAW="+d.value+" box="+d.left+","+d.top+","+d.right+","+d.bottom);}}
}

