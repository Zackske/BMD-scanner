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
    private final ScanConfirmation confirmation=new ScanConfirmation();
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
    private String scanMode="video";
    private int resolutionChoice=1,sensitivity=1;
    private boolean vibrateAfterScan=true,cameraReady=false,captureBusy=false;
    private float zoomRatio=1f;
    private ImageCapture imageCapture;
    private View startButton,stopButton,shutterButton,flashButton;
    private SeekBar zoomSlider;
    private TextView zoomLabel;
    private ScanFeedback scanFeedback;
    private android.app.Dialog cameraSettingsDialog;
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
        scanMode=getPreferences(MODE_PRIVATE).getString("scanMode","video");
        resolutionChoice=getPreferences(MODE_PRIVATE).getInt("cameraResolution",1);
        sensitivity=getPreferences(MODE_PRIVATE).getInt("sensitivity",1);
        vibrateAfterScan=getPreferences(MODE_PRIVATE).getBoolean("vibrateAfterScan",true);
        scanFeedback=new ScanFeedback(new android.os.Handler(android.os.Looper.getMainLooper()),()->{
            if(root!=null&&getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))root.performHapticFeedback(android.os.Build.VERSION.SDK_INT>=30?HapticFeedbackConstants.CONFIRM:HapticFeedbackConstants.CLOCK_TICK);
        });scanFeedback.setEnabled(vibrateAfterScan);
        csvFilename=getPreferences(MODE_PRIVATE).getString("csvFilename","BMD-Scanner-results.csv");
        if(state!=null){ArrayList<String> saved=state.getStringArrayList("results");if(saved!=null)results.addAll(saved);pendingCsv=state.getByteArray("pendingCsv");}
        scanner=BarcodeScanning.getClient(new com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build());
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){@Override public void handleOnBackPressed(){if(screen.equals("home"))finish();else showHome();}});
        showHome();
        if(state!=null&&"results".equals(state.getString("screen")))showResults();
        if(state!=null&&"camera".equals(state.getString("screen")))showScanner();
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
    private void stopCamera(){
        scanning=false;cameraReady=false;cameraSession++;preview=null;imageCapture=null;captureBusy=false;
        if(activeCamera!=null)activeCamera.getCameraControl().enableTorch(false);
        activeCamera=null;torch=false;confirmation.reset();
        if(cameraSettingsDialog!=null){cameraSettingsDialog.dismiss();cameraSettingsDialog=null;}
        if(cameraProvider!=null)cameraProvider.unbindAll();
    }
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
        setting(content,"share",tr("E-mail (Outlook)","Email (Outlook)","E-posta (Outlook)","Е-пошта (Outlook)"),tr("CSV delen via Outlook / Hotmail","Share CSV via Outlook / Hotmail","CSV dosyasını Outlook ile paylaş","Поділитися CSV через Outlook"),0xffa04b04,()->{if(results.isEmpty())Toast.makeText(this,t("empty"),Toast.LENGTH_SHORT).show();else exportCsv();});
        setting(content,"settings",tr("Weergave","Appearance","Görünüm","Вигляд"),"Blue Medical · BMD Scanner 1.3.0",0xff4b1679,null);
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
        TextView clear=text(t("clear"),16,true);clear.setGravity(Gravity.CENTER);clear.setPadding(dp(12),dp(16),dp(12),dp(16));clear.setBackground(card(0xff06416a,0xff00142b,0xff087fba));root.addView(clear);clickable(clear,t("clear"),()->new androidx.appcompat.app.AlertDialog.Builder(this).setMessage(t("confirm")).setPositiveButton(android.R.string.ok,(dialog,which)->{results.clear();confirmation.reset();showResults();}).setNegativeButton(android.R.string.cancel,null).show());
    }
    private void showScanner(){
        stopCamera();screen="camera";base("");
        LinearLayout header=row();header.addView(iconButton("back",t("back"),this::showHome),new LinearLayout.LayoutParams(dp(48),dp(54)));
        TextView title=text(t("scan"),25,true);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        View gear=iconButton("settings",cameraText("settings"),this::showCameraSettings);gear.setTag("camera-settings");header.addView(gear,new LinearLayout.LayoutParams(dp(48),dp(54)));root.addView(header);gap(root,8);
        preview=new PreviewView(this);preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);preview.setContentDescription(t("scan"));
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(0xff000a16);root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));frame.addView(preview,new FrameLayout.LayoutParams(-1,-1));frame.addView(new ScanOverlay(this,!isPhotoMode()),new FrameLayout.LayoutParams(-1,-1));
        TextView mode=text(isPhotoMode()?"Foto":"Video",16,true);mode.setPadding(dp(14),dp(8),dp(14),dp(8));mode.setBackground(card(0xff075b86,0xdd001329,BLUE));FrameLayout.LayoutParams modeParams=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);modeParams.setMargins(dp(10),dp(10),0,0);frame.addView(mode,modeParams);
        flashButton=iconButton("flash",cameraText("flash"),this::toggleTorch);flashButton.setTag("camera-flash");flashButton.setBackground(card(0xff075b86,0xdd001329,BLUE));FrameLayout.LayoutParams flashParams=new FrameLayout.LayoutParams(dp(52),dp(48),Gravity.TOP|Gravity.END);flashParams.setMargins(0,dp(8),dp(10),0);frame.addView(flashButton,flashParams);flashButton.setEnabled(false);
        LinearLayout zoom=row();zoom.setPadding(dp(8),dp(4),dp(8),dp(4));zoom.setBackground(card(0xcc06395b,0xdd001020,0xff3c8299));
        zoom.addView(iconButton("minus",cameraText("zoomout"),()->changeZoom(zoomRatio-.25f)),new LinearLayout.LayoutParams(dp(44),dp(44)));
        zoomSlider=new SeekBar(this);zoomSlider.setMax(100);zoomSlider.setContentDescription(cameraText("zoom"));zoomSlider.setTag("camera-zoom");zoomSlider.setEnabled(false);zoom.addView(zoomSlider,new LinearLayout.LayoutParams(0,dp(44),1));
        zoomLabel=text("1.0×",14,true);zoomLabel.setMinWidth(dp(42));zoomLabel.setGravity(Gravity.CENTER);zoom.addView(zoomLabel);
        zoom.addView(iconButton("plus",cameraText("zoomin"),()->changeZoom(zoomRatio+.25f)),new LinearLayout.LayoutParams(dp(44),dp(44)));
        FrameLayout.LayoutParams zoomParams=new FrameLayout.LayoutParams(-1,dp(52),Gravity.BOTTOM);zoomParams.setMargins(dp(8),0,dp(8),dp(8));frame.addView(zoom,zoomParams);
        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int value,boolean user){if(user&&activeCamera!=null){androidx.camera.core.ZoomState state=activeCamera.getCameraInfo().getZoomState().getValue();if(state!=null)changeZoom(state.getMinZoomRatio()+(state.getMaxZoomRatio()-state.getMinZoomRatio())*value/100f);}}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}});
        if(!isPhotoMode()){
            gap(root,10);LinearLayout status=row();status.setPadding(dp(12),dp(12),dp(12),dp(12));status.setBackground(card(0xff08548c,0xff00182f,BLUE));status.addView(new UiArt.Icon(this,"check"),new LinearLayout.LayoutParams(dp(30),dp(30)));countText=text(cameraText("scanned")+": "+results.size(),22,true);countText.setPadding(dp(10),0,0,0);status.addView(countText);root.addView(status);
        }else countText=null;
        resultText=text("",13,false);resultText.setMaxLines(2);resultText.setPadding(dp(6),dp(5),dp(6),dp(5));resultText.setVisibility(View.GONE);root.addView(resultText);
        gap(root,10);startButton=null;stopButton=null;shutterButton=null;
        if(isPhotoMode()){
            FrameLayout holder=new FrameLayout(this);root.addView(holder,new LinearLayout.LayoutParams(-1,dp(96)));
            FrameLayout shutter=new FrameLayout(this);android.graphics.drawable.GradientDrawable ring=new android.graphics.drawable.GradientDrawable();ring.setShape(android.graphics.drawable.GradientDrawable.OVAL);ring.setColor(0xff00142a);ring.setStroke(dp(3),BLUE);shutter.setBackground(ring);
            View center=new View(this);android.graphics.drawable.GradientDrawable fill=new android.graphics.drawable.GradientDrawable();fill.setShape(android.graphics.drawable.GradientDrawable.OVAL);fill.setColor(Color.WHITE);center.setBackground(fill);FrameLayout.LayoutParams centerParams=new FrameLayout.LayoutParams(dp(66),dp(66),Gravity.CENTER);shutter.addView(center,centerParams);
            shutter.addView(new UiArt.Icon(this,"camera",0xff00142a),new FrameLayout.LayoutParams(dp(36),dp(36),Gravity.CENTER));
            clickable(shutter,cameraText("capture"),this::takeScanPhoto);shutter.setTag("camera-capture");holder.addView(shutter,new FrameLayout.LayoutParams(dp(84),dp(84),Gravity.CENTER));shutterButton=shutter;
        }else{
            LinearLayout controls=row();root.addView(controls);
            startButton=action("play","Start",0xff00cb59,0xff006327,this::startVideoScan);startButton.setTag("camera-start");LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(64),1);left.setMargins(0,0,dp(6),0);controls.addView(startButton,left);
            stopButton=action("stop","Stop",0xfffb3b64,0xffa70024,this::pauseVideoScan);stopButton.setTag("camera-stop");LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,dp(64),1);right.setMargins(dp(6),0,0,0);controls.addView(stopButton,right);
        }
        updateScanButtons();startCamera();
    }
    private boolean isPhotoMode(){return "photo".equals(scanMode);}
    private String cameraText(String key){
        switch(key){
            case "settings":return tr("Camera-instellingen","Camera settings","Kamera ayarları","Налаштування камери");
            case "mode":return tr("Scanmodus","Scan mode","Tarama modu","Режим сканування");
            case "resolution":return tr("Resolutie","Resolution","Çözünürlük","Роздільна здатність");
            case "sensitivity":return tr("Scangevoeligheid","Scan sensitivity","Tarama hassasiyeti","Чутливість сканування");
            case "vibrate":return tr("Trillen na scan","Vibrate after scan","Taramadan sonra titreşim","Вібрація після сканування");
            case "capture":return tr("Foto maken","Take photo","Fotoğraf çek","Зробити фото");
            case "scanned":return tr("Gescand","Scanned","Tarandı","Відскановано");
            case "flash":return tr("Flitser aan/uit","Toggle flashlight","Feneri aç/kapat","Ліхтарик");
            case "zoom":return "Zoom";
            case "zoomin":return tr("Inzoomen","Zoom in","Yakınlaştır","Збільшити");
            case "zoomout":return tr("Uitzoomen","Zoom out","Uzaklaştır","Зменшити");
            case "apply":return tr("Toepassen","Apply","Uygula","Застосувати");
            default:return key;
        }
    }
    private String[] resolutionLabels(){return new String[]{"HD · 1280 × 720","Full HD · 1920 × 1080",tr("Hoogste ondersteunde resolutie","Highest supported resolution","En yüksek desteklenen çözünürlük","Найвища підтримувана роздільна здатність")};}
    private void showCameraSettings(){
        pauseVideoScan();
        android.app.Dialog dialog=new android.app.Dialog(this);cameraSettingsDialog=dialog;
        LinearLayout panel=column();panel.setPadding(dp(18),dp(16),dp(18),dp(16));panel.setBackground(card(0xff06375d,0xff001228,BLUE));
        TextView title=text(cameraText("settings"),23,true);panel.addView(title);gap(panel,14);panel.addView(text(cameraText("mode"),18,true));
        RadioGroup modes=new RadioGroup(this);modes.setOrientation(RadioGroup.HORIZONTAL);RadioButton video=new RadioButton(this),photo=new RadioButton(this);video.setId(View.generateViewId());photo.setId(View.generateViewId());video.setText("Video");photo.setText("Foto");video.setTextColor(WHITE);photo.setTextColor(WHITE);video.setContentDescription("Video");photo.setContentDescription("Foto");modes.addView(video,new RadioGroup.LayoutParams(0,dp(48),1));modes.addView(photo,new RadioGroup.LayoutParams(0,dp(48),1));modes.check(isPhotoMode()?photo.getId():video.getId());panel.addView(modes);
        TextView modeHint=text(tr("Video: continu · Foto: één opname","Video: continuous scan · Photo: one capture","Video: sürekli tara · Fotoğraf: tek çekim","Відео: безперервно · Фото: один знімок"),12,false);panel.addView(modeHint);gap(panel,12);
        panel.addView(text(cameraText("resolution"),18,true));Spinner resolution=new Spinner(this);resolution.setContentDescription(cameraText("resolution"));android.widget.ArrayAdapter<String> options=new android.widget.ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,resolutionLabels());resolution.setAdapter(options);resolution.setSelection(resolutionChoice);panel.addView(resolution,new LinearLayout.LayoutParams(-1,dp(48)));
        gap(panel,10);panel.addView(text(cameraText("sensitivity"),18,true));RadioGroup sensitivityGroup=new RadioGroup(this);sensitivityGroup.setOrientation(RadioGroup.HORIZONTAL);int[] sensitivityIds=new int[3];String[] labels={tr("Hoog","High","Yüksek","Висока"),tr("Normaal","Normal","Normal","Звичайна"),tr("Laag","Low","Düşük","Низька")};
        for(int i=0;i<3;i++){RadioButton item=new RadioButton(this);item.setId(View.generateViewId());sensitivityIds[i]=item.getId();item.setText(labels[i]);item.setTextSize(13);item.setTextColor(WHITE);sensitivityGroup.addView(item,new RadioGroup.LayoutParams(0,dp(48),1));}sensitivityGroup.check(sensitivityIds[sensitivity]);panel.addView(sensitivityGroup);
        TextView note=text(tr("Alleen voor Video · Laag = meer bevestiging","Video only · More confirmation = steadier scanning","Yalnızca Video · Daha fazla onay = daha sakin tarama","Лише відео · Більше підтверджень"),12,false);panel.addView(note);
        Runnable refreshSensitivity=()->{boolean enabled=modes.getCheckedRadioButtonId()==video.getId();sensitivityGroup.setAlpha(enabled?1f:.45f);for(int i=0;i<3;i++)sensitivityGroup.getChildAt(i).setEnabled(enabled);};refreshSensitivity.run();modes.setOnCheckedChangeListener((group,id)->refreshSensitivity.run());
        gap(panel,12);androidx.appcompat.widget.SwitchCompat vibration=new androidx.appcompat.widget.SwitchCompat(this);vibration.setText(cameraText("vibrate"));vibration.setTextColor(WHITE);vibration.setTextSize(18);vibration.setChecked(vibrateAfterScan);vibration.setTag("scan-vibration");panel.addView(vibration,new LinearLayout.LayoutParams(-1,dp(48)));
        panel.addView(text(tr("Korte trilling per toegevoegde QR-code","A short vibration for each added QR code","Eklenen her QR kod için kısa titreşim","Коротка вібрація для кожного доданого QR-коду"),12,false));gap(panel,12);
        TextView deviceNote=text(tr("Resolutie en camerafuncties verschillen per toestel.","Resolution and camera controls depend on the device.","Çözünürlük ve kamera özellikleri cihaza bağlıdır.","Можливості камери залежать від пристрою."),12,false);panel.addView(deviceNote);gap(panel,12);
        View apply=action("check",cameraText("apply"),0xff007de5,0xff003c87,()->{
            scanMode=modes.getCheckedRadioButtonId()==photo.getId()?"photo":"video";resolutionChoice=resolution.getSelectedItemPosition();for(int i=0;i<3;i++)if(sensitivityGroup.getCheckedRadioButtonId()==sensitivityIds[i])sensitivity=i;vibrateAfterScan=vibration.isChecked();
            getPreferences(MODE_PRIVATE).edit().putString("scanMode",scanMode).putInt("cameraResolution",resolutionChoice).putInt("sensitivity",sensitivity).putBoolean("vibrateAfterScan",vibrateAfterScan).apply();scanFeedback.setEnabled(vibrateAfterScan);dialog.dismiss();cameraSettingsDialog=null;showScanner();
        });apply.setContentDescription(cameraText("apply"));panel.addView(apply);
        panel.removeView(apply);ScrollView scroll=new ScrollView(this);scroll.setTag("camera-settings-scroll");scroll.addView(panel);
        LinearLayout outer=column();outer.setBackground(card(0xff06375d,0xff001228,BLUE));outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout.LayoutParams applyParams=new LinearLayout.LayoutParams(-1,dp(60));applyParams.setMargins(dp(18),dp(8),dp(18),dp(16));outer.addView(apply,applyParams);dialog.setContentView(outer);android.view.Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setDimAmount(.65f);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setGravity(Gravity.BOTTOM);window.setLayout(-1,-2);}dialog.show();if(window!=null){window.setLayout(-1,Math.min(dp(660),(int)(getResources().getDisplayMetrics().heightPixels*.86f)));window.setGravity(Gravity.BOTTOM);}
    }
    private void updateScanButtons(){
        if(startButton!=null){startButton.setEnabled(cameraReady&&!scanning);startButton.setAlpha(startButton.isEnabled()?1f:.45f);}
        if(stopButton!=null){stopButton.setEnabled(scanning);stopButton.setAlpha(stopButton.isEnabled()?1f:.45f);}
        if(shutterButton!=null){shutterButton.setEnabled(cameraReady&&!captureBusy);shutterButton.setAlpha(shutterButton.isEnabled()?1f:.45f);}
    }
    private void startVideoScan(){if(!cameraReady||isPhotoMode())return;confirmation.reset();scanning=true;resultText.setVisibility(View.GONE);updateScanButtons();}
    private void pauseVideoScan(){scanning=false;confirmation.reset();updateScanButtons();}
    private void toggleTorch(){
        if(activeCamera==null||!activeCamera.getCameraInfo().hasFlashUnit())return;
        final boolean next=!torch;final int session=cameraSession;ListenableFuture<Void> change=activeCamera.getCameraControl().enableTorch(next);
        change.addListener(()->{if(session!=cameraSession)return;try{change.get();torch=next;flashButton.setBackground(card(torch?0xff00a4c5:0xff075b86,0xdd001329,BLUE));}catch(Exception error){showScanError(error);}},ContextCompat.getMainExecutor(this));
    }
    private void changeZoom(float requested){
        if(activeCamera==null)return;androidx.camera.core.ZoomState state=activeCamera.getCameraInfo().getZoomState().getValue();if(state==null)return;
        final float value=Math.max(state.getMinZoomRatio(),Math.min(state.getMaxZoomRatio(),requested));final int session=cameraSession;ListenableFuture<Void> change=activeCamera.getCameraControl().setZoomRatio(value);
        change.addListener(()->{if(session!=cameraSession)return;try{change.get();zoomRatio=value;zoomLabel.setText(String.format(Locale.getDefault(),"%.1f×",value));float range=state.getMaxZoomRatio()-state.getMinZoomRatio();zoomSlider.setProgress(range>0?Math.round((value-state.getMinZoomRatio())*100/range):0);}catch(Exception error){showScanError(error);}},ContextCompat.getMainExecutor(this));
    }
    private void showScanError(Exception error){if(!"camera".equals(screen)||isDestroyed())return;resultText.setVisibility(View.VISIBLE);resultText.setText(photoText("error")+": "+error.getMessage());}
    private void appendScanResults(List<String> values){
        for(String value:values){if(value==null)continue;results.add(value);scanFeedback.success();}
        if("camera".equals(screen)&&!values.isEmpty()){
            if(countText!=null)countText.setText(cameraText("scanned")+": "+results.size());
            resultText.setVisibility(View.VISIBLE);resultText.setText(values.get(values.size()-1));
        }
    }
    private List<String> qrValues(List<Barcode> codes){List<String> values=new ArrayList<>();for(Barcode code:codes)if(code.getFormat()==Barcode.FORMAT_QR_CODE&&code.getRawValue()!=null)values.add(code.getRawValue());return values;}
    private void takeScanPhoto(){
        if(!cameraReady||imageCapture==null||captureBusy||!isPhotoMode())return;
        captureBusy=true;updateScanButtons();final int session=cameraSession;final File file;
        try{file=File.createTempFile("bmd-capture-",".jpg",getCacheDir());}catch(IOException error){captureBusy=false;updateScanButtons();showScanError(error);return;}
        imageCapture.takePicture(new ImageCapture.OutputFileOptions.Builder(file).build(),ContextCompat.getMainExecutor(this),new ImageCapture.OnImageSavedCallback(){
            @Override public void onError(@NonNull ImageCaptureException error){file.delete();if(session==cameraSession){captureBusy=false;updateScanButtons();showScanError(error);}}
            @Override public void onImageSaved(@NonNull ImageCapture.OutputFileResults output){
                if(session!=cameraSession||isDestroyed()){file.delete();return;}
                cameraExecutor.execute(()->{try{InputImage image=InputImage.fromFilePath(MainActivity.this,Uri.fromFile(file));
                    scanner.process(image).addOnSuccessListener(codes->{if(session!=cameraSession||isDestroyed())return;List<String> values=qrValues(codes);appendScanResults(values);if(values.isEmpty()){resultText.setVisibility(View.VISIBLE);resultText.setText(photoText("none"));}})
                        .addOnFailureListener(error->{if(session==cameraSession)showScanError(error);})
                        .addOnCompleteListener(task->{file.delete();if(session==cameraSession){captureBusy=false;updateScanButtons();}});
                }catch(Exception error){file.delete();runOnUiThread(()->{if(session==cameraSession){captureBusy=false;updateScanButtons();showScanError(error);}});}});
            }
        });
    }
    private static class ScanOverlay extends View{
        private final android.graphics.Paint paint=new android.graphics.Paint(3);
        private final boolean video;
        ScanOverlay(android.content.Context context,boolean video){super(context);this.video=video;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(android.graphics.Canvas canvas){
            float w=getWidth(),h=getHeight(),side=Math.min(w*.76f,h*.76f),left=(w-side)/2,top=(h-side)/2,right=left+side,bottom=top+side,len=side*.18f;
            paint.setStyle(android.graphics.Paint.Style.FILL);paint.setColor(0x55001024);canvas.drawRect(0,0,w,top,paint);canvas.drawRect(0,bottom,w,h,paint);canvas.drawRect(0,top,left,bottom,paint);canvas.drawRect(right,top,w,bottom,paint);
            paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);android.graphics.Path path=new android.graphics.Path();
            path.moveTo(left,top+len);path.lineTo(left,top);path.lineTo(left+len,top);path.moveTo(right-len,top);path.lineTo(right,top);path.lineTo(right,top+len);path.moveTo(left,bottom-len);path.lineTo(left,bottom);path.lineTo(left+len,bottom);path.moveTo(right-len,bottom);path.lineTo(right,bottom);path.lineTo(right,bottom-len);
            float density=getResources().getDisplayMetrics().density;paint.setStrokeWidth(9*density);paint.setColor(0x7700baff);canvas.drawPath(path,paint);paint.setStrokeWidth(4*density);paint.setColor(0xff00eeff);canvas.drawPath(path,paint);paint.setStrokeWidth(density);paint.setColor(Color.WHITE);canvas.drawPath(path,paint);
            if(video){paint.setStrokeWidth(7*density);paint.setColor(0x5500ff64);canvas.drawLine(left,h/2,right,h/2,paint);paint.setStrokeWidth(2*density);paint.setColor(0xff24ff70);canvas.drawLine(left,h/2,right,h/2,paint);}
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
                    List<String> values=qrValues(barcodes);appendScanResults(values);int added=values.size();
                    scanPhotos(queue,found+added,failed);
                }).addOnFailureListener(error->scanPhotos(queue,found,failed+1));
            }catch(Exception error){runOnUiThread(()->scanPhotos(queue,found,failed+1));}
        });
    }
    private androidx.camera.core.resolutionselector.ResolutionSelector cameraResolution(){
        androidx.camera.core.resolutionselector.ResolutionStrategy strategy=resolutionChoice==2?androidx.camera.core.resolutionselector.ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY:
            new androidx.camera.core.resolutionselector.ResolutionStrategy(resolutionChoice==0?new android.util.Size(1280,720):new android.util.Size(1920,1080),androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER);
        return new androidx.camera.core.resolutionselector.ResolutionSelector.Builder().setResolutionStrategy(strategy).build();
    }
    private void startCamera(){
        final int session=cameraSession;final PreviewView cameraPreview=preview;
        ListenableFuture<ProcessCameraProvider> future=ProcessCameraProvider.getInstance(this);
        future.addListener(()->{
            if(session!=cameraSession||cameraPreview==null||isDestroyed())return;
            try{
                cameraProvider=future.get();Preview previewUseCase=new Preview.Builder().build();previewUseCase.setSurfaceProvider(cameraPreview.getSurfaceProvider());cameraProvider.unbindAll();
                if(isPhotoMode()){
                    imageCapture=new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setResolutionSelector(cameraResolution()).build();
                    activeCamera=cameraProvider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,previewUseCase,imageCapture);
                }else{
                    ImageAnalysis analysis=new ImageAnalysis.Builder().setResolutionSelector(cameraResolution()).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();analysis.setAnalyzer(cameraExecutor,proxy->analyze(proxy,session));
                    activeCamera=cameraProvider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,previewUseCase,analysis);
                }
                cameraReady=true;flashButton.setEnabled(activeCamera.getCameraInfo().hasFlashUnit());flashButton.setAlpha(flashButton.isEnabled()?1f:.4f);
                androidx.camera.core.ZoomState state=activeCamera.getCameraInfo().getZoomState().getValue();zoomSlider.setEnabled(state!=null&&state.getMaxZoomRatio()>state.getMinZoomRatio());changeZoom(zoomRatio);updateScanButtons();
                cameraPreview.setOnTouchListener((view,event)->{if(event.getAction()==android.view.MotionEvent.ACTION_UP&&activeCamera!=null){FocusMeteringAction focus=new FocusMeteringAction.Builder(cameraPreview.getMeteringPointFactory().createPoint(event.getX(),event.getY())).build();activeCamera.getCameraControl().startFocusAndMetering(focus);view.performClick();}return true;});
            }catch(Exception error){cameraReady=false;updateScanButtons();showScanError(error);}
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
                appendScanResults(confirmation.acceptFrame(qrValues(barcodes),SystemClock.elapsedRealtime(),sensitivity+1,repeatDelay));
            }).addOnFailureListener(error->{if(scanning&&session==cameraSession)showScanError(error);})
              .addOnCompleteListener(task->{processing.set(false);proxy.close();});
        }catch(Exception error){
            processing.set(false);proxy.close();
            runOnUiThread(()->{if(scanning&&session==cameraSession)showScanError(error);});
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
    @Override protected void onPause(){pauseVideoScan();if(scanFeedback!=null)scanFeedback.cancel();super.onPause();}
    @Override protected void onDestroy(){stopCamera();if(scanFeedback!=null)scanFeedback.cancel();scanner.close();cameraExecutor.shutdown();super.onDestroy();}
}
