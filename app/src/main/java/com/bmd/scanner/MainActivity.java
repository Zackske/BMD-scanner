package com.bmd.scanner;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
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
    private String screen="home";
    private boolean torch=false;
    private byte[] pendingCsv;
    private TextView busyText;
    private long repeatDelay=1500;
    private String csvFilename="BMD-Scanner-results.csv";
    private final ActivityResultLauncher<String[]> pickPhotos=registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris->{if(!uris.isEmpty()){showHome();scanPhotos(new ArrayDeque<>(uris),0,0);}});
    private final ActivityResultLauncher<String> cameraPermission=registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted->{if(granted) showScanner();else Toast.makeText(this,t("permission"),Toast.LENGTH_LONG).show();});
    private final ActivityResultLauncher<String> saveCsv=registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri->{if(uri!=null)writeCsv(uri);else pendingCsv=null;});

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(false);
        lang=getPreferences(MODE_PRIVATE).getString("language","nl");
        repeatDelay=getPreferences(MODE_PRIVATE).getLong("repeatDelay",1500);
        csvFilename=getPreferences(MODE_PRIVATE).getString("csvFilename","BMD-Scanner-results.csv");
        if(state!=null){ArrayList<String> saved=state.getStringArrayList("results");if(saved!=null)results.addAll(saved);pendingCsv=state.getByteArray("pendingCsv");}
        scanner=BarcodeScanning.getClient(new com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build());
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){@Override public void handleOnBackPressed(){if(screen.equals("home"))finish();else showHome();}});
        showHome();
        if(state!=null&&"results".equals(state.getString("screen")))showResults();
    }
    @Override protected void onSaveInstanceState(@NonNull Bundle state){
        state.putStringArrayList("results",new ArrayList<>(results));state.putString("screen",screen);state.putByteArray("pendingCsv",pendingCsv);super.onSaveInstanceState(state);
    }
    private String tr(String nl,String en,String turkish,String uk){return lang.equals("en")?en:lang.equals("tr")?turkish:lang.equals("uk")?uk:nl;}
    private String t(String key){
        String[] keys={"title","subtitle","scan","export","share","settings","language","back","clear","confirm","empty","permission","results","start","stop","saved","error","select"};
        String[][] values={
            {"BMD Scanner","Batch QR-scanner","Scannen","CSV maken","Verzenden","Instellingen","Taal","Terug","Resultaten wissen","Alle resultaten wissen?","Nog geen scanresultaten","Cameratoegang nodig","Resultaten","Start camera","Stop camera","CSV opgeslagen","Export mislukt","Selecteer taal"},
            {"BMD Scanner","Batch QR scanner","Scan","Create CSV","Send","Settings","Language","Back","Clear results","Clear all results?","No scan results yet","Camera permission required","Scan results","Start camera","Stop camera","CSV saved","Export failed","Select language"},
            {"BMD Scanner","Toplu QR tarayıcı","Tara","CSV oluştur","Gönder","Ayarlar","Dil","Geri","Sonuçları sil","Tüm sonuçlar silinsin mi?","Henüz sonuç yok","Kamera izni gerekli","Tarama sonuçları","Kamerayı başlat","Kamerayı durdur","CSV kaydedildi","Dışa aktarma başarısız","Dil seçin"},
            {"BMD Scanner","Пакетний QR-сканер","Сканувати","Створити CSV","Надіслати","Налаштування","Мова","Назад","Очистити результати","Видалити всі результати?","Результатів поки немає","Потрібен дозвіл на камеру","Результати сканування","Увімкнути камеру","Зупинити камеру","CSV збережено","Помилка експорту","Виберіть мову"}
        };
        int index=Arrays.asList(keys).indexOf(key);if(index<0)return key;
        int locale=lang.equals("en")?1:lang.equals("tr")?2:lang.equals("uk")?3:0;
        return values[locale][index];
    }
    private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String value,int size,boolean bold){
        TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(WHITE);
        view.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));view.setIncludeFontPadding(false);return view;
    }
    private LinearLayout column(){LinearLayout view=new LinearLayout(this);view.setOrientation(LinearLayout.VERTICAL);return view;}
    private LinearLayout row(){LinearLayout view=new LinearLayout(this);view.setGravity(Gravity.CENTER_VERTICAL);return view;}
    private void gap(LinearLayout parent,int height){parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height)));}
    private Drawable card(int top,int bottom,int edge){return UiArt.card(top,bottom,edge,dp(22));}
    private void clickable(View view,String label,Runnable action){
        view.setContentDescription(label);view.setFocusable(true);view.setClickable(true);
        android.util.TypedValue value=new android.util.TypedValue();getTheme().resolveAttribute(android.R.attr.selectableItemBackground,value,true);
        view.setForeground(ContextCompat.getDrawable(this,value.resourceId));view.setOnClickListener(v->{if(!photoBusy)action.run();else Toast.makeText(this,photoText("busy"),Toast.LENGTH_SHORT).show();});
    }
    private View iconButton(String icon,String label,Runnable action){
        FrameLayout frame=new FrameLayout(this);frame.setMinimumWidth(dp(48));frame.setMinimumHeight(dp(48));
        UiArt.Icon art=new UiArt.Icon(this,icon);FrameLayout.LayoutParams artParams=new FrameLayout.LayoutParams(dp(28),dp(28),Gravity.CENTER);frame.addView(art,artParams);clickable(frame,label,action);return frame;
    }
    private void base(String title){
        FrameLayout shell=new FrameLayout(this);shell.setBackgroundColor(0xff001127);shell.addView(new UiArt.Backdrop(this),new FrameLayout.LayoutParams(-1,-1));
        root=column();root.setPadding(dp(18),dp(8),dp(18),dp(22));shell.addView(root,new FrameLayout.LayoutParams(-1,-1));
        ViewCompat.setOnApplyWindowInsetsListener(shell,(view,insets)->{androidx.core.graphics.Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());view.setPadding(safe.left,safe.top,safe.right,safe.bottom);return WindowInsetsCompat.CONSUMED;});
        setContentView(shell);ViewCompat.requestApplyInsets(shell);
        if(!title.isEmpty()){
            LinearLayout header=row();header.addView(iconButton("back",t("back"),this::showHome),new LinearLayout.LayoutParams(dp(48),dp(52)));
            TextView heading=text(title,25,true);header.addView(heading,new LinearLayout.LayoutParams(0,dp(52),1));heading.setGravity(Gravity.CENTER_VERTICAL);root.addView(header);
            View line=new View(this);line.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0x0000dcff,BLUE,0x0000dcff}));root.addView(line,new LinearLayout.LayoutParams(-1,dp(2)));gap(root,14);
        }
    }
    private void stopCamera(){scanning=false;cameraSession++;preview=null;activeCamera=null;torch=false;if(cameraProvider!=null)cameraProvider.unbindAll();}
    private void scrollPage(LinearLayout content){ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));}
    private void homeCard(LinearLayout parent,String icon,String title,String detail,int top,int bottom,int edge,Runnable action){
        LinearLayout button=column();button.setGravity(Gravity.CENTER);button.setPadding(dp(12),dp(16),dp(12),dp(16));button.setBackground(card(top,bottom,edge));
        UiArt.Icon art=new UiArt.Icon(this,icon);button.addView(art,new LinearLayout.LayoutParams(dp(48),dp(48)));gap(button,10);
        TextView label=text(title,21,true);label.setGravity(Gravity.CENTER);button.addView(label);
        if(!detail.isEmpty()){gap(button,6);TextView hint=text(detail,13,false);hint.setGravity(Gravity.CENTER);hint.setTextColor(0xffe3f4ff);button.addView(hint);}
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-1,1);params.setMargins(dp(5),dp(5),dp(5),dp(5));parent.addView(button,params);clickable(button,title,action);button.setTag(icon);
    }
    private void showHome(){
        stopCamera();screen="home";base("");LinearLayout content=column();
        content.addView(new UiArt.Brand(this),new LinearLayout.LayoutParams(-1,dp(112)));gap(content,12);
        LinearLayout hero=row();hero.setGravity(Gravity.CENTER);hero.addView(new UiArt.Icon(this,"qr"),new LinearLayout.LayoutParams(dp(62),dp(62)));
        LinearLayout names=column();names.setPadding(dp(12),0,0,0);TextView name=text("BMD",46,true);name.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD_ITALIC));names.addView(name);
        TextView subtitle=text("Multi QR Scanner",20,true);subtitle.setTextColor(BLUE);names.addView(subtitle);hero.addView(names);content.addView(hero);gap(content,14);
        int height=getResources().getConfiguration().screenHeightDp<700?152:176;
        LinearLayout first=row();content.addView(first,new LinearLayout.LayoutParams(-1,dp(height)));
        homeCard(first,"qr",t("scan"),"Batch",0xff0098fa,0xff003aa0,BLUE,this::openCamera);
        homeCard(first,"photo",photoText("choose"),"",0xff00d960,0xff006525,0xff2eff8b,this::choosePhotos);
        LinearLayout second=row();content.addView(second,new LinearLayout.LayoutParams(-1,dp(height)));
        homeCard(second,"list",t("results"),"",0xffc62dff,0xff4a007e,0xffec91ff,this::showResults);
        homeCard(second,"settings",t("settings"),"",0xffffb800,0xffa54300,0xffffdf63,this::showSettings);
        busyText=text("",14,false);busyText.setGravity(Gravity.CENTER);content.addView(busyText);scrollPage(content);
    }
    private void openCamera(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)showScanner();else cameraPermission.launch(Manifest.permission.CAMERA);}
    private void setting(LinearLayout parent,String icon,String title,String subtitle,int color,Runnable action){
        LinearLayout entry=row();entry.setPadding(dp(12),dp(14),dp(12),dp(14));entry.setBackground(card(color,0xff00152e,0xff008edc));
        entry.addView(new UiArt.Icon(this,icon),new LinearLayout.LayoutParams(dp(40),dp(40)));LinearLayout labels=column();labels.setPadding(dp(12),0,dp(4),0);labels.addView(text(title,20,true));gap(labels,6);TextView description=text(subtitle,13,false);description.setTextColor(0xffcfdfef);labels.addView(description);entry.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        if(action!=null){entry.addView(text("›",30,false));clickable(entry,title,action);}parent.addView(entry,new LinearLayout.LayoutParams(-1,-2));gap(parent,14);
    }
    private void showSettings(){
        stopCamera();screen="settings";base(t("settings"));LinearLayout content=column();
        setting(content,"language","Taal / Language","Nederlands, English, Türkçe, Українська",0xff006fe6,()->{
            String[] names={"Nederlands","English","Türkçe","Українська"},codes={"nl","en","tr","uk"};
            new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(t("language")).setSingleChoiceItems(names,Arrays.asList(codes).indexOf(lang),(dialog,which)->{lang=codes[which];getPreferences(MODE_PRIVATE).edit().putString("language",lang).apply();dialog.dismiss();showSettings();}).setNegativeButton(android.R.string.cancel,null).show();
        });
        setting(content,"qr",tr("Scanner","Scanner","Tarayıcı","Сканер"),tr("Tijd tussen herhaalde codes: ","Repeat interval: ","Tekrar aralığı: ","Інтервал повторення: ")+(repeatDelay/1000.0)+" s",0xff064a77,()->{
            String[] labels={"1.5 s","3 s","5 s"};long[] delays={1500,3000,5000};
            new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(t("scan")).setItems(labels,(dialog,which)->{repeatDelay=delays[which];getPreferences(MODE_PRIVATE).edit().putLong("repeatDelay",repeatDelay).apply();showSettings();}).show();
        });
        setting(content,"csv",tr("CSV-instellingen","CSV settings","CSV ayarları","Налаштування CSV"),csvFilename,0xff006d38,()->{
            EditText name=new EditText(this);name.setSingleLine(true);name.setText(csvFilename);name.setSelectAllOnFocus(true);
            androidx.appcompat.app.AlertDialog dialog=new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(tr("Bestandsnaam","File name","Dosya adı","Назва файлу")).setView(name).setPositiveButton(android.R.string.ok,null).setNegativeButton(android.R.string.cancel,null).create();
            dialog.setOnShowListener(ignored->dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{String value=name.getText().toString().trim();if(value.isEmpty()||value.contains("/")||value.contains("\\")){name.setError(tr("Kies een geldige bestandsnaam","Choose a valid file name","Geçerli dosya adı seçin","Виберіть коректну назву"));return;}csvFilename=value.toLowerCase(Locale.ROOT).endsWith(".csv")?value:value+".csv";getPreferences(MODE_PRIVATE).edit().putString("csvFilename",csvFilename).apply();dialog.dismiss();showSettings();}));dialog.show();
        });
        setting(content,"settings",tr("Weergave","Appearance","Görünüm","Вигляд"),"Blue Medical · BMD Scanner 1.2.0",0xff4b1679,null);
        gap(content,18);content.addView(new UiArt.Brand(this),new LinearLayout.LayoutParams(-1,dp(104)));scrollPage(content);
    }
    private View action(String icon,String title,int top,int bottom,Runnable run){
        LinearLayout button=row();button.setGravity(Gravity.CENTER);button.setPadding(dp(16),dp(15),dp(16),dp(15));button.setBackground(card(top,bottom,BLUE));button.addView(new UiArt.Icon(this,icon),new LinearLayout.LayoutParams(dp(34),dp(34)));TextView label=text(title,22,true);label.setPadding(dp(14),0,0,0);button.addView(label);clickable(button,title,run);return button;
    }
    private void showResults(){
        stopCamera();screen="results";base(t("results"));TextView summary=text(results.size()+" QR-codes",19,false);summary.setGravity(Gravity.CENTER);root.addView(summary);gap(root,14);
        LinearLayout items=column();if(results.isEmpty()){TextView empty=text(t("empty"),17,false);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(8),dp(40),dp(8),dp(40));items.addView(empty);}
        for(int i=0;i<results.size();i++){
            LinearLayout result=row();result.setPadding(dp(14),dp(16),dp(14),dp(16));result.setBackground(card(0xff073e66,0xff00142b,0xff087fba));result.addView(new UiArt.Icon(this,"qr"),new LinearLayout.LayoutParams(dp(28),dp(28)));
            TextView value=text((i+1)+".  "+results.get(i),16,false);value.setTextIsSelectable(true);value.setPadding(dp(12),0,0,0);result.addView(value,new LinearLayout.LayoutParams(0,-2,1));items.addView(result);gap(items,10);
        }
        scrollPage(items);gap(root,12);View csv=action("csv",t("export"),0xff00ce59,0xff006327,this::exportCsv);csv.setTag("csv");root.addView(csv,new LinearLayout.LayoutParams(-1,-2));gap(root,10);
        TextView clear=text(t("clear"),16,true);clear.setGravity(Gravity.CENTER);clear.setPadding(dp(12),dp(16),dp(12),dp(16));clear.setBackground(card(0xff06416a,0xff00142b,0xff087fba));root.addView(clear);clickable(clear,t("clear"),()->new androidx.appcompat.app.AlertDialog.Builder(this).setMessage(t("confirm")).setPositiveButton(android.R.string.ok,(dialog,which)->{results.clear();lastSeen.clear();showResults();}).setNegativeButton(android.R.string.cancel,null).show());
    }
    private void showScanner(){
        stopCamera();screen="camera";base(t("scan")+" (Batch)");
        preview=new PreviewView(this);preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);preview.setContentDescription(t("scan"));
        FrameLayout frame=new FrameLayout(this);root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));frame.addView(preview,new FrameLayout.LayoutParams(-1,-1));frame.addView(new ScanOverlay(this),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout.LayoutParams flashParams=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP|Gravity.END);frame.addView(iconButton("flash",tr("Zaklamp aan/uit","Toggle flashlight","Feneri aç/kapat","Ліхтарик"),this::toggleTorch),flashParams);
        gap(root,10);LinearLayout status=row();status.setPadding(dp(14),dp(12),dp(12),dp(12));status.setBackground(card(0xff08548c,0xff00182f,BLUE));status.addView(new UiArt.Icon(this,"check"),new LinearLayout.LayoutParams(dp(32),dp(32)));countText=text(t("results")+": "+results.size(),22,true);countText.setPadding(dp(10),0,0,0);status.addView(countText);root.addView(status);
        resultText=text("",14,false);resultText.setMaxLines(2);resultText.setPadding(dp(6),dp(8),dp(6),dp(8));root.addView(resultText);
        LinearLayout controls=row();controls.setGravity(Gravity.CENTER);root.addView(controls);
        View photos=iconButton("photo",photoText("choose"),this::choosePhotos);photos.setBackground(card(0xff096eb0,0xff001b3b,BLUE));controls.addView(photos,new LinearLayout.LayoutParams(dp(60),dp(60)));
        LinearLayout stop=column();stop.setGravity(Gravity.CENTER);stop.setBackground(UiArt.card(0xffff4f73,0xffb70024,0xffffc1d0,dp(60)));stop.addView(new UiArt.Icon(this,"stop"),new LinearLayout.LayoutParams(dp(36),dp(36)));stop.addView(text("Stop",18,true));clickable(stop,t("stop"),this::showHome);LinearLayout.LayoutParams stopParams=new LinearLayout.LayoutParams(dp(100),dp(100));stopParams.setMargins(dp(24),0,dp(24),0);controls.addView(stop,stopParams);
        View list=iconButton("list",t("results"),this::showResults);list.setBackground(card(0xff096eb0,0xff001b3b,BLUE));controls.addView(list,new LinearLayout.LayoutParams(dp(60),dp(60)));startCamera();
    }
    private void toggleTorch(){
        if(activeCamera==null||!activeCamera.getCameraInfo().hasFlashUnit()){Toast.makeText(this,tr("Geen zaklamp beschikbaar","Flashlight unavailable","Fener kullanılamıyor","Ліхтарик недоступний"),Toast.LENGTH_SHORT).show();return;}
        torch=!torch;activeCamera.getCameraControl().enableTorch(torch);
    }
    private static class ScanOverlay extends View{
        private final android.graphics.Paint paint=new android.graphics.Paint(3);
        ScanOverlay(android.content.Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(android.graphics.Canvas canvas){
            float w=getWidth(),h=getHeight(),side=Math.min(w*.76f,h*.76f),left=(w-side)/2,top=(h-side)/2,right=left+side,bottom=top+side,len=side*.18f;
            paint.setStyle(android.graphics.Paint.Style.FILL);paint.setColor(0x55001024);canvas.drawRect(0,0,w,top,paint);canvas.drawRect(0,bottom,w,h,paint);canvas.drawRect(0,top,left,bottom,paint);canvas.drawRect(right,top,w,bottom,paint);
            paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);android.graphics.Path path=new android.graphics.Path();
            path.moveTo(left,top+len);path.lineTo(left,top);path.lineTo(left+len,top);path.moveTo(right-len,top);path.lineTo(right,top);path.lineTo(right,top+len);path.moveTo(left,bottom-len);path.lineTo(left,bottom);path.lineTo(left+len,bottom);path.moveTo(right-len,bottom);path.lineTo(right,bottom);path.lineTo(right,bottom-len);
            float density=getResources().getDisplayMetrics().density;paint.setStrokeWidth(9*density);paint.setColor(0x7700baff);canvas.drawPath(path,paint);paint.setStrokeWidth(4*density);paint.setColor(0xff00eeff);canvas.drawPath(path,paint);paint.setStrokeWidth(density);paint.setColor(Color.WHITE);canvas.drawPath(path,paint);
            paint.setStrokeWidth(7*density);paint.setColor(0x5500ff64);canvas.drawLine(left,h/2,right,h/2,paint);paint.setStrokeWidth(2*density);paint.setColor(0xff24ff70);canvas.drawLine(left,h/2,right,h/2,paint);
        }
    }
    private String photoText(String key){
        String[] codes={"nl","en","tr","uk"};
        int locale=Arrays.asList(codes).indexOf(lang);if(locale<0)locale=0;
        switch(key){
            case "choose":return new String[]{"Foto’s uploaden","Upload photos","Fotoğraf seç","Вибрати фото"}[locale];
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
        photoBusy=true;if(busyText!=null)busyText.setText(photoText("busy"));
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
                resultText.setText(tr("Richt op de volledige QR-code. Tik om scherp te stellen.","Point at the entire QR code. Tap to focus.","QR kodun tamamını gösterin. Odaklamak için dokunun.","Наведіть на повний QR-код. Торкніться для фокусування."));
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
                    Long previous=lastSeen.get(value);if(previous!=null&&now-previous<repeatDelay)continue;
                    lastSeen.put(value,now);results.add(value);
                    countText.setText(t("results")+": "+results.size());resultText.setText(value);
                }
            }).addOnFailureListener(error->{if(scanning&&session==cameraSession)resultText.setText(photoText("error")+": "+error.getMessage());})
              .addOnCompleteListener(task->{processing.set(false);proxy.close();});
        }catch(Exception error){
            processing.set(false);proxy.close();
            runOnUiThread(()->{if(scanning&&session==cameraSession)resultText.setText(photoText("error")+": "+error.getMessage());});
        }
    }
    private byte[] csvBytes(){return CsvData.encode(results);}
    private void writeCsv(Uri uri){
        final byte[] data=pendingCsv!=null?pendingCsv:csvBytes();pendingCsv=null;
        cameraExecutor.execute(()->{
            try{
                try(OutputStream stream=getContentResolver().openOutputStream(uri)){if(stream==null)throw new IOException("No output stream");stream.write(data);}
                runOnUiThread(()->{if(!isDestroyed()&&!isFinishing())new androidx.appcompat.app.AlertDialog.Builder(this).setMessage(t("saved")).setPositiveButton(t("share"),(dialog,which)->shareCsv(uri)).setNegativeButton(android.R.string.ok,null).show();});
            }catch(Exception error){runOnUiThread(()->{if(!isDestroyed())Toast.makeText(this,t("error")+": "+error.getMessage(),Toast.LENGTH_LONG).show();});}
        });
    }
    private void exportCsv(){if(results.isEmpty()){Toast.makeText(this,t("empty"),Toast.LENGTH_SHORT).show();return;}pendingCsv=csvBytes();saveCsv.launch(csvFilename);}
    private void shareCsv(Uri uri){
        try{
            Intent intent=new Intent(Intent.ACTION_SEND);intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM,uri);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newUri(getContentResolver(),"CSV",uri));
            startActivity(Intent.createChooser(intent,t("share")));
        }catch(Exception error){Toast.makeText(this,t("error")+": "+error.getMessage(),Toast.LENGTH_LONG).show();}
    }
    @Override protected void onDestroy(){stopCamera();scanner.close();cameraExecutor.shutdown();super.onDestroy();}
}
