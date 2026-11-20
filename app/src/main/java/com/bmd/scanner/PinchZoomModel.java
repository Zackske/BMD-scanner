package com.bmd.scanner;
final class PinchZoomModel {
    float min=1,max=1,target=1;
    void configure(float low,float high,float initial){if(!Float.isFinite(low)||!Float.isFinite(high)||low<=0||high<low){low=high=1;}min=low;max=high;target=clamp(initial);}
    float clamp(float value){return Float.isFinite(value)?Math.max(min,Math.min(max,value)):target;}
    boolean available(){return max-min>.001f;}
    float scale(float factor){if(Float.isFinite(factor)&&factor>0)target=clamp(target*factor);return target;}
}
