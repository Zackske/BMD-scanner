package com.bmd.scanner;
import org.junit.Test;
import static org.junit.Assert.*;
public class LotDataTest {
 @Test public void parsesHriAndRawGs1WithoutGuessing(){String gtin="08720634200066";LotData a=LotData.parse("(01)"+gtin+"(17)280418(10)2610143");assertEquals("2610143",a.lot);assertEquals(gtin,a.gtin);assertEquals("280418",a.expiry);LotData b=LotData.parse("]d201"+gtin+"17280418102610134\u001d21ABC");assertEquals("2610134",b.lot);assertEquals("",LotData.parse("2610143").lot);assertEquals("",LotData.parse("PRO2520 2610143").lot);assertEquals("",LotData.parse("(10)123456789012345678901").lot);}
}

