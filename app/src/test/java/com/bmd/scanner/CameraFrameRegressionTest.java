package com.bmd.scanner;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import static org.junit.Assert.*;
public class CameraFrameRegressionTest {
    @Test public void actualResampledCameraFrameContainsThreeQrAndThreeBarcodes()throws Exception{
        byte[] gray;try(InputStream input=new GZIPInputStream(getClass().getResourceAsStream("/camera-frame.gray.gz"))){gray=input.readAllBytes();}assertEquals(720*1280,gray.length);
        List<ScanTracker.Detection> codes=PhysicalCodeReader.decode(gray,720,1280,"both");assertEquals(codes.stream().map(d->d.kind+":"+d.value).collect(java.util.stream.Collectors.joining(",")),6,codes.size());assertEquals(3,codes.stream().filter(d->d.kind.equals("QR")).count());assertEquals(3,PhysicalCodeReader.decode(gray,720,1280,"bar").size());
    }
}
