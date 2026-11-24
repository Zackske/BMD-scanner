package com.bmd.scanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The approved customer design, with offline artwork and a privately imported customer register. */
public class CustomerPortalActivity extends AppCompatActivity {
    WebView portal;
    private MenuHeader menuHeader;
    private volatile String rememberedState="{}";
    private String language;
    private String brand;
    private int loadGeneration;
    private CustomerRegister register;
    private final ExecutorService imports=Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String[]> picker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{if(uri!=null)importCustomers(uri);});

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle saved){
        ThemeSkin.prepare(this);super.onCreate(saved);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);ThemeSkin.window(this);
        language=getIntent().getStringExtra("language");
        if(!Arrays.asList(MainActivity.LANGS).contains(language))language="nl";
        if(saved!=null)rememberedState=saved.getString("customer-portal-state","{}");
        Configuration config=new Configuration(getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(language));Context words=createConfigurationContext(config);
        brand=words.getString(R.string.ui_short_name);
        try{register=new CustomerRegister(this,new JSONObject(readAsset("customer-portal/maps.json")).getJSONObject("countries"));}
        catch(Exception failure){throw new IllegalStateException("Missing offline country artwork",failure);}

        FrameLayout shell=new FrameLayout(this);shell.setBackgroundColor(ThemeSkin.active.background);
        portal=new WebView(this);portal.setTag("customer-portal");portal.setBackgroundColor(ThemeSkin.active.background);
        // Let WebView composite and scroll with the window's GPU acceleration.
        // A software layer rasterizes the whole portal on the UI thread.
        portal.setLayerType(View.LAYER_TYPE_NONE,null);
        WebSettings settings=portal.getSettings();settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);settings.setSupportMultipleWindows(false);
        settings.setBuiltInZoomControls(false);settings.setDisplayZoomControls(false);
        // The tile fitter adjusts the measured text without changing registered names.
        settings.setTextZoom(Math.round(getResources().getConfiguration().fontScale*100));
        portal.addJavascriptInterface(new SessionBridge(),"BmdPortalSession");
        portal.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return true;}
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return true;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){return bundledResource(request.getUrl());}
        });
        shell.addView(new UiArt.Backdrop(this),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout contents=new LinearLayout(this);contents.setOrientation(LinearLayout.VERTICAL);
        menuHeader=new MenuHeader(this,"portal",words.getString(R.string.ui_portal),words.getString(R.string.ui_back),()->getOnBackPressedDispatcher().onBackPressed());
        menuHeader.setPadding(dp(16),dp(8),dp(16),0);menuHeader.findViewWithTag("menu-back").setTag("customer-portal-back");
        contents.addView(menuHeader,new LinearLayout.LayoutParams(-1,-2));
        contents.addView(portal,new LinearLayout.LayoutParams(-1,0,1));
        shell.addView(contents,new FrameLayout.LayoutParams(-1,-1));
        ViewCompat.setOnApplyWindowInsetsListener(shell,(view,insets)->{
            androidx.core.graphics.Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left,safe.top,safe.right,safe.bottom);return WindowInsetsCompat.CONSUMED;
        });
        setContentView(shell);ViewCompat.requestApplyInsets(shell);
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){
                portal.evaluateJavascript("Boolean(window.BmdCustomerPortal&&window.BmdCustomerPortal.back())",handled->{if(!"true".equals(handled))finish();});
            }
        });
        loadPortal();
    }
    private void loadPortal(){
        final int generation=++loadGeneration;
        final String savedState=rememberedState,theme=ThemeSkin.active.id;
        imports.execute(()->{try{
            JSONObject state=new JSONObject(savedState);
            state.put("theme",theme);state.put("language",language);state.put("brand",brand);
            JSONObject art=new JSONObject(readAsset("customer-portal/art.json"));art.put("maps",new JSONObject(readAsset("customer-portal/maps.json")));
            JSONArray customers;
            try{customers=register.read();}catch(Exception invalidPrivateFile){customers=new JSONArray();}
            String html=readAsset("customer_portal.html")
                .replace("/*BMD_NATIVE_STATE*/null",state.toString().replace("<","\\u003c"))
                .replace("/*BMD_CUSTOMER_DATA*/[]",customers.toString().replace("<","\\u003c"))
                .replace("/*BMD_PORTAL_ASSETS*/{}",art.toString().replace("<","\\u003c"));
            runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&portal!=null&&generation==loadGeneration)
                portal.loadDataWithBaseURL("https://bmd.invalid/customer-portal/",html,"text/html","UTF-8",null);});
        }catch(Exception failure){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&generation==loadGeneration)
            throw new IllegalStateException("Offline portal artwork could not be read",failure);});}});
    }
    void importCustomers(Uri uri){
        imports.execute(()->{
            boolean success=false;
            try(InputStream input=getContentResolver().openInputStream(uri)){
                if(input==null)throw new IllegalArgumentException("No document");
                register.importText(CustomerRegister.readText(input));success=true;
            }catch(Exception invalidImport){/* Keep the previous register and show translated feedback. */}
            final boolean imported=success;
            runOnUiThread(()->{if(isFinishing()||isDestroyed()||portal==null)return;
                if(imported){rememberedState="{}";loadPortal();}
                else portal.evaluateJavascript("window.BmdCustomerPortal&&window.BmdCustomerPortal.importError()",null);
            });
        });
    }

    public final class SessionBridge {
        @JavascriptInterface public void headerCount(String text){
            if(text==null||text.length()>160)return;
            runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&menuHeader!=null)menuHeader.setSubtitle(text);});
        }
        @JavascriptInterface public void importCustomers(){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())picker.launch(new String[]{"application/json","text/plain","application/octet-stream"});});}
        @JavascriptInterface public void remember(String state){
            if(state==null||state.length()>4096)return;
            try{JSONObject parsed=new JSONObject(state);if(parsed.has("page"))rememberedState=parsed.toString();}catch(Exception ignored){/* Ignore malformed navigation state. */}
        }
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private WebResourceResponse bundledResource(Uri uri){
        String path=uri.getPath();
        if("https".equals(uri.getScheme())&&"bmd.invalid".equals(uri.getHost())&&path!=null&&!path.contains("..")&&!path.contains("\\")&&(path.startsWith("/assets/customer-portal/images/")||path.equals("/assets/fonts/nunito_regular.ttf"))){
            try{String mime=path.endsWith(".svg")?"image/svg+xml":path.endsWith(".webp")?"image/webp":path.endsWith(".png")?"image/png":"font/ttf";return new WebResourceResponse(mime,null,getAssets().open(path.substring("/assets/".length())));}catch(Exception ignored){/* A missing bundled image must not fall back to the network. */}
        }
        return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
    }
    private String readAsset(String name)throws Exception{
        try(InputStream in=getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    @Override protected void onSaveInstanceState(Bundle out){out.putString("customer-portal-state",rememberedState);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){
        imports.shutdown();
        if(portal!=null){portal.removeJavascriptInterface("BmdPortalSession");portal.stopLoading();if(portal.getParent() instanceof ViewGroup)((ViewGroup)portal.getParent()).removeView(portal);portal.destroy();portal=null;}
        super.onDestroy();
    }
}
