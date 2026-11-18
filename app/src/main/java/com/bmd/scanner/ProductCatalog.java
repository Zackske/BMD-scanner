package com.bmd.scanner;
import java.util.*;

/** Explicit physical-product grouping. Equal payloads never automatically merge products. */
final class ProductCatalog {
    static final class Product {
        int id,number,colour;final List<ScanTracker.Item> codes=new ArrayList<>();String lot="",gtin="",expiry="";boolean conflict,manual;
        Product(int id){this.id=id;}
        String description(){StringBuilder b=new StringBuilder();b.append("LOT=").append(lot.isEmpty()?"?":lot).append("; GTIN=").append(gtin).append("; EXP=").append(expiry).append("; ");for(ScanTracker.Item c:codes)b.append('[').append(c.kind).append(": ").append(c.value).append("] ");return b.toString();}
    }
    final Map<Integer,Integer> groups=new TreeMap<>();final Map<Integer,String> manualLots=new TreeMap<>();
    final Map<String,Integer> colours=new LinkedHashMap<>();private final Map<Integer,Integer> numbers=new HashMap<>();
    int group(int id){return groups.getOrDefault(id,id);}
    List<Product> view(List<ScanTracker.Item> rows){
        Map<Integer,Product> result=new TreeMap<>();for(ScanTracker.Item row:rows){int id=group(row.id);Product p=result.computeIfAbsent(id,Product::new);p.codes.add(row);LotData d=LotData.parse(row.value);p.lot=combine(p,p.lot,d.lot);p.gtin=combine(p,p.gtin,d.gtin);p.expiry=combine(p,p.expiry,d.expiry);}
        int next=0;for(Product p:result.values()){p.number=numbers.getOrDefault(p.id,++next);if(manualLots.containsKey(p.id)){p.lot=manualLots.get(p.id);p.manual=true;}p.colour=colour(p.lot);}return new ArrayList<>(result.values());
    }
    private String combine(Product p,String old,String next){if(next.isEmpty())return old;if(!old.isEmpty()&&!old.equals(next))p.conflict=true;return old.isEmpty()?next:old;}
    List<Product> project(List<ScanTracker.Item> rows){numbers.clear();List<Product> list=view(rows);int n=0;for(Product p:list){p.number=++n;numbers.put(p.id,n);}return list;}
    void merge(Collection<Integer> selected,List<ScanTracker.Item> rows){
        if(selected.size()<2)return;Set<Integer> set=new HashSet<>(selected);String lot="",gtin="",expiry="";
        for(Product p:view(rows))if(set.contains(p.id)){if(p.conflict)throw new IllegalArgumentException("conflicting codes");if(!p.lot.isEmpty()){if(!lot.isEmpty()&&!lot.equals(p.lot))throw new IllegalArgumentException("different lots");lot=p.lot;}if(!p.gtin.isEmpty()){if(!gtin.isEmpty()&&!gtin.equals(p.gtin))throw new IllegalArgumentException("different products");gtin=p.gtin;}if(!p.expiry.isEmpty()){if(!expiry.isEmpty()&&!expiry.equals(p.expiry))throw new IllegalArgumentException("different expiry dates");expiry=p.expiry;}}
        int first=Collections.min(set);String manual="";for(int id:set){if(manualLots.containsKey(id))manual=manualLots.get(id);manualLots.remove(id);}for(ScanTracker.Item row:rows)if(set.contains(group(row.id)))groups.put(row.id,first);if(!manual.isEmpty())manualLots.put(first,manual);
    }
    void split(Collection<Integer> selected,List<ScanTracker.Item> rows){Set<Integer> set=new HashSet<>(selected);List<Integer> ids=new ArrayList<>();for(ScanTracker.Item row:rows)if(set.contains(group(row.id)))ids.add(row.id);for(int id:ids)groups.remove(id);for(int id:set)manualLots.remove(id);}
    void reset(){groups.clear();manualLots.clear();numbers.clear();}
    int colour(String lot){if(lot.isEmpty())return 0xffabb8c4;if(!colours.containsKey(lot))colours.put(lot,colours.size());int n=colours.get(lot);if(n==0)return 0xff32ef6d;if(n==1)return 0xffffc333;if(n==2)return 0xffbc69ff;
        double hue=(n*.618033988749895)%1*6;int x=(int)(255*(1-Math.abs(hue%2-1))),r=0,g=0,b=0;switch((int)hue){case 0:r=255;g=x;break;case 1:r=x;g=255;break;case 2:g=255;b=x;break;case 3:g=x;b=255;break;case 4:r=x;b=255;break;default:r=255;b=x;}return 0xff000000|(r<<16)|(g<<8)|b;}
    Map<String,Integer> lotCounts(List<ScanTracker.Item> rows){Map<String,Integer> counts=new LinkedHashMap<>();for(Product p:view(rows)){String lot=p.conflict?"!":p.lot;counts.put(lot,counts.getOrDefault(lot,0)+1);}return counts;}
    List<ScanTracker.Item> exportRows(List<ScanTracker.Item> rows){List<ScanTracker.Item> result=new ArrayList<>();for(Product p:project(rows)){ScanTracker.Item first=p.codes.get(0);ScanTracker.Item row=new ScanTracker.Item(p.number,new ScanTracker.Detection(first.raw,"Product",first.left,first.top,first.right,first.bottom),first.seen);row.value=p.description()+(p.manual?" [manual lot]":"");row.confirmed=true;result.add(row);}return result;}
}

