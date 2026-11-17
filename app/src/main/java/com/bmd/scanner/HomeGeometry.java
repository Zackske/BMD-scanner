package com.bmd.scanner;

/** Keeps foreground proportions while assigning spare height only to background gaps. */
final class HomeGeometry {
    static final int[] EDGES={106,400,470,1205,1255,1410,1450,1564};
    final float scale,left,right;
    final float[] rows=new float[8];
    HomeGeometry(int width,int height){
        scale=Math.min(width/879f,height/1458f);
        float contentWidth=879*scale;
        left=(width-contentWidth)/2;right=(width+contentWidth)/2;
        float spare=Math.max(0,height-1458*scale);
        for(int i=0;i<7;i++)rows[i+1]=rows[i]+(EDGES[i+1]-EDGES[i])*scale+((i==1||i==3||i==5)?spare/3:0);
        rows[7]=height;
    }
    float y(float sourceY){for(int i=0;i<7;i++)if(sourceY<=EDGES[i+1])return rows[i]+(sourceY-EDGES[i])*(rows[i+1]-rows[i])/(EDGES[i+1]-EDGES[i]);return rows[7];}
}

