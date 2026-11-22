package com.bmd.scanner;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.net.Uri;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Bundled, on-device OCR. Spatial word grouping preserves table columns on a page. */
final class PicklistReader {
    static PicklistData.Page recognize(Bitmap image)throws Exception{
        TextRecognizer reader=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        try{
            Text text=Tasks.await(reader.process(InputImage.fromBitmap(image,0)),60,TimeUnit.SECONDS);
            List<Text.Element> words=new ArrayList<>();for(Text.TextBlock block:text.getTextBlocks())for(Text.Line line:block.getLines())words.addAll(line.getElements());
            words.removeIf(e->e.getBoundingBox()==null);words.sort(Comparator.comparingDouble(e->e.getBoundingBox().centerY()));
            List<List<Text.Element>> bands=new ArrayList<>();float y=-10000,height=0;List<Text.Element> row=null;
            for(Text.Element e:words){Rect b=e.getBoundingBox();if(row==null||Math.abs(b.centerY()-y)>Math.max(height,b.height())*.52f){row=new ArrayList<>();bands.add(row);y=b.centerY();height=b.height();}row.add(e);}
            List<String> lines=new ArrayList<>();for(List<Text.Element> band:bands){band.sort(Comparator.comparingInt(e->e.getBoundingBox().left));StringBuilder s=new StringBuilder();for(Text.Element e:band){if(s.length()>0)s.append(' ');s.append(e.getText());}lines.add(s.toString());}
            MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] hash=md.digest(String.join("\n",lines).getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte b:hash)hex.append(String.format(Locale.ROOT,"%02x",b));return new PicklistData.Page(lines,hex.toString());
        }finally{reader.close();}
    }
    static List<PicklistData.Page> read(Context c,File file,String mime)throws Exception{
        List<PicklistData.Page> pages=new ArrayList<>();
        if(mime.equals("application/pdf")){
            try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer pdf=new PdfRenderer(fd)){
                if(pdf.getPageCount()>100)throw new IOException("100");
                for(int i=0;i<pdf.getPageCount();i++)try(PdfRenderer.Page p=pdf.openPage(i)){
                    float scale=Math.min(3f,2200f/Math.max(p.getWidth(),p.getHeight()));Bitmap b=Bitmap.createBitmap(Math.round(p.getWidth()*scale),Math.round(p.getHeight()*scale),Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);try{p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);pages.add(recognize(b));}finally{b.recycle();}
                }
            }
        }else{
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),bounds);if(bounds.outWidth<1)throw new IOException("Image");BitmapFactory.Options opts=new BitmapFactory.Options();while(Math.max(bounds.outWidth,bounds.outHeight)/Math.max(1,opts.inSampleSize)>3000)opts.inSampleSize=Math.max(1,opts.inSampleSize)*2;
            Bitmap b=BitmapFactory.decodeFile(file.getPath(),opts);if(b==null)throw new IOException("Image");Bitmap upright=b;try{ExifInterface exif=new ExifInterface(file);Matrix m=new Matrix();if(exif.isFlipped())m.postScale(-1,1);m.postRotate(exif.getRotationDegrees());upright=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true);pages.add(recognize(upright));}finally{if(upright!=b)upright.recycle();b.recycle();}
        }
        return pages;
    }
}
