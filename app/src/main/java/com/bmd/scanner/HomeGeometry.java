package com.bmd.scanner;

/** Fits the approved foreground proportionally; only internal background gaps flex. */
final class HomeGeometry {
    static final int[] EDGES={0,300,477,1337,1490,1648,1685,1812};
    final float scale,left,right;
    final float[] rows=new float[8];
    HomeGeometry(int width,int height){
        scale=Math.min(width/868f,height/1445f);
        float contentWidth=868*scale;
        left=(width-contentWidth)/2;right=(width+contentWidth)/2;
        float flexible=Math.max(0,height-1445*scale);
        for(int i=0;i<7;i++){
            int sourceHeight=EDGES[i+1]-EDGES[i];
            boolean gap=i==1||i==3||i==5;
            rows[i+1]=rows[i]+(gap?flexible*sourceHeight/367f:sourceHeight*scale);
        }
        rows[7]=height;
    }
    float y(float sourceY){for(int i=0;i<7;i++)if(sourceY<=EDGES[i+1])return rows[i]+(sourceY-EDGES[i])*(rows[i+1]-rows[i])/(EDGES[i+1]-EDGES[i]);return rows[7];}
}
