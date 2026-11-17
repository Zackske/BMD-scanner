package com.bmd.scanner;
import org.junit.Test;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import javax.xml.parsers.DocumentBuilderFactory;
import static org.junit.Assert.*;
public class DocumentDataTest {
    private List<ScanTracker.Item> rows(){ScanTracker t=new ScanTracker();for(String value:new String[]{"00123","00123","=SUM(A1)","Україна हिन्दी & <é>\n\"x\""})t.importCode(new ScanTracker.Detection(value,"QR",0,0,1,1),0);return t.items();}
    @Test public void csvKeepsDuplicateRowsUnicodeAndNeutralizesSpreadsheetFormulas(){String text=new String(DocumentData.csv(rows(),new String[]{"Nr.","Type","Code"}),StandardCharsets.UTF_8);assertTrue(text.startsWith("\ufeff"));assertTrue(text.contains("\"1\",\"QR\",\"00123\""));assertTrue(text.contains("\"2\",\"QR\",\"00123\""));assertTrue(text.contains("'=SUM(A1)"));assertTrue(text.contains("हिन्दी"));assertTrue(text.contains("\"\"x\"\""));}
    @Test public void officeFilesAreRealZipPackagesWithWellFormedXmlAndTextCells()throws Exception{for(boolean excel:new boolean[]{false,true}){byte[] data=DocumentData.office(rows(),new String[]{"Nr.","Type","Code"},excel);Map<String,String> files=new HashMap<>();try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(data))){ZipEntry e;while((e=in.getNextEntry())!=null){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)b.write(buf,0,n);byte[] xml=b.toByteArray();DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));files.put(e.getName(),new String(xml,StandardCharsets.UTF_8));}}assertTrue(files.containsKey("[Content_Types].xml"));String body=files.get(excel?"xl/worksheets/sheet1.xml":"word/document.xml");assertTrue(body.contains("00123"));assertTrue(body.contains("हिन्दी"));assertTrue(body.contains("&amp;"));if(excel){assertTrue(body.contains("t=\"inlineStr\""));assertFalse(body.contains("<f>"));}}}
}
