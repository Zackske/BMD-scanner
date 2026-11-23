package com.bmd.scanner;
import org.junit.Test;
import static org.junit.Assert.*;
public class ScanModesTest {
    @Test public void approvedCycleMatchesDecoderModes(){assertEquals("bar",ScanModes.next("qr"));assertEquals("both",ScanModes.next("bar"));assertEquals("qr",ScanModes.next("both"));}
    @Test public void settingsSelectionAndSavedModeAgree(){for(int i=0;i<3;i++){assertEquals(i,ScanModes.index(ScanModes.ORDER[i]));assertEquals(ScanModes.ORDER[i],ScanModes.normalize(ScanModes.ORDER[i]));}assertEquals("both",ScanModes.normalize(null));assertEquals("both",ScanModes.normalize("off"));}
}

