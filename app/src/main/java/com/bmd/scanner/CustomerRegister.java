package com.bmd.scanner;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/** Private, backup-excluded register. A failed import keeps the previous register intact. */
final class CustomerRegister {
    static final String FILE_NAME="customer-register.json";
    static final int MAX_BYTES=2*1024*1024;
    private final AtomicFile file;
    private final JSONObject countries;
    CustomerRegister(Context context,JSONObject countries){
        file=new AtomicFile(new File(context.getNoBackupFilesDir(),FILE_NAME));this.countries=countries;
    }
    JSONArray read()throws Exception{
        try(InputStream input=file.openRead()){return validate(readText(input));}
        catch(FileNotFoundException noRegister){return new JSONArray();}
    }
    static String readText(InputStream input)throws Exception{
        try(ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int count;
            while((count=input.read(buffer))!=-1){if(out.size()+count>MAX_BYTES)throw new IllegalArgumentException("Register too large");out.write(buffer,0,count);}
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    JSONArray validate(String text)throws Exception{
        JSONObject document=new JSONObject(text);
        if(document.optInt("schema")!=1)throw new IllegalArgumentException("Unsupported register");
        JSONArray rows=document.getJSONArray("customers");
        if(rows.length()<1||rows.length()>1000)throw new IllegalArgumentException("Invalid customer count");
        Set<String> ids=new HashSet<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);
            for(String key:new String[]{"id","country","name","customerId","shippingGroup"})if(!(row.get(key) instanceof String))throw new IllegalArgumentException("Invalid identity field");
            String id=row.getString("id"),country=row.getString("country");
            if(!id.matches("[A-Za-z0-9_-]{1,100}")||!ids.add(id)||!countries.has(country))throw new IllegalArgumentException("Invalid identity or country");
            for(String key:new String[]{"name","customerId"})if(row.getString(key).trim().isEmpty())throw new IllegalArgumentException("Missing identity");
            String group=row.getString("shippingGroup");
            if(!group.equals("eu")&&!group.equals("non-eu")&&!group.equals("world"))throw new IllegalArgumentException("Invalid shipping group");
            for(String key:new String[]{"displayName","contact","email","vat","sourceSheet","sourceGroup"}){
                if(!row.has(key)||row.isNull(key))row.put(key,"");else if(!(row.get(key) instanceof String))throw new IllegalArgumentException("Invalid text field");
            }
            for(String key:new String[]{"shipping","extraShipping","documents","other","rawCells"}){
                if(!row.has(key))row.put(key,new JSONArray());else row.getJSONArray(key);
            }
            if(!row.has("address"))row.put("address",new JSONObject());else row.getJSONObject("address");
            if(!row.has("sourceStatus"))row.put("sourceStatus",new JSONObject());else row.getJSONObject("sourceStatus");
            if(row.has("identitySource")&&!row.isNull("identitySource")){
                JSONObject source=row.getJSONObject("identitySource");
                if(!source.has("rows"))source.put("rows",new JSONArray());else source.getJSONArray("rows");
            }
            for(String key:new String[]{"other","rawCells"})for(int n=0;n<row.getJSONArray(key).length();n++)row.getJSONArray(key).getJSONObject(n);
        }
        return rows;
    }
    synchronized void importText(String text)throws Exception{
        JSONArray rows=validate(text);byte[] bytes=new JSONObject().put("schema",1).put("customers",rows).toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES)throw new IllegalArgumentException("Register too large");
        FileOutputStream output=null;
        try{output=file.startWrite();output.write(bytes);file.finishWrite(output);}
        catch(Exception failure){if(output!=null)file.failWrite(output);throw failure;}
    }
}
