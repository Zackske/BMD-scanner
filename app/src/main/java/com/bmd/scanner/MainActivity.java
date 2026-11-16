package com.bmd.scanner;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
    private static final int BG=0xff0a1527, TILE=0xff172940, WHITE=Color.WHITE, BLUE=0xff23b9e8;
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
    private boolean scanning=false;
    private final ActivityResultLauncher<String> cameraPermission=registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted->{if(granted) showScanner();else Toast.makeText(this,t("permission"),Toast.LENGTH_LONG).show();});
    private final ActivityResultLauncher<String> saveCsv=registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri->{if(uri!=null) writeCsv(uri);});

    @Override public void onCreate(Bundle state){super.onCreate(state);lang=getPreferences(MODE_PRIVATE).getString("language","nl");scanner=BarcodeScanning.getClient();getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){@Override public void handleOnBackPressed(){showHome();}});showHome();}
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
    private void base(String title){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setPadding(dp(18),dp(22),dp(18),dp(12));setContentView(root);
        TextView heading=text(title,29,true);heading.setGravity(Gravity.CENTER_VERTICAL);root.addView(heading);
    }
    private void tile(String label,Runnable click){TextView view=text(label,19,true);view.setBackgroundColor(TILE);view.setPadding(dp(22),dp(23),dp(18),dp(23));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(12);root.addView(view,p);view.setOnClickListener(v->click.run());}
    private void stopCamera(){scanning=false;if(cameraProvider!=null)cameraProvider.unbindAll();}
    private void showHome(){stopCamera();base(t("title"));root.addView(text(t("subtitle"),16,false));tile("▣  "+t("scan"),()->{if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)showScanner();else cameraPermission.launch(Manifest.permission.CAMERA);});tile("▤  "+t("results")+" ("+results.size()+")",this::showResults);tile("⇩  "+t("export"),this::exportCsv);tile("✉  "+t("share"),this::shareCsv);tile("⚙  "+t("settings"),this::showSettings);}
    private void showSettings(){stopCamera();base(t("settings"));tile("‹  "+t("back"),this::showHome);root.addView(text(t("select"),18,true));String[] names={"Nederlands","English","Türkçe","Українська"};String[] codes={"nl","en","tr","uk"};for(int i=0;i<names.length;i++){final String c=codes[i];tile((lang.equals(c)?"●  ":"○  ")+names[i],()->{lang=c;getPreferences(MODE_PRIVATE).edit().putString("language",lang).apply();showSettings();});}}
    private void showResults(){stopCamera();base(t("results")+" ("+results.size()+")");tile("‹  "+t("back"),this::showHome);ScrollView scroll=new ScrollView(this);LinearLayout items=new LinearLayout(this);items.setOrientation(LinearLayout.VERTICAL);if(results.isEmpty())items.addView(text(t("empty"),16,false));for(int i=0;i<results.size();i++){TextView item=text((i+1)+".  "+results.get(i),15,false);item.setTextIsSelectable(true);items.addView(item);}scroll.addView(items);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));tile(t("export"),this::exportCsv);tile(t("share"),this::shareCsv);tile(t("clear"),()->new androidx.appcompat.app.AlertDialog.Builder(this).setMessage(t("confirm")).setPositiveButton(android.R.string.ok,(d,w)->{results.clear();showResults();}).setNegativeButton(android.R.string.cancel,null).show());}
    private void showScanner(){stopCamera();base(t("scan"));tile("‹  "+t("back"),this::showHome);preview=new PreviewView(this);preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,0,1);p.topMargin=dp(12);root.addView(preview,p);countText=text(t("results")+": "+results.size(),19,true);root.addView(countText);resultText=text("",14,false);resultText.setMaxLines(2);root.addView(resultText);tile(t("stop"),this::showHome);startCamera();}
    private void startCamera(){ListenableFuture<ProcessCameraProvider> future=ProcessCameraProvider.getInstance(this);future.addListener(()->{try{cameraProvider=future.get();if(preview==null)return;Preview previewUseCase=new Preview.Builder().build();previewUseCase.setSurfaceProvider(preview.getSurfaceProvider());ImageAnalysis analysis=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();analysis.setAnalyzer(cameraExecutor,this::analyze);cameraProvider.unbindAll();cameraProvider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,previewUseCase,analysis);scanning=true;}catch(Exception e){runOnUiThread(()->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());}},ContextCompat.getMainExecutor(this));}
    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void analyze(@NonNull ImageProxy proxy){if(!scanning||!processing.compareAndSet(false,true)){proxy.close();return;}android.media.Image image=proxy.getImage();if(image==null){processing.set(false);proxy.close();return;}InputImage input=InputImage.fromMediaImage(image,proxy.getImageInfo().getRotationDegrees());scanner.process(input).addOnSuccessListener(barcodes->{long now=SystemClock.elapsedRealtime();for(Barcode barcode:barcodes){if(barcode.getFormat()!=Barcode.FORMAT_QR_CODE)continue;String value=barcode.getRawValue();if(value==null)continue;Long previous=lastSeen.get(value);if(previous!=null&&now-previous<1500)continue;lastSeen.put(value,now);results.add(value);runOnUiThread(()->{if(countText!=null)countText.setText(t("results")+": "+results.size());if(resultText!=null)resultText.setText(value);});}}).addOnCompleteListener(task->{processing.set(false);proxy.close();});}
    private byte[] csvBytes(){StringBuilder b=new StringBuilder("\ufeff");for(String value:results){b.append('"').append(value.replace("\"","\"\"")).append('"').append("\r\n");}return b.toString().getBytes(StandardCharsets.UTF_8);}
    private void writeCsv(Uri uri){try(OutputStream stream=getContentResolver().openOutputStream(uri)){if(stream==null)throw new IOException("No output stream");stream.write(csvBytes());Toast.makeText(this,t("saved"),Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,t("error")+": "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    private void exportCsv(){if(results.isEmpty()){Toast.makeText(this,t("empty"),Toast.LENGTH_SHORT).show();return;}saveCsv.launch("BMD-Scanner-results.csv");}
    private void shareCsv(){if(results.isEmpty()){Toast.makeText(this,t("empty"),Toast.LENGTH_SHORT).show();return;}try{File directory=new File(getCacheDir(),"exports");if(!directory.exists()&&!directory.mkdirs())throw new IOException("Cannot create export directory");File file=new File(directory,"BMD-Scanner-results.csv");try(FileOutputStream stream=new FileOutputStream(file)){stream.write(csvBytes());}Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".provider",file);Intent intent=new Intent(Intent.ACTION_SEND);intent.setType("text/csv");intent.putExtra(Intent.EXTRA_STREAM,uri);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.setClipData(ClipData.newUri(getContentResolver(),"CSV",uri));startActivity(Intent.createChooser(intent,t("share")));}catch(Exception e){Toast.makeText(this,t("error")+": "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    @Override protected void onDestroy(){stopCamera();scanner.close();cameraExecutor.shutdown();super.onDestroy();}
}
