package com.bmd.scanner;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Durable shipment evidence. A reviewed carton is revalidated from its session on every read. */
final class PicklistJob {
    final File directory;final List<PicklistData.Row> rows=new ArrayList<>();
    final Map<String,String> gtinArticles=new LinkedHashMap<>();final Map<Integer,String> cartons=new LinkedHashMap<>();
    JSONArray mappingHistory=new JSONArray();
    int capacity=113;String order="";boolean confirmed;
    PicklistJob(File dir){directory=dir;}
    static PicklistJob create(Context c)throws IOException{File d=new File(new File(c.getFilesDir(),"picklists"),UUID.randomUUID().toString());if(!d.mkdirs())throw new IOException("Folder");return new PicklistJob(d);}
    static PicklistJob load(Context c,String path)throws Exception{
        File root=new File(c.getFilesDir(),"picklists").getCanonicalFile(),dir=new File(path).getCanonicalFile();
        if(!root.equals(dir.getParentFile()))throw new IOException("Session");
        PicklistJob j=new PicklistJob(dir);JSONObject o=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(dir,"picklist.json").toPath()),StandardCharsets.UTF_8));
        j.mappingHistory=o.optJSONArray("mappingHistory");if(j.mappingHistory==null)j.mappingHistory=new JSONArray();j.capacity=o.getInt("capacity");j.order=o.optString("order");j.confirmed=o.optBoolean("confirmed");
        JSONArray rs=o.getJSONArray("rows");for(int n=0;n<rs.length();n++){JSONObject r=rs.getJSONObject(n);j.rows.add(new PicklistData.Row(r.getString("article"),r.getString("lot"),r.getString("expiry"),r.getInt("quantity"),r.optBoolean("manual")));}
        JSONObject maps=o.getJSONObject("gtinArticles");for(Iterator<String> it=maps.keys();it.hasNext();){String key=it.next();j.gtinArticles.put(key,maps.getString(key));}
        JSONObject boxes=o.getJSONObject("cartons");for(Iterator<String> it=boxes.keys();it.hasNext();){String key=it.next();String folder=boxes.getString(key);File f=new File(folder).getCanonicalFile();if(!new File(c.getFilesDir(),"boxes").getCanonicalFile().equals(f.getParentFile()))throw new IOException("Carton");j.cartons.put(Integer.parseInt(key),folder);}
        if(new HashSet<>(j.cartons.values()).size()!=j.cartons.size())throw new IOException("Duplicate carton session");if(!j.rows.isEmpty())j.plan();else if(j.confirmed)throw new IOException("Rows");return j;
    }
    CartonPlan plan(){return new CartonPlan(total(),capacity);}
    int total(){int n=0;for(PicklistData.Row row:rows)n=Math.addExact(n,row.quantity);return n;}
    JSONObject json()throws JSONException{
        JSONObject o=new JSONObject().put("version",1).put("order",order).put("capacity",capacity).put("confirmed",confirmed).put("shipmentApproved",false);
        JSONArray rs=new JSONArray();for(PicklistData.Row r:rows)rs.put(new JSONObject().put("article",r.article).put("lot",r.lot).put("expiry",r.expiry).put("quantity",r.quantity).put("manual",r.manual));
        JSONObject boxes=new JSONObject();for(Map.Entry<Integer,String> e:cartons.entrySet())boxes.put(e.getKey().toString(),e.getValue());
        o.put("mappingHistory",mappingHistory).put("rows",rs).put("gtinArticles",new JSONObject(gtinArticles)).put("cartons",boxes);return o;
    }
    void link(String gtin,String article)throws Exception{mappingHistory.put(new JSONObject().put("gtin",gtin).put("previous",gtinArticles.getOrDefault(gtin,"" )).put("article",article).put("time",System.currentTimeMillis()));if(article.isEmpty())gtinArticles.remove(gtin);else gtinArticles.put(gtin,article);save();}
    void save()throws Exception{AtomicFile target=new AtomicFile(new File(directory,"picklist.json"));FileOutputStream out=target.startWrite();try{out.write(json().toString(2).getBytes(StandardCharsets.UTF_8));target.finishWrite(out);}catch(Exception e){target.failWrite(out);throw e;}}
    static String article(String raw){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\bPRO\\d{4}\\b",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(raw);return m.find()?m.group().toUpperCase(Locale.ROOT):"";}
    String reference(String gtin,String raw){String direct=article(raw);return direct.isEmpty()?gtinArticles.getOrDefault(gtin,""):direct;}
    String key(String article,String lot,String expiry){return article+"|"+lot+"|"+PicklistData.date(expiry);}
    List<String> candidates(String lot,String expiry){Set<String> result=new LinkedHashSet<>();for(PicklistData.Row row:rows)if(row.lot.equals(lot)&&row.expiry.equals(PicklistData.date(expiry)))result.add(row.article);return new ArrayList<>(result);}
    /** Returns null for missing/uncertain identification or a changed, incomplete carton. */
    Map<String,Integer> observed(int index)throws Exception{
        if(!cartons.containsKey(index))return null;
        JSONObject o=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(cartons.get(index),"session.json").toPath()),StandardCharsets.UTF_8));
        if(!o.optString("picklistJob").equals(directory.getPath())||o.optInt("cartonIndex",-1)!=index)return null;
        if(!o.optBoolean("complete")||!o.optBoolean("reviewed")||o.getInt("expected")!=plan().quantities[index])return null;
        JSONArray products=o.getJSONArray("products"),items=o.getJSONArray("items");Map<String,Integer> result=new LinkedHashMap<>();
        if(products.length()!=plan().quantities[index])return null;
        for(int n=0;n<products.length();n++){
            JSONObject p=products.getJSONObject(n);if(p.optBoolean("conflict"))return null;
            JSONArray ids=p.getJSONArray("ids");String article="",gtin="",expiry="",lot=p.optString("lot");
            for(int k=0;k<items.length();k++){JSONObject item=items.getJSONObject(k);boolean use=false;for(int z=0;z<ids.length();z++)if(ids.getInt(z)==item.getInt("id"))use=true;if(!use||item.optBoolean("deleted"))continue;
                LotData data=LotData.parse(item.getString("value"));if(!data.gtin.isEmpty())gtin=data.gtin;if(!data.expiry.isEmpty())expiry=data.expiry;String ref=article(item.getString("value"));if(!ref.isEmpty()){if(!article.isEmpty()&&!article.equals(ref))return null;article=ref;}
            }
            if(article.isEmpty())article=gtinArticles.getOrDefault(gtin,"");if(article.isEmpty()||lot.isEmpty()||PicklistData.date(expiry).isEmpty())return null;
            String key=key(article,lot,expiry);result.put(key,result.getOrDefault(key,0)+1);
        }
        return result;
    }
    Map<String,Integer> totals()throws Exception{Map<String,Integer> result=new LinkedHashMap<>();for(int i=0;i<plan().boxes;i++){Map<String,Integer> count=observed(i);if(count!=null)for(Map.Entry<String,Integer> e:count.entrySet())result.put(e.getKey(),result.getOrDefault(e.getKey(),0)+e.getValue());}return result;}
    boolean complete()throws Exception{if(!confirmed)return false;for(int i=0;i<plan().boxes;i++)if(observed(i)==null)return false;return totals().equals(PicklistData.expected(rows));}
}
