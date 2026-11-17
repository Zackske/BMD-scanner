package com.bmd.scanner;

import java.util.*;

/** Session-local physical instances. Payload equality is deliberately not identity. */
final class ScanTracker {
    static final class Detection {
        final String value,kind;final float left,top,right,bottom;
        Detection(String value,String kind,float l,float t,float r,float b){this.value=value;this.kind=kind;left=l;top=t;right=r;bottom=b;}
        float x(){return (left+right)/2;}float y(){return (top+bottom)/2;}
    }
    static final class Item {
        final int id;final String raw,kind;String value;float left,top,right,bottom;int hits=1;long seen;boolean confirmed,deleted;
        Item(int id,Detection d,long time){this.id=id;raw=d.value;value=d.value;kind=d.kind;update(d,time);}
        void update(Detection d,long time){left=d.left;top=d.top;right=d.right;bottom=d.bottom;seen=time;}
        float x(){return (left+right)/2;}float y(){return (top+bottom)/2;}
        Item copy(){Item i=new Item(id,new Detection(raw,kind,left,top,right,bottom),seen);i.value=value;i.confirmed=confirmed;i.deleted=deleted;i.hits=hits;return i;}
    }
    private final List<Item> tracks=new ArrayList<>();private int next=1;private long lastFrame;
    List<Item> items(){List<Item> result=new ArrayList<>();for(Item i:tracks)if(i.confirmed&&!i.deleted)result.add(i.copy());return result;}
    List<Item> visible(long now){List<Item> result=new ArrayList<>();for(Item i:tracks)if(i.confirmed&&!i.deleted&&now-i.seen<800)result.add(i.copy());return result;}
    List<Item> all(){List<Item> result=new ArrayList<>();for(Item i:tracks)if(i.confirmed||i.deleted)result.add(i.copy());return result;}
    void rescale(float scale){for(Item i:tracks){i.left=.5f+(i.left-.5f)*scale;i.right=.5f+(i.right-.5f)*scale;i.top=.5f+(i.top-.5f)*scale;i.bottom=.5f+(i.bottom-.5f)*scale;}}
    void reset(){tracks.clear();next=1;lastFrame=0;}
    void edit(int id,String value){for(Item i:tracks)if(i.id==id)i.value=value;}
    void remove(int id){for(Item i:tracks)if(i.id==id)i.deleted=true;}
    void restore(List<Item> saved){reset();for(Item i:saved){tracks.add(i.copy());next=Math.max(next,i.id+1);}}
    void importCode(Detection d,long now){Item item=new Item(next++,d,now);item.confirmed=true;tracks.add(item);}
    int update(List<Detection> detections,long now,int required){
        float dx=0,dy=0;
        // Unambiguous payloads supply a common camera translation for duplicate labels.
        List<Float> xs=new ArrayList<>(),ys=new ArrayList<>();
        for(Detection d:detections){Item only=null;int old=0,fresh=0;for(Item i:tracks)if(i.raw.equals(d.value)&&i.kind.equals(d.kind)&&now-i.seen<1200){old++;only=i;}for(Detection e:detections)if(e.value.equals(d.value)&&e.kind.equals(d.kind))fresh++;if(old==1&&fresh==1){xs.add(d.x()-only.x());ys.add(d.y()-only.y());}}
        if(!xs.isEmpty()){Collections.sort(xs);Collections.sort(ys);dx=xs.get(xs.size()/2);dy=ys.get(ys.size()/2);}
        final float motionX=dx,motionY=dy;
        class Match {Item item;int index;float score;Match(Item i,int n,float s){item=i;index=n;score=s;}}
        List<Match> matches=new ArrayList<>();
        for(Item i:tracks)for(int n=0;n<detections.size();n++){
            Detection d=detections.get(n);if(!i.raw.equals(d.value)||!i.kind.equals(d.kind))continue;
            float x=i.x()+(now-i.seen<1200?motionX:0),y=i.y()+(now-i.seen<1200?motionY:0);
            float distance=(float)Math.hypot(x-d.x(),y-d.y());float extent=Math.max(Math.max(i.right-i.left,i.bottom-i.top),Math.max(d.right-d.left,d.bottom-d.top));
            float gate=Math.max(.055f,Math.min(.22f,extent*.8f));if(distance<=gate)matches.add(new Match(i,n,distance));
        }
        matches.sort(Comparator.comparingDouble(m->m.score));Set<Integer> used=new HashSet<>();Set<Item> assigned=new HashSet<>();int added=0;
        for(Match m:matches){if(used.contains(m.index)||assigned.contains(m.item))continue;Item i=m.item;Detection d=detections.get(m.index);boolean consecutive=now-i.seen<1200&&lastFrame-i.seen<100;i.hits=consecutive?i.hits+1:1;i.update(d,now);used.add(m.index);assigned.add(i);if(!i.confirmed&&!i.deleted&&i.hits>=required){i.confirmed=true;added++;}}
        for(int n=0;n<detections.size();n++)if(!used.contains(n)){Item i=new Item(next++,detections.get(n),now);i.confirmed=required<=1;tracks.add(i);if(i.confirmed)added++;}
        // Keep confirmed/deleted instances for the whole session; discard only unconfirmed noise.
        tracks.removeIf(i->!i.confirmed&&!i.deleted&&now-i.seen>1800);lastFrame=now;return added;
    }
}
