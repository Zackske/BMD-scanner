package com.bmd.scanner;

import android.Manifest;
import android.app.Dialog;
import android.content.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.core.resolutionselector.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.view.*;
import androidx.exifinterface.media.ExifInterface;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Full-screen QR/barcode reader. Raw content is shown; external actions require a tap. */
public class GenericScanActivity extends AppCompatActivity {
    private Context words;private String language="nl";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final LinkedHashMap<String,GenericCode> history=new LinkedHashMap<>();
    private final Set<String> visibleResults=new HashSet<>();private long lastSeen,lastFrame;
    private volatile boolean paused,destroyed,zooming;private volatile int epoch;
    private FrameLayout host;private PreviewView preview;private TextView hint,count;
    private View settingsAnchor;private ScannerControls.Torch torch;private PinchCameraZoom pinch;private ScannerSettingsPopup settings;
    private long lastFocus;private float lastFocusX=-1,lastFocusY=-1,touchX,touchY;private int zoomRequest;
    private ProcessCameraProvider provider;private androidx.camera.core.Camera camera;private Preview live;private ImageAnalysis analysis;
    private Dialog resultDialog;private boolean torchPending,importing;private float zoomRatio=1;private Toast toast;
    private final ActivityResultLauncher<String> permission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),ok->{if(ok)bind();else hint.setText(s("permission"));});
    private final ActivityResultLauncher<String[]> gallery=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{if(uri==null){paused=false;return;}readPhoto(uri);});

    @Override public void onCreate(Bundle state){ThemeSkin.prepare(this);super.onCreate(state);language=getIntent().getStringExtra("language");if(language==null)language="nl";Configuration cfg=new Configuration(getResources().getConfiguration());cfg.setLocale(Locale.forLanguageTag(language));words=createConfigurationContext(cfg);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);ThemeSkin.window(this);if(state!=null){ArrayList<String> values=state.getStringArrayList("values"),formats=state.getStringArrayList("formats");if(values!=null&&formats!=null&&values.size()==formats.size())for(int i=0;i<values.size();i++){GenericCode c=new GenericCode(values.get(i),formats.get(i));history.put(c.key(),c);}}
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){public void handleOnBackPressed(){if(settings!=null){settings.close();return;}if(resultDialog!=null){resultDialog.dismiss();return;}finish();}});layout();
    }
    @android.annotation.SuppressLint("DiscouragedApi") private String s(String key,Object...args){int id=getResources().getIdentifier("ui_"+key,"string",getPackageName());return id==0?key:args.length==0?words.getString(id):words.getString(id,args);}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setTypeface(ReferenceArt.face(this,false,650));t.setIncludeFontPadding(false);return t;}
    private void layout(){host=new FrameLayout(this);host.setTag("generic-camera");host.setBackgroundColor(ThemeSkin.active.background);ViewCompat.setOnApplyWindowInsetsListener(host,(view,insets)->{InsetsPadding.apply(view,insets);return insets;});setContentView(host);
        preview=new PreviewView(this);preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);preview.setContentDescription(s("generic_scan"));host.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(5),dp(2),dp(5),dp(2));bar.setBackground(ScannerControls.toolbar(this));bar.setTag("generic-toolbar");View back=new BackButton(this,s("back"),true);back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(dp(44),dp(44)));
        torch=new ScannerControls.Torch(this,s("on"),s("off"),s("flash"));torch.available(false);torch.setOnClickListener(v->toggleTorch());bar.addView(torch,new LinearLayout.LayoutParams(0,dp(44),1));settingsAnchor=new FrameLayout(this);ScannerControls.Gear gear=new ScannerControls.Gear(this);((FrameLayout)settingsAnchor).addView(gear,new FrameLayout.LayoutParams(dp(30),dp(30),Gravity.CENTER));settingsAnchor.setTag("generic-settings");settingsAnchor.setContentDescription(s("settings"));settingsAnchor.setClickable(true);settingsAnchor.setFocusable(true);settingsAnchor.setOnClickListener(v->{if(settings!=null||paused)return;paused=true;gear.animate().rotationBy(90).setDuration(200).start();settings=new ScannerSettingsPopup(host,settingsAnchor,this::s,()->{settings=null;paused=false;bind();});});bar.addView(settingsAnchor,new LinearLayout.LayoutParams(dp(44),dp(44)));FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(50),Gravity.TOP|Gravity.CENTER_HORIZONTAL);bp.setMargins(dp(6),dp(6),dp(6),0);host.addView(bar,bp);
        LinearLayout dock=new LinearLayout(this);dock.setGravity(Gravity.CENTER_VERTICAL);dock.setPadding(dp(10),0,dp(10),0);dock.setBackground(ScannerControls.dock(this));count=text(Integer.toString(history.size()),19);count.setTag("generic-history");count.setGravity(Gravity.CENTER);count.setBackground(ScannerControls.glass(this));count.setContentDescription(s("generic_results"));count.setOnClickListener(v->{if(!paused)showResults(new ArrayList<>(history.values()));});dock.addView(count,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView name=text(s("generic_scan"),19);name.setGravity(Gravity.CENTER);name.setMaxLines(1);name.setAutoSizeTextTypeUniformWithConfiguration(11,19,1,android.util.TypedValue.COMPLEX_UNIT_SP);dock.addView(name,new LinearLayout.LayoutParams(0,dp(50),1));FrameLayout photo=new FrameLayout(this);photo.setBackground(ScannerControls.glass(this));photo.setContentDescription(s("photos"));photo.setTag("generic-gallery");photo.setClickable(true);photo.setFocusable(true);UiArt.Icon icon=new UiArt.Icon(this,"photo",ScannerControls.accent(),true);photo.addView(icon,new FrameLayout.LayoutParams(dp(28),dp(28),Gravity.CENTER));photo.setOnClickListener(v->{if(!paused){paused=true;gallery.launch(new String[]{"image/*"});}});dock.addView(photo,new LinearLayout.LayoutParams(dp(48),dp(48)));FrameLayout.LayoutParams dp=new FrameLayout.LayoutParams(-1,dp(60),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);dp.setMargins(dp(6),0,dp(6),dp(12));host.addView(dock,dp);
        hint=text(s("generic_hint"),14);hint.setGravity(Gravity.CENTER);hint.setPadding(dp(12),dp(8),dp(12),dp(8));hint.setBackground(ScannerControls.toolbar(this));FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);hp.setMargins(dp(18),0,dp(18),dp(86));host.addView(hint,hp);
        TextView hud=text("",20);hud.setTag("pinch-zoom-ratio");hud.setGravity(Gravity.CENTER);hud.setBackground(ScannerControls.toolbar(this));FrameLayout.LayoutParams zp=new FrameLayout.LayoutParams(dp(104),dp(44),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);zp.bottomMargin=dp(132);host.addView(hud,zp);pinch=new PinchCameraZoom(this,preview,hud,new PinchCameraZoom.Listener(){public boolean ready(){return camera!=null&&!paused;}public void begin(){zooming=true;}public void change(float ratio){setZoom(ratio);}public void end(){zooming=false;}});
        preview.setTag("generic-preview");preview.setOnTouchListener((v,event)->{if(event.getPointerCount()==1){touchX=event.getX();touchY=event.getY();}return pinch.touch(event);});preview.setOnClickListener(v->focusPreview(touchX,touchY,true));
        host.post(()->{int shortSide=Math.min(getResources().getDisplayMetrics().widthPixels,getResources().getDisplayMetrics().heightPixels);int width=Math.min(host.getWidth()-dp(12),shortSide-dp(12));for(View v:new View[]{bar,dock}){FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)v.getLayoutParams();lp.width=width;lp.leftMargin=lp.rightMargin=0;v.setLayoutParams(lp);}if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)bind();else permission.launch(Manifest.permission.CAMERA);});
    }
    private ResolutionSelector resolution(){int choice=Math.max(0,Math.min(CameraResolutionOptions.TARGETS.length-1,getSharedPreferences("MainActivity",0).getInt("cameraResolution",1)));ResolutionSelector.Builder b=new ResolutionSelector.Builder();if(choice==CameraResolutionOptions.HIGHEST)b.setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY);else{CameraResolutionOptions.Target t=CameraResolutionOptions.TARGETS[choice];b.setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY);b.setResolutionStrategy(new ResolutionStrategy(new android.util.Size(t.longEdge(),t.shortEdge()),ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER));}return b.build();}
    private void bind(){
        if(destroyed||ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
        final int token=++epoch;final boolean restoreTorch=torch!=null&&torch.lit();ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);
        f.addListener(()->{if(destroyed||token!=epoch)return;try{
            provider=f.get();stopUseCases();int rotation=preview.getDisplay()==null?Surface.ROTATION_0:preview.getDisplay().getRotation();
            live=new Preview.Builder().setResolutionSelector(resolution()).setTargetRotation(rotation).build();
            analysis=new ImageAnalysis.Builder().setResolutionSelector(resolution()).setTargetRotation(rotation).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
            live.setSurfaceProvider(preview.getSurfaceProvider());analysis.setAnalyzer(worker,img->analyze(img,token));
            UseCaseGroup.Builder group=new UseCaseGroup.Builder().addUseCase(live).addUseCase(analysis);if(preview.getViewPort()!=null)group.setViewPort(preview.getViewPort());
            camera=provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,group.build());lastFocus=0;lastFocusX=lastFocusY=-1;
            torch.available(camera.getCameraInfo().hasFlashUnit());
            camera.getCameraInfo().getTorchState().removeObservers(this);camera.getCameraInfo().getTorchState().observe(this,value->{if(token==epoch&&!destroyed)torch.state(value==TorchState.ON,false);});
            if(restoreTorch&&camera.getCameraInfo().hasFlashUnit())camera.getCameraControl().enableTorch(true);
            ZoomState zoom=camera.getCameraInfo().getZoomState().getValue();if(zoom!=null){zoomRatio=Math.max(zoom.getMinZoomRatio(),Math.min(zoom.getMaxZoomRatio(),zoomRatio));pinch.configure(zoom.getMinZoomRatio(),zoom.getMaxZoomRatio(),zoomRatio,Locale.forLanguageTag(language));camera.getCameraControl().setZoomRatio(zoomRatio);}
            preview.getPreviewStreamState().removeObservers(this);preview.getPreviewStreamState().observe(this,state->{if(token==epoch&&!destroyed&&state==PreviewView.StreamState.STREAMING){touchX=preview.getWidth()/2f;touchY=preview.getHeight()/2f;focusPreview(touchX,touchY,false);}});
        }catch(Exception e){hint.setText(s("generic_error"));}},ContextCompat.getMainExecutor(this));
    }

    private void analyze(ImageProxy image,int token){
        long now=SystemClock.elapsedRealtime();if(destroyed||paused||zooming||token!=epoch||now-lastFrame<250){image.close();return;}lastFrame=now;
        try{GenericCodeReader.Frame frame=GenericCodeReader.readFrame(image,getSharedPreferences("MainActivity",0).getInt("sensitivity",1));
            runOnUiThread(()->{if(destroyed||paused||zooming||token!=epoch)return;
                if(frame.focus!=null&&analysis!=null){try{MeteringPoint point=new SurfaceOrientedMeteringPointFactory(1,1,analysis).createPoint(frame.focus[0],frame.focus[1],.18f);focus(point,frame.focus[0],frame.focus[1],false);}catch(IllegalStateException ignored){}}
                List<GenericCode> decoded=frame.codes;if(decoded.isEmpty()){if(now-lastSeen>1000)visibleResults.clear();return;}lastSeen=now;
                List<GenericCode> fresh=new ArrayList<>();for(GenericCode c:decoded)if(!visibleResults.contains(c.key()))fresh.add(c);if(fresh.isEmpty())return;
                for(GenericCode c:decoded)visibleResults.add(c.key());showResults(fresh);
            });
        }catch(Exception e){runOnUiThread(()->{if(!destroyed&&token==epoch)hint.setText(s("generic_error"));});}finally{image.close();}
    }
    private void focusPreview(float x,float y,boolean userTap){
        if(preview==null||preview.getWidth()<1||preview.getHeight()<1||x<0||y<0||x>preview.getWidth()||y>preview.getHeight())return;
        focus(preview.getMeteringPointFactory().createPoint(x,y,.18f),x/preview.getWidth(),y/preview.getHeight(),userTap);
    }
    private void focus(MeteringPoint point,float x,float y,boolean force){
        if(destroyed||paused||zooming||camera==null)return;long now=SystemClock.elapsedRealtime();
        if(!force&&(now-lastFocus<1200||(Math.abs(x-lastFocusX)<.1f&&Math.abs(y-lastFocusY)<.1f&&now-lastFocus<3000)))return;
        FocusMeteringAction action=new FocusMeteringAction.Builder(point,FocusMeteringAction.FLAG_AF|FocusMeteringAction.FLAG_AE).setAutoCancelDuration(2,TimeUnit.SECONDS).build();
        if(!camera.getCameraInfo().isFocusMeteringSupported(action))return;lastFocus=now;lastFocusX=x;lastFocusY=y;final int token=epoch;
        ListenableFuture<FocusMeteringResult> request=camera.getCameraControl().startFocusAndMetering(action);
        request.addListener(()->{if(destroyed||token!=epoch)return;try{request.get();}catch(Exception ignored){/* Fixed-focus cameras and interrupted focus must keep scanning. */}},ContextCompat.getMainExecutor(this));
    }

    private void toggleTorch(){if(camera==null||torchPending||!camera.getCameraInfo().hasFlashUnit())return;boolean next=!torch.lit();torchPending=true;torch.pending(true);int token=epoch;ListenableFuture<Void> f=camera.getCameraControl().enableTorch(next);f.addListener(()->{if(destroyed||token!=epoch)return;torchPending=false;torch.pending(false);try{f.get();torch.state(next,true);}catch(Exception e){notifyUser(s("generic_error"));}},ContextCompat.getMainExecutor(this));}
    private void setZoom(float ratio){if(camera==null)return;ZoomState state=camera.getCameraInfo().getZoomState().getValue();if(state==null)return;
        float target=Math.max(state.getMinZoomRatio(),Math.min(state.getMaxZoomRatio(),ratio));zoomRatio=target;int token=epoch,request=++zoomRequest;
        ListenableFuture<Void> f=camera.getCameraControl().setZoomRatio(target);f.addListener(()->{if(destroyed||token!=epoch||request!=zoomRequest)return;try{f.get();}catch(Exception e){if(camera!=null){ZoomState actual=camera.getCameraInfo().getZoomState().getValue();if(actual!=null){zoomRatio=actual.getZoomRatio();pinch.restore(zoomRatio);}}}},ContextCompat.getMainExecutor(this));
    }

    void showResults(List<GenericCode> results){if(results.isEmpty()){notifyUser(s("generic_empty"));return;}for(GenericCode c:results)history.put(c.key(),c);while(history.size()>250)history.remove(history.keySet().iterator().next());count.setText(Integer.toString(history.size()));paused=true;vibrate();if(results.size()==1){showCode(results.get(0));return;}LinearLayout rows=column();for(GenericCode c:results)rows.addView(action(c.format+"\n"+c.value,()->showCode(c)));showPanel(s("generic_results"),rows);}
    void showCode(GenericCode code){LinearLayout panel=column();String url=code.webLink();if(!url.isEmpty()){
        LinkPreviewView link=new LinkPreviewView(this,url,key->s(key));((TextView)link.findViewWithTag("generic-result-value")).setText(code.value);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(20);panel.addView(link,lp);
        View open=GenericResultSheet.action(this,s("generic_open_link"),"external",true,()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE));}catch(ActivityNotFoundException e){notifyUser(s("generic_error"));}});open.setTag("generic-open-link");LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-1,-2);op.bottomMargin=dp(10);panel.addView(open,op);
    }else{TextView content=text(code.value,18);content.setTag("generic-result-value");content.setTextIsSelectable(true);content.setPadding(0,0,0,dp(20));panel.addView(content);}
        LinearLayout actions=new LinearLayout(this);View copy=GenericResultSheet.action(this,s("generic_copy"),"copy",false,()->{ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText(s("generic_scan"),code.value));notifyUser(s("generic_copied"));});copy.setTag("generic-copy");LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.rightMargin=dp(5);actions.addView(copy,cp);View share=GenericResultSheet.action(this,s("share"),"share",false,()->startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,code.value),s("share"))));share.setTag("generic-share");LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,-2,1);sp.leftMargin=dp(5);actions.addView(share,sp);panel.addView(actions);showPanel(s("generic_result"),panel,code.format);
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private View action(String label,Runnable run){TextView t=text(label,16);t.setPadding(dp(12),dp(12),dp(12),dp(12));t.setMinHeight(dp(48));t.setBackground(ScannerControls.toolbar(this));t.setClickable(true);t.setFocusable(true);t.setOnClickListener(v->run.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);t.setLayoutParams(p);return t;}
    private void showPanel(String title,LinearLayout contents){showPanel(title,contents,"");}
    private void showPanel(String title,LinearLayout contents,String format){if(resultDialog!=null){resultDialog.setOnDismissListener(null);resultDialog.dismiss();}paused=true;GenericResultSheet d=new GenericResultSheet(this,title,format,s("close"),contents);resultDialog=d;d.setOnDismissListener(v->{if(resultDialog==d)resultDialog=null;paused=false;hint.setText(s("generic_hint"));});d.show();}
    private void readPhoto(Uri uri){paused=true;importing=true;hint.setText(s("busy"));final int token=epoch;worker.execute(()->{Bitmap bitmap=null;try{BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,bounds);}if(bounds.outWidth<=0||bounds.outHeight<=0)throw new IOException("Image");BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>3072)options.inSampleSize*=2;try(InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,options);}if(bitmap==null)throw new IOException("Image");try(InputStream in=getContentResolver().openInputStream(uri)){ExifInterface exif=new ExifInterface(in);Matrix transform=new Matrix();if(exif.isFlipped())transform.postScale(-1,1);transform.postRotate(exif.getRotationDegrees());if(!transform.isIdentity()){Bitmap corrected=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),transform,true);if(corrected!=bitmap){bitmap.recycle();bitmap=corrected;}}}List<GenericCode> found=GenericCodeReader.read(bitmap);runOnUiThread(()->{if(destroyed||token!=epoch)return;importing=false;paused=false;hint.setText(s("generic_hint"));showResults(found);});}catch(Exception e){runOnUiThread(()->{if(destroyed||token!=epoch)return;importing=false;paused=false;hint.setText(s("generic_error"));});}finally{if(bitmap!=null)bitmap.recycle();}});}
    private void vibrate(){if(getSharedPreferences("MainActivity",0).getBoolean("vibrateAfterScan",true))host.performHapticFeedback(HapticFeedbackConstants.CONFIRM);}
    private void notifyUser(String value){if(toast!=null)toast.cancel();toast=Toast.makeText(this,value,Toast.LENGTH_SHORT);toast.show();}
    private void stopUseCases(){if(provider!=null){if(live!=null)provider.unbind(live);if(analysis!=null)provider.unbind(analysis);}camera=null;torchPending=false;if(torch!=null)torch.pending(false);}
    @Override protected void onSaveInstanceState(Bundle out){ArrayList<String> values=new ArrayList<>(),formats=new ArrayList<>();for(GenericCode c:history.values()){values.add(c.value);formats.add(c.format);}out.putStringArrayList("values",values);out.putStringArrayList("formats",formats);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){destroyed=true;epoch++;if(resultDialog!=null)resultDialog.dismiss();if(settings!=null)settings.dispose();if(pinch!=null)pinch.dispose();stopUseCases();worker.shutdown();if(toast!=null)toast.cancel();super.onDestroy();}
    /** Keep camera overlays inside system bars in either orientation. */
    private static final class InsetsPadding {static void apply(View v,WindowInsetsCompat insets){androidx.core.graphics.Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());v.setPadding(safe.left,safe.top,safe.right,safe.bottom);}}
}
