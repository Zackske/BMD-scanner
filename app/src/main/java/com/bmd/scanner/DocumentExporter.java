package com.bmd.scanner;

import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.text.*;
import java.io.*;
import java.util.*;

final class DocumentExporter {
    static final String[] EXT={"csv","xlsx","docx","pdf"};
    static final String[] MIME={"text/csv","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/pdf"};
    static byte[] build(int format,List<ScanTracker.Item> rows,String[] headings)throws IOException{
        if(format==0)return DocumentData.csv(rows,headings);if(format<3)return DocumentData.office(rows,headings,format==1);
        PdfDocument doc=new PdfDocument();try(ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
            TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.rgb(3,30,60));paint.setTextSize(12);PdfDocument.Page page=null;int y=0,number=0;
            List<String> lines=new ArrayList<>();lines.add("BMD");lines.add(String.join("   |   ",headings));for(ScanTracker.Item i:rows)lines.add(i.id+"   |   "+i.kind+"   |   "+i.value);
            for(String text:lines){StaticLayout layout=StaticLayout.Builder.obtain(text,0,text.length(),paint,515).setIncludePad(false).setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY).build();
                for(int line=0;line<layout.getLineCount();line++){int height=layout.getLineBottom(line)-layout.getLineTop(line);if(page==null||y+height>800){if(page!=null)doc.finishPage(page);page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,++number).create());y=40;}Canvas c=page.getCanvas();c.save();c.clipRect(40,y,555,y+height);c.translate(40,y-layout.getLineTop(line));layout.draw(c);c.restore();y+=height;}y+=10;
            }if(page!=null)doc.finishPage(page);doc.writeTo(bytes);return bytes.toByteArray();
        }finally{doc.close();}
    }
    static String filename(String entered,int format){String base=entered.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_").replaceAll("(?i)\\.(csv|xlsx|docx|pdf)$","").trim();if(base.isEmpty())base="BMD-scanresultaten";if(base.length()>80)base=base.substring(0,80);return base+"."+EXT[format];}
}
