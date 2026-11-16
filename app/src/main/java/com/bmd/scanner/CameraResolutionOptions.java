package com.bmd.scanner;

final class CameraResolutionOptions {
    static final int HIGHEST=2;
    static final class Target {
        final int width,height;final String label;
        Target(int width,int height,String label){this.width=width;this.height=height;this.label=label;}
        int longEdge(){return Math.max(width,height);}
        int shortEdge(){return Math.min(width,height);}
    }
    static final Target[] TARGETS={
        new Target(1280,720,"HD · 1280 × 720"),new Target(1920,1080,"Full HD · 1920 × 1080"),null,
        new Target(720,1600,"720 × 1600 (HD+)"),new Target(720,1612,"720 × 1612 (HD+)"),
        new Target(1080,1920,"1080 × 1920 (FHD)"),new Target(1080,2340,"1080 × 2340 (FHD+)"),
        new Target(1080,2400,"1080 × 2400 (FHD+)"),new Target(1080,2424,"1080 × 2424 (FHD+)"),
        new Target(1220,2712,"1220 × 2712 (1.5K)"),new Target(1280,2856,"1280 × 2856 (1.5K)"),
        new Target(1440,3120,"1440 × 3120 (QHD+)"),new Target(1440,3200,"1440 × 3200 (QHD+)")
    };
    static String[] labels(String highest){String[] labels=new String[TARGETS.length];for(int i=0;i<labels.length;i++)labels[i]=i==HIGHEST?highest:TARGETS[i].label;return labels;}
    static double distance(int width,int height,Target target){
        int longer=Math.max(width,height),shorter=Math.min(width,height);
        return Math.abs(Math.log((double)longer/target.longEdge()))+Math.abs(Math.log((double)shorter/target.shortEdge()));
    }
    private CameraResolutionOptions(){}
}
