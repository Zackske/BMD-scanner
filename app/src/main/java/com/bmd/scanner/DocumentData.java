package com.bmd.scanner;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Real OOXML documents, with text cells so leading zeroes and formula-like codes survive. */
final class DocumentData {
    static String xml(String s){StringBuilder out=new StringBuilder();s.codePoints().forEach(c->{if(c==9||c==10||c==13||c>=32&&c<=0xD7FF||c>=0xE000&&c<=0xFFFD||c>=0x10000&&c<=0x10FFFF)out.appendCodePoint(c);});return out.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    static byte[] csv(List<ScanTracker.Item> rows,String[] headings){StringBuilder b=new StringBuilder("\ufeff");b.append(line(Arrays.asList(headings)));for(ScanTracker.Item i:rows)b.append(line(Arrays.asList(Integer.toString(i.id),i.kind,i.value)));return b.toString().getBytes(StandardCharsets.UTF_8);}
    private static String line(List<String> cells){List<String> quoted=new ArrayList<>();for(String c:cells){if(!c.isEmpty()&&"=+@-\t\r".indexOf(c.charAt(0))>=0)c="'"+c;quoted.add("\""+c.replace("\"","\"\"")+"\"");}return String.join(",",quoted)+"\r\n";}
    private static void entry(ZipOutputStream zip,String path,String text)throws IOException{zip.putNextEntry(new ZipEntry(path));zip.write(text.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
    static byte[] office(List<ScanTracker.Item> rows,String[] headings,boolean excel)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(bytes)){
            String ns="http://schemas.openxmlformats.org/",part=excel?"xl/workbook.xml":"word/document.xml";
            entry(zip,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/>"+(excel?"<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>":"<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>")+"</Types>");
            entry(zip,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\""+ns+"officeDocument/2006/relationships/officeDocument\" Target=\""+part+"\"/></Relationships>");
            List<List<String>> all=new ArrayList<>();all.add(Arrays.asList(headings));for(ScanTracker.Item i:rows)all.add(Arrays.asList(Integer.toString(i.id),i.kind,i.value));
            if(excel){
                entry(zip,"xl/workbook.xml","<workbook xmlns=\""+ns+"spreadsheetml/2006/main\" xmlns:r=\""+ns+"officeDocument/2006/relationships\"><sheets><sheet name=\"Scans\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
                entry(zip,"xl/_rels/workbook.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\""+ns+"officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
                StringBuilder sheet=new StringBuilder("<worksheet xmlns=\""+ns+"spreadsheetml/2006/main\"><cols><col min=\"1\" max=\"2\" width=\"16\" customWidth=\"1\"/><col min=\"3\" max=\"3\" width=\"65\" customWidth=\"1\"/></cols><sheetData>");int r=0;
                for(List<String> row:all){r++;sheet.append("<row r=\"").append(r).append("\">");for(int c=0;c<row.size();c++)sheet.append("<c r=\"").append((char)('A'+c)).append(r).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(row.get(c))).append("</t></is></c>");sheet.append("</row>");}sheet.append("</sheetData></worksheet>");entry(zip,"xl/worksheets/sheet1.xml",sheet.toString());
            }else{
                StringBuilder doc=new StringBuilder("<w:document xmlns:w=\""+ns+"wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>BMD</w:t></w:r></w:p><w:tbl><w:tblPr><w:tblW w:w=\"0\" w:type=\"auto\"/></w:tblPr>");
                for(List<String> row:all){doc.append("<w:tr>");for(String cell:row)doc.append("<w:tc><w:p><w:r><w:t xml:space=\"preserve\">").append(xml(cell)).append("</w:t></w:r></w:p></w:tc>");doc.append("</w:tr>");}doc.append("</w:tbl><w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"900\" w:right=\"900\" w:bottom=\"900\" w:left=\"900\"/></w:sectPr></w:body></w:document>");entry(zip,"word/document.xml",doc.toString());
            }
        }return bytes.toByteArray();
    }
}
