package com.bmd.scanner;

/** Native code positions are in the rotated crop. Camera metering uses the original surface. */
final class CodeFocusPoint {
    private CodeFocusPoint(){}
    static float[] map(float x,float y,int rotation,int left,int top,int cropWidth,int cropHeight,int frameWidth,int frameHeight){
        if(!Float.isFinite(x)||!Float.isFinite(y)||cropWidth<1||cropHeight<1||frameWidth<1||frameHeight<1||left<0||top<0||left+cropWidth>frameWidth||top+cropHeight>frameHeight)return null;
        float originalX,originalY;
        switch(rotation){
            case 0:originalX=x;originalY=y;break;
            case 90:originalX=y;originalY=cropHeight-1-x;break;
            case 180:originalX=cropWidth-1-x;originalY=cropHeight-1-y;break;
            case 270:originalX=cropWidth-1-y;originalY=x;break;
            default:return null;
        }
        if(originalX<0||originalY<0||originalX>=cropWidth||originalY>=cropHeight)return null;
        return new float[]{(left+originalX)/frameWidth,(top+originalY)/frameHeight};
    }
}
