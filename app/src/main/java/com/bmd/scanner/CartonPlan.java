package com.bmd.scanner;

/** Quantities are physical products, never code counts or a requirement to fill a carton. */
final class CartonPlan {
    final int total,capacity,boxes;final int[] quantities;
    CartonPlan(int total,int capacity){
        if(total<1||total>10000||(capacity!=113&&capacity!=125))throw new IllegalArgumentException("capacity");
        this.total=total;this.capacity=capacity;boxes=(total+capacity-1)/capacity;quantities=new int[boxes];
        for(int i=0;i<boxes;i++)quantities[i]=total/boxes+(i<total%boxes?1:0);
    }
    CartonPlan(int total,int capacity,int[] allocation){this(total,capacity);if(allocation==null||allocation.length!=boxes)throw new IllegalArgumentException("cartons");int sum=0;for(int i=0;i<boxes;i++){int q=allocation[i];if(q<1||q>capacity)throw new IllegalArgumentException("capacity");sum+=q;quantities[i]=q;}if(sum!=total)throw new IllegalArgumentException("total");}
    double average(){return (double)total/boxes;}
    int spare(){return boxes*capacity-total;}
    String dimensions(){return capacity==113?"70 × 50 × 30 cm":"80 × 50 × 30 cm";}
}
