package com.bmd.scanner;

import java.util.*;

/** Coordinates are on the overview image. Payload equality alone never identifies a product. */
final class BoxSession {
    final ScanTracker tracker=new ScanTracker();
    final ProductCatalog products=new ProductCatalog();
    final List<Shot> shots=new ArrayList<>();
    final List<Integer> expectedHistory=new ArrayList<>();
    int expected; boolean reviewed;
    BoxSession(int expected){setExpected(expected);}
    void setExpected(int n){if(n<1||n>10000)throw new IllegalArgumentException("count");expected=n;expectedHistory.add(n);reviewed=false;}
    static boolean marker(String value){return value.matches("BMD-BOX-[ABCD]");}
    static final class Shot {final String file;final double[] map;final Set<Integer> ids;Shot(String f,double[] m,Set<Integer> i){file=f;map=m.clone();ids=i;}}
    static float[] point(double[] m,float x,float y){double z=m[6]*x+m[7]*y+m[8];if(!Double.isFinite(z)||Math.abs(z)<1e-8)throw new IllegalArgumentException("map");return new float[]{(float)((m[0]*x+m[1]*y+m[2])/z),(float)((m[3]*x+m[4]*y+m[5])/z)};}
    static ScanTracker.Detection project(ScanTracker.Detection d,double[] m){float[] a=point(m,d.left,d.top),b=point(m,d.right,d.top),c=point(m,d.right,d.bottom),e=point(m,d.left,d.bottom);float l=Math.min(Math.min(a[0],b[0]),Math.min(c[0],e[0])),r=Math.max(Math.max(a[0],b[0]),Math.max(c[0],e[0])),t=Math.min(Math.min(a[1],b[1]),Math.min(c[1],e[1])),v=Math.max(Math.max(a[1],b[1]),Math.max(c[1],e[1]));if(l<-.03||t<-.03||r>1.03||v>1.03||r<=l||v<=t)throw new IllegalArgumentException("outside overview");return new ScanTracker.Detection(d.value,d.kind,Math.max(0,l),Math.max(0,t),Math.min(1,r),Math.min(1,v));}
    Set<Integer> add(String file,List<ScanTracker.Detection> detections,double[] map){
        // Validate the entire photo before mutating the session.
        List<ScanTracker.Detection> mapped=new ArrayList<>();for(ScanTracker.Detection d:detections)if(!marker(d.value))mapped.add(project(d,map));
        Set<Integer> ids=new HashSet<>(),used=new HashSet<>();List<ScanTracker.Item> previous=tracker.items();
        for(ScanTracker.Detection d:mapped)for(ScanTracker.Item old:previous)if(old.kind.equals(d.kind)&&!old.raw.equals(d.value)&&Math.abs(old.x()-d.x())<Math.min(old.right-old.left,d.right-d.left)*.3f&&Math.abs(old.y()-d.y())<Math.min(old.bottom-old.top,d.bottom-d.top)*.3f)throw new IllegalArgumentException("Conflicting code at the same overview position");
        for(ScanTracker.Detection d:mapped){ScanTracker.Item match=null;float best=Float.MAX_VALUE;for(ScanTracker.Item old:previous){if(used.contains(old.id)||!old.raw.equals(d.value)||!old.kind.equals(d.kind))continue;float gx=Math.max(.001f,Math.min(old.right-old.left,d.right-d.left)*.3f),gy=Math.max(.001f,Math.min(old.bottom-old.top,d.bottom-d.top)*.3f);float dx=Math.abs(old.x()-d.x()),dy=Math.abs(old.y()-d.y());if(dx<=gx&&dy<=gy&&dx/gx+dy/gy<best){best=dx/gx+dy/gy;match=old;}}
            if(match==null){tracker.importCode(d,System.currentTimeMillis());List<ScanTracker.Item> all=tracker.items();ids.add(all.get(all.size()-1).id);}else{ids.add(match.id);used.add(match.id);}}
        shots.add(new Shot(file,map,ids));reviewed=false;products.project(tracker.items());return ids;
    }
    boolean countMatches(){return products.view(tracker.items()).size()==expected;}
    boolean ready(){if(shots.size()<4||!reviewed||!countMatches())return false;for(ProductCatalog.Product p:products.view(tracker.items()))if(p.conflict||p.lot.isEmpty())return false;return true;}
}

