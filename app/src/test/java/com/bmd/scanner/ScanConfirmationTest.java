package com.bmd.scanner;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ScanConfirmationTest {
    @Test public void lowSensitivityNeedsConsecutiveFramesForEachIndividualCode(){
        ScanConfirmation gate=new ScanConfirmation();
        assertTrue(gate.acceptFrame(Arrays.asList("A","B","A"),0,3,1500).isEmpty());
        assertTrue(gate.acceptFrame(Arrays.asList("A","B"),100,3,1500).isEmpty());
        assertEquals(Arrays.asList("A","B"),gate.acceptFrame(Arrays.asList("A","B"),200,3,1500));
    }
    @Test public void missingFramesAndLongPausesResetConfirmation(){
        ScanConfirmation gate=new ScanConfirmation();
        gate.acceptFrame(Arrays.asList("A"),0,2,1500);gate.acceptFrame(Collections.emptyList(),100,2,1500);
        assertTrue(gate.acceptFrame(Arrays.asList("A"),200,2,1500).isEmpty());
        assertTrue(gate.acceptFrame(Arrays.asList("A"),2000,2,1500).isEmpty());
        assertEquals(Arrays.asList("A"),gate.acceptFrame(Arrays.asList("A"),2100,2,1500));
    }
    @Test public void cooldownSuppressesDuplicateAddsUntilItExpires(){
        ScanConfirmation gate=new ScanConfirmation();
        assertEquals(Arrays.asList("A"),gate.acceptFrame(Arrays.asList("A","A"),0,1,1500));
        assertTrue(gate.acceptFrame(Arrays.asList("A"),100,1,1500).isEmpty());
        assertEquals(Arrays.asList("B"),gate.acceptFrame(Arrays.asList("A","B"),200,1,1500));
        assertEquals(Arrays.asList("A"),gate.acceptFrame(Arrays.asList("A"),1500,1,1500));
    }
}
