package com.bmd.scanner;

import java.util.ArrayList;
import java.util.List;

/** Real hardware bounds only; never advertises an unsupported ultra-wide setting. */
final class ZoomOutRange {
    final float min,max;
    final float[] stops;
    ZoomOutRange(float hardwareMin,float hardwareMax){
        if(!Float.isFinite(hardwareMin)||!Float.isFinite(hardwareMax)||hardwareMin<=0||hardwareMax<hardwareMin){hardwareMin=1;hardwareMax=1;}
        min=Math.max(.5f,hardwareMin);max=Math.max(min,Math.min(1,hardwareMax));
        List<Float> values=new ArrayList<>();values.add(min);
        if(.75f>min+.005f&&.75f<max-.005f)values.add(.75f);
        if(max>min+.005f)values.add(max);
        stops=new float[values.size()];for(int n=0;n<stops.length;n++)stops[n]=values.get(n);
    }
    boolean available(){return max-min>.01f;}
    float clamp(float value){return Float.isFinite(value)?Math.max(min,Math.min(max,value)):max;}
    float step(float current,int direction){current=clamp(current);if(direction<0){for(int n=stops.length-1;n>=0;n--)if(stops[n]<current-.005f)return stops[n];return min;}for(float stop:stops)if(stop>current+.005f)return stop;return max;}
    float nearest(float value){float found=stops[0];for(float stop:stops)if(Math.abs(stop-value)<Math.abs(found-value))found=stop;return found;}
}
