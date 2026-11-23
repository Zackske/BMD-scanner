package com.bmd.scanner;

import java.util.*;

/** Confirms decoded values across processed frames; never changes the QR contents. */
final class ScanConfirmation {
    private final Map<String,Integer> streaks=new HashMap<>();
    private final Map<String,Long> lastAdded=new HashMap<>();
    private long previousFrame=-1;
    List<String> acceptFrame(List<String> decoded,long now,int required,long repeatDelay){
        if(previousFrame<0||now-previousFrame>1200)streaks.clear();
        previousFrame=now;
        Set<String> present=new LinkedHashSet<>(decoded);present.remove(null);
        streaks.keySet().retainAll(present);
        List<String> accepted=new ArrayList<>();
        for(String value:present){
            int count=Math.min(required,streaks.getOrDefault(value,0)+1);streaks.put(value,count);
            Long previous=lastAdded.get(value);
            if(count>=required&&(previous==null||now-previous>=repeatDelay)){
                accepted.add(value);lastAdded.put(value,now);streaks.put(value,0);
            }
        }
        return accepted;
    }
    void reset(){streaks.clear();lastAdded.clear();previousFrame=-1;}
}
