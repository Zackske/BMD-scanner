package com.bmd.scanner;
import org.junit.Test;
import static org.junit.Assert.*;
public class CartonPlanTest {
    @Test public void pickingHasOnePassWithoutChangingPhysicalCartonLimits(){CartonPlan picking=CartonPlan.picking(200);assertEquals(1,picking.boxes);assertArrayEquals(new int[]{200},picking.quantities);assertEquals(2,new CartonPlan(200,113).boxes);assertEquals(2,new CartonPlan(200,125).boxes);}
    @Test public void realCapacitiesAndBalancedExample(){for(int capacity:new int[]{113,125}){CartonPlan p=new CartonPlan(556,capacity);assertEquals(5,p.boxes);assertEquals(111.2,p.average(),.00001);assertArrayEquals(new int[]{112,111,111,111,111},p.quantities);}assertEquals(1,new CartonPlan(28,113).boxes);assertEquals(2,new CartonPlan(114,113).boxes);assertEquals(1,new CartonPlan(125,125).boxes);assertEquals(9,new CartonPlan(556,113).spare());}
    @Test public void everySupportedQuantityIsPreservedWithoutOverflow(){for(int capacity:new int[]{113,125})for(int total=1;total<=10000;total++){CartonPlan p=new CartonPlan(total,capacity);int sum=0;for(int n:p.quantities){assertTrue(n>0&&n<=capacity);sum+=n;}assertEquals(total,sum);assertTrue(p.quantities[0]-p.quantities[p.boxes-1]<=1);assertTrue((p.boxes-1)*capacity<total);}}
    @Test(expected=IllegalArgumentException.class)public void zeroIsNotAPhysicalOrder(){new CartonPlan(0,113);}
    @Test public void alreadyPackedCartonsCanKeepTheirRealQuantities(){assertArrayEquals(new int[]{113,113,113,113,104},new CartonPlan(556,113,new int[]{113,113,113,113,104}).quantities);}
    @Test(expected=IllegalArgumentException.class)public void editedAllocationMustMatchPicklist(){new CartonPlan(556,113,new int[]{113,111,111,111,111});}
    @Test public void pickingKeepsTwoHundredProductsInOneControlWithoutChangingBoxCapacity(){CartonPlan picking=CartonPlan.picking(200);assertEquals(1,picking.boxes);assertArrayEquals(new int[]{200},picking.quantities);assertEquals(2,new CartonPlan(200,113).boxes);}
}
