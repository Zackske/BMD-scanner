package com.bmd.scanner;

import android.graphics.Bitmap;
import android.graphics.Rect;
import androidx.camera.core.ImageProxy;
import java.util.*;
import zxingcpp.BarcodeReader;

/** General reader, independent of GS1/product interpretation and physical-product counting. */
final class GenericCodeReader {
    static final class Frame {
        final List<GenericCode> codes;final float[] focus;
        Frame(List<GenericCode> codes,float[] focus){this.codes=codes;this.focus=focus;}
    }
    static Set<BarcodeReader.Format> formats(){
        Set<BarcodeReader.Format> all=EnumSet.allOf(BarcodeReader.Format.class);all.remove(BarcodeReader.Format.NONE);
        return all;
    }
    private static BarcodeReader reader(int sensitivity){BarcodeReader.Options o=new BarcodeReader.Options();o.setFormats(formats());o.setTryHarder(sensitivity>0);o.setTryRotate(true);o.setTryInvert(true);o.setTryDownscale(true);o.setMinLineCount(3);o.setMaxNumberOfSymbols(255);o.setTextMode(BarcodeReader.TextMode.PLAIN);return new BarcodeReader(o);}
    static List<GenericCode> read(ImageProxy image,int sensitivity){return results(reader(sensitivity).read(image));}
    static Frame readFrame(ImageProxy image,int sensitivity){
        BarcodeReader decoder=reader(sensitivity);decoder.getOptions().setReturnErrors(true);
        List<BarcodeReader.Result> decoded=decoder.read(image);Rect crop=image.getCropRect();int rotation=image.getImageInfo().getRotationDegrees();
        int width=rotation%180==0?crop.width():crop.height(),height=rotation%180==0?crop.height():crop.width();
        float best=Float.MAX_VALUE;float[] focus=null;
        for(BarcodeReader.Result code:decoded){BarcodeReader.Position p=code.getPosition();
            float x=(p.getTopLeft().x+p.getTopRight().x+p.getBottomRight().x+p.getBottomLeft().x)/4f;
            float y=(p.getTopLeft().y+p.getTopRight().y+p.getBottomRight().y+p.getBottomLeft().y)/4f;
            float[] mapped=CodeFocusPoint.map(x,y,rotation,crop.left,crop.top,crop.width(),crop.height(),image.getWidth(),image.getHeight());
            float distance=(x/width-.5f)*(x/width-.5f)+(y/height-.5f)*(y/height-.5f);
            if(mapped!=null&&distance<best){best=distance;focus=mapped;}
        }
        // Locations with decoding errors can guide focus, but are never reported as read codes.
        return new Frame(results(decoded),focus);
    }
    static List<GenericCode> read(Bitmap bitmap){return results(reader(2).read(bitmap,new Rect(),0));}
    private static List<GenericCode> results(List<BarcodeReader.Result> decoded){Map<String,GenericCode> result=new LinkedHashMap<>();for(BarcodeReader.Result r:decoded){if(r.getError()!=null||r.getText()==null||r.getText().isEmpty())continue;GenericCode item=new GenericCode(r.getText(),r.getFormat().name().replace('_',' '));result.put(item.key(),item);}return new ArrayList<>(result.values());}
}
