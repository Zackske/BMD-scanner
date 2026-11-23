package com.bmd.scanner;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class CsvDataTest {
    @Test public void escapesQuotesNewlinesAndUnicodeWithoutChangingScanContents(){
        byte[] actual=CsvData.encode(Arrays.asList("BMD-123","a,\"b\"\nc","Україна","BMD-123"));
        assertEquals("\ufeff\"BMD-123\"\r\n\"a,\"\"b\"\"\nc\"\r\n\"Україна\"\r\n\"BMD-123\"\r\n",new String(actual,StandardCharsets.UTF_8));
        assertEquals((byte)0xef,actual[0]);assertEquals((byte)0xbb,actual[1]);assertEquals((byte)0xbf,actual[2]);
    }
    @Test public void emptyExportContainsOnlyUtf8Bom(){assertArrayEquals(new byte[]{(byte)0xef,(byte)0xbb,(byte)0xbf},CsvData.encode(Collections.emptyList()));}
}
