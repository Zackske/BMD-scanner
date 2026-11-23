package com.bmd.scanner;
import java.util.*;
import java.util.regex.*;

/** Conservative GS1 extraction. Unknown or malformed data stays unknown; never infer from digits. */
final class LotData {
    final String gtin,lot,expiry;
    LotData(String gtin,String lot,String expiry){this.gtin=gtin;this.lot=lot;this.expiry=expiry;}
    static LotData parse(String value){
        if(value==null)return new LotData("","","");
        String s=value.startsWith("]d2")||value.startsWith("]C1")||value.startsWith("]Q3")?value.substring(3):value;
        Map<String,String> fields=new HashMap<>();
        Matcher hri=Pattern.compile("\\((\\d{2,4})\\)([^()]*)").matcher(s);
        if(hri.find()){do{fields.put(hri.group(1),hri.group(2).replace("\u001d","").trim());}while(hri.find());}
        else if(s.startsWith("01")&&s.length()>=16&&s.substring(2,16).matches("\\d{14}")){
            int pos=0;while(pos+2<=s.length()){
                if(s.charAt(pos)==29){pos++;continue;}String ai=s.substring(pos,pos+2);pos+=2;int length;
                if(ai.equals("01"))length=14;else if(Arrays.asList("11","13","15","17").contains(ai))length=6;
                else if(ai.equals("10")||ai.equals("21")){int end=s.indexOf(29,pos);if(end<0)end=s.length();String v=s.substring(pos,end);if(v.length()>20)break;fields.put(ai,v);pos=end;continue;}else break;
                if(pos+length>s.length())break;String v=s.substring(pos,pos+length);if(!v.matches("\\d+"))break;fields.put(ai,v);pos+=length;
            }
        }
        String lot=fields.getOrDefault("10","");if(lot.length()>20||lot.indexOf(29)>=0)lot="";
        String gtin=fields.getOrDefault("01","");if(!gtin.matches("\\d{14}"))gtin="";
        String expiry=fields.getOrDefault("17","");if(!expiry.matches("\\d{6}"))expiry="";
        return new LotData(gtin,lot,expiry);
    }
}

