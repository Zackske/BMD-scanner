package com.bmd.scanner;

import java.util.*;
import java.util.regex.*;
import java.time.*;

/** Conservative table parser: no correction of OCR digits and no inferred product identity. */
final class PicklistData {
    static final class Row {
        final String article,lot,expiry;final int quantity;final boolean manual;
        Row(String a,String l,String e,int q,boolean m){
            article=a.trim().toUpperCase(Locale.ROOT);lot=l.trim();expiry=date(e);quantity=q;manual=m;
            if(!article.matches("[A-Z][A-Z0-9._/-]{2,39}")||lot.isEmpty()||lot.length()>40||expiry.isEmpty()||q<1||q>10000)throw new IllegalArgumentException("row");
        }
        String key(){return article+"|"+lot+"|"+expiry;}
    }
    static final class Page {
        final List<String> lines;final String digest;
        Page(List<String> lines,String digest){this.lines=new ArrayList<>(lines);this.digest=digest;}
    }
    final List<Row> rows=new ArrayList<>();final List<Page> pages=new ArrayList<>();
    int unparsed,declared=-1,missingPages,requiredPages;String order="",kind="";
    static String date(String input){try{
        String v=input.trim();if(v.matches("\\d{6}")){int y=2000+Integer.parseInt(v.substring(0,2)),m=Integer.parseInt(v.substring(2,4)),d=Integer.parseInt(v.substring(4));return (d==0?YearMonth.of(y,m).atEndOfMonth():LocalDate.of(y,m,d)).toString();}
        if(v.matches("\\d{2}[-/.]\\d{2}[-/.]\\d{4}"))v=v.substring(6)+"-"+v.substring(3,5)+"-"+v.substring(0,2);
        return LocalDate.parse(v).toString();
    }catch(Exception e){return "";}}
    static PicklistData parse(List<Page> pages){
        PicklistData out=new PicklistData();Set<String> digests=new HashSet<>();Set<Integer> numbered=new HashSet<>();
        for(Page page:pages){if(!digests.add(page.digest))throw new IllegalArgumentException("duplicate");out.pages.add(page);
            for(String raw:page.lines){String line=raw.replace('\u00a0',' ').replaceAll("\\s+"," ").trim();String lower=line.toLowerCase(Locale.ROOT);
                if(lower.contains("shipment list")||lower.contains("shipmentlist"))out.kind="shipment";
                if(lower.contains("invoice"))out.kind="invoice";
                Matcher order=Pattern.compile("(?i)\\b(?:order|ordernummer)\\s*[:#]?\\s*(\\d{6,12})").matcher(line);if(order.find())out.order=order.group(1);
                Matcher pagination=Pattern.compile("(?i)\\b(?:page|pagina|seite)\\s*(\\d+)\\s*(?:of|van|von|/)\\s*(\\d+)").matcher(line);
                if(pagination.find()){numbered.add(Integer.parseInt(pagination.group(1)));out.requiredPages=Math.max(out.requiredPages,Integer.parseInt(pagination.group(2)));}
                if(lower.matches("^(total|totaal)\\b.*")){Matcher n=Pattern.compile("\\b(\\d+)\\b").matcher(line);while(n.find())out.declared=Integer.parseInt(n.group(1));}
                Matcher id=Pattern.compile("^(PRO\\d{4})\\b",Pattern.CASE_INSENSITIVE).matcher(line);
                if(!id.find())continue;
                Matcher tail=Pattern.compile("(\\d+)\\s+([A-Za-z0-9._/-]+)\\s+(\\d{4}-\\d{2}-\\d{2}|\\d{2}[-/.]\\d{2}[-/.]\\d{4})$").matcher(line);
                try{if(!tail.find())throw new IllegalArgumentException();out.rows.add(new Row(id.group(1),tail.group(2),tail.group(3),Integer.parseInt(tail.group(1)),false));}catch(Exception e){out.unparsed++;}
            }
        }
        if(out.requiredPages>0)for(int n=1;n<=out.requiredPages;n++)if(!numbered.contains(n))out.missingPages++;
        return out;
    }
    int total(){int sum=0;for(Row row:rows)sum=Math.addExact(sum,row.quantity);return sum;}
    boolean consistent(){try{return !rows.isEmpty()&&unparsed==0&&missingPages==0&&total()<=10000&&(declared<0||declared==total());}catch(Exception e){return false;}}
    static Map<String,Integer> expected(List<Row> rows){Map<String,Integer> result=new LinkedHashMap<>();for(Row row:rows)result.put(row.key(),result.getOrDefault(row.key(),0)+row.quantity);return result;}
    static boolean within(List<Row> rows,Map<String,Integer> observed){Map<String,Integer> expected=expected(rows);for(Map.Entry<String,Integer> e:observed.entrySet())if(e.getValue()<0||e.getValue()>expected.getOrDefault(e.getKey(),0))return false;return true;}
}
