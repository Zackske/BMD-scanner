package com.bmd.scanner;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ProductCatalogTest {
 private ScanTracker.Item code(int id,String lot,String kind){ScanTracker.Item row=new ScanTracker.Item(id,new ScanTracker.Detection("(01)08720634200066(17)280418(10)"+lot,kind,.1f,id*.003f,.2f,id*.003f+.002f),1);row.confirmed=true;return row;}
 @Test public void mixedLotsCountThirteenSeparateProducts(){List<ScanTracker.Item> rows=new ArrayList<>();for(int i=1;i<=13;i++)rows.add(code(i,Arrays.asList(2,6,11).contains(i)?"2026144":Arrays.asList(4,9).contains(i)?"2610134":"2610143","Data Matrix"));ProductCatalog c=new ProductCatalog();assertEquals(13,c.project(rows).size());assertEquals(Integer.valueOf(8),c.lotCounts(rows).get("2610143"));assertEquals(Integer.valueOf(2),c.lotCounts(rows).get("2610134"));assertEquals(Integer.valueOf(3),c.lotCounts(rows).get("2026144"));assertNotEquals(c.colour("2610143"),c.colour("2610134"));}
 @Test public void identicalSymbolsStaySeparateUntilOperatorGroupsOneBox(){List<ScanTracker.Item> rows=Arrays.asList(code(1,"2610143","Data Matrix"),code(2,"2610143","Data Matrix"),code(3,"2610143","Barcode"),code(4,"2610143","Data Matrix"));ProductCatalog c=new ProductCatalog();assertEquals(4,c.view(rows).size());c.merge(Arrays.asList(1,2,3),rows);assertEquals(2,c.view(rows).size());assertEquals(3,c.view(rows).get(0).codes.size());assertEquals(2,c.exportRows(rows).size());c.split(Collections.singleton(1),rows);assertEquals(4,c.view(rows).size());}
 @Test public void refusesMergingDifferentLots(){ProductCatalog c=new ProductCatalog();try{c.merge(Arrays.asList(1,2),Arrays.asList(code(1,"2610143","Data Matrix"),code(2,"2610134","Barcode")));fail();}catch(IllegalArgumentException expected){assertTrue(c.groups.isEmpty());}}
 @Test public void twoHundredIdenticalProductsAreNotDeduplicated(){List<ScanTracker.Item> rows=new ArrayList<>();for(int i=1;i<=200;i++)rows.add(code(i,"2610143","Data Matrix"));ProductCatalog c=new ProductCatalog();assertEquals(200,c.project(rows).size());assertEquals(Integer.valueOf(200),c.lotCounts(rows).get("2610143"));assertEquals(200,c.exportRows(rows).size());}
}

