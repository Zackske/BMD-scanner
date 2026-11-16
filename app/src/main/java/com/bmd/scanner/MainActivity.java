package com.bmd.scanner;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final int BG=0xff001027, TILE=0xff063568, WHITE=Color.WHITE, BLUE=0xff00eaff;
    private final ArrayList<String> results=new ArrayList<>();
    private final HashMap<String,Long> lastSeen=new HashMap<>();
    private final AtomicBoolean processing=new AtomicBoolean(false);
    private final ExecutorService cameraExecutor=Executors.newSingleThreadExecutor();
    private BarcodeScanner scanner;
    private ProcessCameraProvider cameraProvider;
    private PreviewView preview;
    private LinearLayout root;
    private TextView countText, resultText;
    private String lang="nl";
    private volatile boolean scanning=false;
    private volatile int cameraSession=0;
    private androidx.camera.core.Camera activeCamera;
    private boolean photoBusy=false;
    private final ActivityResultLauncher<String[]> pickPhotos=registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris->{if(!uris.isEmpty()) scanPhotos(new ArrayDeque<>(uris),0,0);});
    private final ActivityResultLauncher<String> cameraPermission=registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted->{if(granted) showScanner();else Toast.makeText(this,t("permission"),Toast.LENGTH_LONG).show();});
    private final ActivityResultLauncher<String> saveCsv=registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri->{if(uri!=null) writeCsv(uri);});

    @Override public void onCreate(Bundle state){super.onCreate(state);lang=getPreferences(MODE_PRIVATE).getString("language","nl");scanner=BarcodeScanning.getClient(new com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build());getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){@Override public void handleOnBackPressed(){showHome();}});showHome();}
    private String t(String key){
        String[] keys={"title","subtitle","scan","export","share","settings","language","back","clear","confirm","empty","permission","results","start","stop","saved","error","select"};
        String[][] values={
            {"BMD Scanner","Batch QR-scanner","Scannen","CSV exporteren","Outlook / Hotmail","Instellingen","Taal","Terug","Resultaten wissen","Alle resultaten wissen?","Nog geen scanresultaten","Cameratoegang nodig","Scanresultaten","Start camera","Stop camera","CSV opgeslagen","Export mislukt","Selecteer taal"},
            {"BMD Scanner","Batch QR scanner","Scan","Export CSV","Outlook / Hotmail","Settings","Language","Back","Clear results","Clear all results?","No scan results yet","Camera permission required","Scan results","Start camera","Stop camera","CSV saved","Export failed","Select language"},
            {"BMD Scanner","Toplu QR tarayıcı","Tara","CSV dışa aktar","Outlook / Hotmail","Ayarlar","Dil","Geri","Sonuçları sil","Tüm sonuçlar silinsin mi?","Henüz sonuç yok","Kamera izni gerekli","Tarama sonuçları","Kamerayı başlat","Kamerayı durdur","CSV kaydedildi","Dışa aktarma başarısız","Dil seçin"},
            {"BMD Scanner","Пакетний QR-сканер","Сканувати","Експорт CSV","Outlook / Hotmail","Налаштування","Мова","Назад","Очистити результати","Видалити всі результати?","Результатів поки немає","Потрібен дозвіл на камеру","Результати сканування","Увімкнути камеру","Зупинити камеру","CSV збережено","Помилка експорту","Виберіть мову"}
        };
        int index=Arrays.asList(keys).indexOf(key);if(index<0)return key;
        int locale=lang.equals("en")?1:lang.equals("tr")?2:lang.equals("uk")?3:0;
        return values[locale][index];
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String value,int size,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextColor(WHITE);v.setTextSize(size);if(bold)v.setTypeface(null,Typeface.BOLD);v.setPadding(dp(4),dp(7),dp(4),dp(7));return v;}
    private GradientDrawable glow(int top,int bottom){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{top,bottom,0xff00132d});g.setCornerRadius(dp(20));g.setStroke(dp(2),0xff16dfff);return g;}
    private void base(String title){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff000716,0xff03417b,0xff000716}));root.setPadding(dp(18),dp(22),dp(18),dp(12));setContentView(root);
        TextView heading=text(title,29,true);heading.setGravity(Gravity.CENTER_VERTICAL);root.addView(heading);
    }
    private void tile(String label,Runnable click){TextView view=text(label,19,true);view.setBackground(glow(0xff087bc7,0xff00204a));view.setPadding(dp(22),dp(20),dp(18),dp(20));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(12);root.addView(view,p);view.setOnClickListener(v->click.run());}
    private void stopCamera(){scanning=false;cameraSession++;preview=null;activeCamera=null;if(cameraProvider!=null)cameraProvider.unbindAll();}
    private void colored(LinearLayout parent,String icon,String label,String detail,int top,int bottom,Runnable action){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER);card.setBackground(glow(top,bottom));card.setPadding(dp(7),dp(10),dp(7),dp(10));TextView symbol=text(icon,31,true);symbol.setGravity(Gravity.CENTER);card.addView(symbol);TextView title=text(label,19,true);title.setGravity(Gravity.CENTER);card.addView(title);TextView sub=text(detail,12,false);sub.setGravity(Gravity.CENTER);card.addView(sub);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(160),1);p.setMargins(dp(4),dp(5),dp(4),dp(5));parent.addView(card,p);card.setOnClickListener(v->action.run());}
    private void showHome(){stopCamera();base("");root.removeAllViews();TextView brand=text("BLUE MEDICAL",32,true);brand.setTextColor(0xff0647a8);brand.setGravity(Gravity.CENTER);brand.setBackground(glow(0xffefffff,0xff60a9e8));root.addView(brand);TextView sub=text("A Translumina Group Company",13,false);sub.setGravity(Gravity.CENTER);root.addView(sub);TextView logo=text("▦  BMD",45,true);logo.setGravity(Gravity.CENTER);root.addView(logo);TextView tag=text("Multi QR Scanner",25,true);tag.setTextColor(BLUE);tag.setGravity(Gravity.CENTER);root.addView(tag);TextView hint=text(t("subtitle"),16,false);hint.setGravity(Gravity.CENTER);root.addView(hint);LinearLayout r1=new LinearLayout(this);root.addView(r1);colored(r1,"▦",t("scan")+" (Batch)","Meerdere QR-codes achter elkaar",0xff00c9ff,0xff0040bc,()->{if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)showScanner();else cameraPermission.launch(Manifest.permission.CAMERA);});colored(r1,"☷",t("results"),"Bekijk scanresultaten",0xffe640f9,0xff64008f,this::showResults);LinearLayout r2=new LinearLayout(this);root.addView(r2);colored(r2,"▤","CSV Export","Exporteer resultaten",0xff00ed75,0xff007b35,this::exportCsv);colored(r2,"✉","E-mail (Outlook)","Outlook / Hotmail",0xffffbb13,0xffb64c00,this::shareCsv);tile("▧  "+photoText("choose"),this::choosePhotos);tile("⚙  "+t("settings"),this::showSettings);ScrollView homeScroll=new ScrollView(this);((ViewGroup)root.getParent()).removeView(root);homeScroll.addView(root);setContentView(homeScroll);}
    private void showSettings(){stopCamera();base(t("settings"));tile("‹  "+t("back"),this::showHome);tile("🌐  Taal / Language",()->{String[] names={"Nederlands","English","Türkçe","Українська"};String[] codes={"nl","en","tr","uk"};new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Taal / Language").setItems(names,(dialog,i)->{lang=codes[i];getPreferences(MODE_PRIVATE).edit().putString("language",lang).apply();showSettings();}).show();});tile("▦  Scanner — batchmodus",()->Toast.makeText(this,"QR batchscanner",Toast.LENGTH_SHORT).show());tile("▤  CSV-instellingen — UTF-8",()->Toast.makeText(this,"Alleen resultaten, één per regel",Toast.LENGTH_SHORT).show());tile("✉  E-mail (Outlook)",this::shareCsv);tile("◉  Weergave — Blue Medical",()->Toast.makeText(this,"Blue Medical thema",Toast.LENGTH_SHORT).show());TextView brand=text("BLUE MEDICAL",30,true);brand.setGravity(Gravity.CENTER);root.addView(brand);}
    private void showResults(){stopCamera();base(t("results")+" ("+results.size()+")");tile("‹  "+t("back"),this::showHome);ScrollView scroll=new ScrollView(this);LinearLayout items=new LinearLayout(this);items.setOrientation(LinearLayout.VERTICAL);if(results.isEmpty())items.addView(text(t("empty"),16,false));for(int i=0;i<results.size();i++){TextView item=text((i+1)+".  "+results.get(i),15,false);item.setTextIsSelectable(true);items.addView(item);}scroll.addView(items);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));tile(t("export"),this::exportCsv);tile(t("share"),this::shareCsv);tile(t("clear"),()->new androidx.appcompat.app.AlertDialog.Builder(this).setMessage(t("confirm")).setPositiveButton(android.R.string.ok,(d,w)->{results.clear();showResults();}).setNegativeButton(android.R.string.cancel,null).show());}
    private void showScanner(){stopCamera();base("▦  Scannen (Batch)");preview=new PreviewView(this);preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);android.widget.FrameLayout cameraFrame=new android.widget.FrameLayout(this);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,0,1);p.topMargin=dp(12);root.addView(cameraFrame,p);cameraFrame.addView(preview,new android.widget.FrameLayout.LayoutParams(-1,-1));android.view.View overlay=new android.view.View(this){android.graphics.Paint paint=new android.graphics.Paint(3);protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight(),left=w*.17f,right=w*.83f,top=h*.2f,bottom=h*.8f,len=dp(42);paint.setColor(0xff00eaff);paint.setStrokeWidth(dp(6));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setStyle(android.graphics.Paint.Style.STROKE);android.graphics.Path path=new android.graphics.Path();path.moveTo(left,top+len);path.lineTo(left,top);path.lineTo(left+len,top);path.moveTo(right-len,top);path.lineTo(right,top);path.lineTo(right,top+len);path.moveTo(left,bottom-len);path.lineTo(left,bottom);path.lineTo(left+len,bottom);path.moveTo(right-len,bottom);path.lineTo(right,bottom);path.lineTo(right,bottom-len);canvas.drawPath(path,paint);paint.setColor(0xff00ff65);paint.setStrokeWidth(dp(3));canvas.drawLine(left, h*.5f,right,h*.5f,paint);}};cameraFrame.addView(overlay,new android.widget.FrameLayout.LayoutParams(-1,-1));countText=text("✔  Gescand: "+results.size(),23,true);countText.setBackground(glow(0xff06558e,0xff00142e));root.addView(countText);resultText=text("",14,false);resultText.setMaxLines(3);root.addView(resultText);LinearLayout buttons=new LinearLayout(this);buttons.setGravity(Gravity.CENTER);root.addView(buttons);TextView gallery=text("▧\n"+photoText("photo"),14,true);buttons.addView(gallery,new LinearLayout.LayoutParams(0,dp(78),1));gallery.setOnClickListener(v->choosePhotos());TextView stop=text("■  "+t("stop"),24,true);stop.setGravity(Gravity.CENTER);stop.setBackground(glow(0xffff4167,0xffaa001b));buttons.addView(stop,new LinearLayout.LayoutParams(0,dp(78),2));stop.setOnClickListener(v->showHome());TextView list=text("☷",26,true);list.setGravity(Gravity.CENTER);buttons.addView(list,new LinearLayout.LayoutParams(0,dp(78),1));list.setOnClickListener(v->showResults());startCamera();}
    private String photoText(String key){
        String[] codes={"nl","en","tr","uk"};
        int locale=Arrays.asList(codes).indexOf(lang);if(locale<0)locale=0;
        switch(key){
            case "choose":return new String[]{"Foto’s kiezen","Choose photos","Fotoğraf seç","Вибрати фото"}[locale];
            case "photo":return new String[]{"Foto’s","Photos","Fotoğraf","Фото"}[locale];
            case "busy":return new String[]{"Foto’s verwerken…","Reading photos…","Fotoğraflar okunuyor…","Обробка фото…"}[locale];
            case "none":return new String[]{"Geen QR-code gevonden. Kies een scherpe foto waarop de volledige code zichtbaar is.","No QR code found. Choose a sharp photo showing the entire code.","QR kod bulunamadı. Kodun tamamını gösteren net bir fotoğraf seçin.","QR-код не знайдено. Виберіть чітке фото з повним кодом."}[locale];
            case "failed":return new String[]{"Foto’s niet leesbaar","Photos could not be read","Fotoğraflar okunamadı","Не вдалося прочитати фото"}[locale];
            default:return new String[]{"Scannerfout","Scanner error","Tarayıcı hatası","Помилка сканера"}[locale];
        }
    }
    private void choosePhotos(){
        if(photoBusy){Toast.makeText(this,photoText("busy"),Toast.LENGTH_SHORT).show();return;}
        showHome();pickPhotos.launch(new String[]{"image/*"});
    }
    private void scanPhotos(ArrayDeque<Uri> queue,int found,int failed){
        if(isDestroyed())return;
        photoBusy=true;
        if(queue.isEmpty()){
            photoBusy=false;showResults();
            String message=found==0?photoText("none"):t("results")+": +"+found;
            if(failed>0)message+="\n"+photoText("failed")+": "+failed;
            new androidx.appcompat.app.AlertDialog.Builder(this).setMessage(message).setPositiveButton(android.R.string.ok,null).show();
            return;
        }
        Uri uri=queue.removeFirst();
        cameraExecutor.execute(()->{
            try{
                InputImage input=InputImage.fromFilePath(this,uri);
                scanner.process(input).addOnSuccessListener(barcodes->{
                    int added=0;
                    for(Barcode code:barcodes){String value=code.getRawValue();if(code.getFormat()==Barcode.FORMAT_QR_CODE&&value!=null){results.add(value);added++;}}
                    scanPhotos(queue,found+added,failed);
                }).addOnFailureListener(error->scanPhotos(queue,found,failed+1));
            }catch(Exception error){runOnUiThread(()->scanPhotos(queue,found,failed+1));}
        });
    }
    private void startCamera(){
        final int session=cameraSession;
        final PreviewView cameraPreview=preview;
        ListenableFuture<ProcessCameraProvider> future=ProcessCameraProvider.getInstance(this);
        future.addListener(()->{
            if(session!=cameraSession||cameraPreview==null||isDestroyed())return;
            try{
                cameraProvider=future.get();
                Preview previewUseCase=new Preview.Builder().build();
                previewUseCase.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                androidx.camera.core.resolutionselector.ResolutionSelector resolution=new androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                    .setResolutionStrategy(new androidx.camera.core.resolutionselector.ResolutionStrategy(new android.util.Size(1280,720),androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build();
                ImageAnalysis analysis=new ImageAnalysis.Builder().setResolutionSelector(resolution)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                analysis.setAnalyzer(cameraExecutor,proxy->analyze(proxy,session));
                cameraProvider.unbindAll();
                activeCamera=cameraProvider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,previewUseCase,analysis);
                scanning=true;
                cameraPreview.setOnTouchListener((view,event)->{
                    if(event.getAction()==android.view.MotionEvent.ACTION_UP&&activeCamera!=null){
                        FocusMeteringAction focus=new FocusMeteringAction.Builder(cameraPreview.getMeteringPointFactory().createPoint(event.getX(),event.getY())).build();
                        activeCamera.getCameraControl().startFocusAndMetering(focus);
                        view.performClick();
                    }
                    return true;
                });
                resultText.setText(lang.equals("nl")?"Richt op de volledige QR-code. Tik op het beeld om scherp te stellen.":"Point at the entire QR code. Tap the preview to focus.");
            }catch(Exception error){resultText.setText(photoText("error")+": "+error.getMessage());}
        },ContextCompat.getMainExecutor(this));
    }
    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void analyze(@NonNull ImageProxy proxy,int session){
        if(!scanning||session!=cameraSession||!processing.compareAndSet(false,true)){proxy.close();return;}
        try{
            android.media.Image image=proxy.getImage();
            if(image==null){processing.set(false);proxy.close();return;}
            InputImage input=InputImage.fromMediaImage(image,proxy.getImageInfo().getRotationDegrees());
            scanner.process(input).addOnSuccessListener(barcodes->{
                if(!scanning||session!=cameraSession)return;
                long now=SystemClock.elapsedRealtime();
                for(Barcode barcode:barcodes){
                    if(barcode.getFormat()!=Barcode.FORMAT_QR_CODE)continue;
                    String value=barcode.getRawValue();if(value==null)continue;
                    Long previous=lastSeen.get(value);if(previous!=null&&now-previous<1500)continue;
                    lastSeen.put(value,now);results.add(value);
                    countText.setText("✔  "+t("results")+": "+results.size());resultText.setText(value);
                }
            }).addOnFailureListener(error->{if(scanning&&session==cameraSession)resultText.setText(photoText("error")+": "+error.getMessage());})
              .addOnCompleteListener(task->{processing.set(false);proxy.close();});
        }catch(Exception error){
            processing.set(false);proxy.close();
            runOnUiThread(()->{if(scanning&&session==cameraSession)resultText.setText(photoText("error")+": "+error.getMessage());});
        }
    }
    private byte[] csvBytes(){StringBuilder b=new StringBuilder("\ufeff");for(String value:results){b.append('"').append(value.replace("\"","\"\"")).append('"').append("\r\n");}return b.toString().getBytes(StandardCharsets.UTF_8);}
    private void writeCsv(Uri uri){try(OutputStream stream=getContentResolver().openOutputStream(uri)){if(stream==null)throw new IOException("No output stream");stream.write(csvBytes());Toast.makeText(this,t("saved"),Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,t("error")+": "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    private void exportCsv(){if(results.isEmpty()){Toast.makeText(this,t("empty"),Toast.LENGTH_SHORT).show();return;}saveCsv.launch("BMD-Scanner-results.csv");}
    private void shareCsv(){if(results.isEmpty()){Toast.makeText(this,t("empty"),Toast.LENGTH_SHORT).show();return;}try{File directory=new File(getCacheDir(),"exports");if(!directory.exists()&&!directory.mkdirs())throw new IOException("Cannot create export directory");File file=new File(directory,"BMD-Scanner-results.csv");try(FileOutputStream stream=new FileOutputStream(file)){stream.write(csvBytes());}Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".provider",file);Intent intent=new Intent(Intent.ACTION_SEND);intent.setType("text/csv");intent.putExtra(Intent.EXTRA_STREAM,uri);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.setClipData(ClipData.newUri(getContentResolver(),"CSV",uri));startActivity(Intent.createChooser(intent,t("share")));}catch(Exception e){Toast.makeText(this,t("error")+": "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    @Override protected void onDestroy(){stopCamera();scanner.close();cameraExecutor.shutdown();super.onDestroy();}
}
