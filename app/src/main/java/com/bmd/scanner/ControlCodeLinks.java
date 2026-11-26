package com.bmd.scanner;

import java.util.*;

/** Website payloads can be inspected, but cannot inflate a physical-product count. */
final class ControlCodeLinks {
    private ControlCodeLinks(){}
    static boolean isLink(String value){if(value==null)return false;String candidate=value.trim();if(!candidate.regionMatches(true,0,"https://",0,8)&&!candidate.regionMatches(true,0,"http://",0,7))return false;return !new GenericCode(candidate,"").webLink().isEmpty();}
    static List<ScanTracker.Detection> products(List<ScanTracker.Detection> detections){List<ScanTracker.Detection> result=new ArrayList<>();for(ScanTracker.Detection detection:detections)if(!isLink(detection.value))result.add(detection);return result;}
    static List<ScanTracker.Detection> links(List<ScanTracker.Detection> detections){List<ScanTracker.Detection> result=new ArrayList<>();for(ScanTracker.Detection detection:detections)if(isLink(detection.value))result.add(detection);return result;}
}
