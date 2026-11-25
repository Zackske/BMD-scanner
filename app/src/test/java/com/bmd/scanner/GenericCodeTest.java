package com.bmd.scanner;

import org.junit.Test;
import static org.junit.Assert.*;

public class GenericCodeTest {
    @Test public void onlyExplicitWebAddressesCanOpen(){
        assertEquals("https://example.org/product?lot=2610143",new GenericCode("https://example.org/product?lot=2610143","QR").webLink());
        assertEquals("http://example.org/",new GenericCode(" http://example.org/ ","QR").webLink());
        for(String payload:new String[]{"javascript:alert(1)","intent://test/#Intent;scheme=https;end","tel:+3112345678","mailto:test@example.org","WIFI:T:WPA;S:test;P:secret;;","file:///sdcard/private","https:///missing-host","https://good.example@evil.example/","https://example.org/\nnext","PRO2520 LOT 2610143","08720634200066"})assertEquals(payload,"",new GenericCode(payload,"QR").webLink());
    }
    @Test public void rawValuesAndFormatIdentityArePreserved(){String gs1="01087206342000661728041810ABC\u001d21SERIAL";GenericCode item=new GenericCode(gs1,"DATA MATRIX");assertEquals(gs1,item.value);assertNotEquals(new GenericCode("123","EAN 13").key(),new GenericCode("123","QR CODE").key());}
    @Test public void previewsKeepTheExactHostAndOnlyUseHttps(){
        GenericCode secure=new GenericCode("https://example.org:8443/product?lot=42","QR");assertTrue(secure.canPreview());assertEquals("example.org:8443",secure.webHost());
        GenericCode clear=new GenericCode("http://example.org/product","QR");assertFalse(clear.canPreview());assertEquals("http://example.org/product",clear.webLink());assertEquals("example.org",clear.webHost());
        for(String value:new String[]{"BMD-000123","https://example.org@other.example/","file:///private","javascript:alert(1)","data:text/html,test"}){GenericCode code=new GenericCode(value,"QR");assertFalse(code.canPreview());assertEquals("",code.webHost());}
    }
    @Test(expected=IllegalArgumentException.class)public void emptyDoesNotBecomeAResult(){new GenericCode("","QR");}
}
