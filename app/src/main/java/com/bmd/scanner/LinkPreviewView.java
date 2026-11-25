package com.bmd.scanner;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.http.SslError;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.ByteArrayInputStream;

/** A web preview is created only after a tap and released with its result popup. */
final class LinkPreviewView extends LinearLayout {
    interface Words {String get(String key);}
    interface Loader {void load(WebView view,String url);}
    private final GenericCode code;
    private final Words words;
    private final Loader loader;
    private final FrameLayout frame;
    private final TextView status,host;
    private final ProgressBar progress;
    private final Button toggle;
    private final LinearLayout loading;
    private WebView web;
    private boolean disposed,failed;
    private final Runnable timeout=()->fail();

    LinkPreviewView(Context context,String url,Words words){this(context,url,words,(view,address)->view.loadUrl(address));}
    LinkPreviewView(Context context,String url,Words words,Loader loader){
        super(context);this.code=new GenericCode(url,"QR");this.words=words;this.loader=loader;
        setTag("generic-link-preview");setContentDescription(words.get("generic_preview"));setOrientation(VERTICAL);setPadding(dp(15),dp(15),dp(15),dp(10));setBackground(GenericResultSheet.card(context));
        LinearLayout address=new LinearLayout(context);address.setGravity(Gravity.CENTER_VERTICAL);address.addView(new UiArt.Icon(context,"language",0xffd9e3ea,true),new LayoutParams(dp(26),dp(26)));LinearLayout captions=new LinearLayout(context);captions.setOrientation(VERTICAL);
        host=label(code.webHost(),18);host.setTag("generic-link-host");host.setTextColor(ScannerControls.accent());host.setTypeface(ReferenceArt.face(context,false,850));host.setTextIsSelectable(true);captions.addView(host);
        TextView value=label(url,12);value.setTag("generic-result-value");value.setTextColor(0xffbcd1dd);value.setTextIsSelectable(true);value.setPadding(0,dp(3),0,0);captions.addView(value);LayoutParams captionsLayout=new LayoutParams(0,-2,1);captionsLayout.leftMargin=dp(10);address.addView(captions,captionsLayout);addView(address);
        loading=new LinearLayout(context);loading.setGravity(Gravity.CENTER_VERTICAL);loading.setPadding(0,dp(8),0,0);loading.setVisibility(GONE);
        progress=new ProgressBar(context);progress.setIndeterminateTintList(ColorStateList.valueOf(ScannerControls.accent()));progress.setVisibility(GONE);loading.addView(progress,new LayoutParams(dp(20),dp(20)));
        status=label("",13);status.setTag("generic-preview-status");status.setMaxLines(3);status.setEllipsize(TextUtils.TruncateAt.END);status.setPadding(dp(4),0,0,0);status.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);loading.addView(status,new LayoutParams(0,-2,1));addView(loading);
        frame=new FrameLayout(context);frame.setVisibility(GONE);frame.setTag("generic-preview-frame");int height=Math.max(dp(90),Math.min(dp(200),Math.round(getResources().getDisplayMetrics().heightPixels*.24f)));LayoutParams fp=new LayoutParams(-1,height);fp.topMargin=dp(10);addView(frame,fp);
        toggle=new Button(context);toggle.setTag("generic-preview-load");toggle.setText(words.get("generic_preview_load"));toggle.setTextColor(0xffbcd1dd);toggle.setTypeface(ReferenceArt.face(context,false,650));toggle.setTextSize(14);toggle.setAllCaps(false);toggle.setMinHeight(dp(48));toggle.setMinimumHeight(dp(48));toggle.setPadding(dp(8),dp(6),dp(8),dp(6));toggle.setBackgroundColor(Color.TRANSPARENT);toggle.setMaxLines(2);toggle.setAutoSizeTextTypeUniformWithConfiguration(11,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);toggle.setOnClickListener(v->{if(web==null)start();else hidePreview();});LayoutParams bp=new LayoutParams(-1,-2);bp.topMargin=dp(8);addView(toggle,bp);
        if(!code.canPreview()){loading.setVisibility(VISIBLE);status.setText(words.get("generic_preview_error"));toggle.setVisibility(GONE);}
    }
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView label(String value,int size){TextView t=new TextView(getContext());t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setTypeface(ReferenceArt.face(getContext(),false,650));t.setIncludeFontPadding(false);return t;}
    private void start(){
        if(disposed||!code.canPreview())return;
        failed=false;loading.setVisibility(VISIBLE);status.setText(words.get("generic_preview_loading"));progress.setVisibility(VISIBLE);toggle.setText(words.get("generic_preview_hide"));frame.setVisibility(VISIBLE);
        if(android.animation.ValueAnimator.areAnimatorsEnabled()){frame.setAlpha(0);frame.setTranslationY(dp(8));frame.animate().alpha(1).translationY(0).setDuration(220).start();}
        try{
        WebView view=new WebView(getContext());web=view;view.setTag("generic-link-preview-web");view.setContentDescription(words.get("generic_preview"));view.setBackgroundColor(Color.WHITE);view.setSaveEnabled(false);view.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        WebSettings config=view.getSettings();config.setJavaScriptEnabled(false);config.setDomStorageEnabled(false);config.setAllowFileAccess(false);config.setAllowContentAccess(false);config.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);config.setSafeBrowsingEnabled(true);config.setGeolocationEnabled(false);config.setSupportMultipleWindows(false);config.setJavaScriptCanOpenWindowsAutomatically(false);config.setMediaPlaybackRequiresUserGesture(true);config.setCacheMode(WebSettings.LOAD_NO_CACHE);config.setBlockNetworkLoads(false);config.setLoadWithOverviewMode(true);config.setUseWideViewPort(true);CookieManager.getInstance().setAcceptThirdPartyCookies(view,false);
        view.setWebViewClient(new WebViewClient(){
            @Override public void onPageStarted(WebView current,String url,android.graphics.Bitmap icon){if(web==current&&permitted(url))host.setText(new GenericCode(url,"QR").webHost());}
            @Override public boolean shouldOverrideUrlLoading(WebView current,WebResourceRequest request){boolean blocked=!permitted(request.getUrl().toString());if(blocked&&request.isForMainFrame()&&web==current)fail();return blocked;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView current,WebResourceRequest request){return permitted(request.getUrl().toString())?null:new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
            @Override public void onPageFinished(WebView current,String url){if(disposed||failed||web!=current)return;removeCallbacks(timeout);progress.setVisibility(GONE);String title=current.getTitle();status.setText(title==null||title.trim().isEmpty()?code.webHost():title);}
            @Override public void onReceivedError(WebView current,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame()&&web==current)fail();}
            @Override public void onReceivedHttpError(WebView current,WebResourceRequest request,WebResourceResponse error){if(request.isForMainFrame()&&web==current)fail();}
            @Override public void onReceivedSslError(WebView current,SslErrorHandler handler,SslError error){handler.cancel();if(web==current)fail();}
            @Override public boolean onRenderProcessGone(WebView current,RenderProcessGoneDetail detail){if(web==current){web=null;frame.removeView(current);current.destroy();fail();}return true;}
        });
        frame.addView(view,new FrameLayout.LayoutParams(-1,-1));postDelayed(timeout,15000);
        loader.load(view,code.webLink());
        }catch(RuntimeException error){fail();}
    }
    private static boolean permitted(String url){try{return new GenericCode(url,"QR").canPreview();}catch(IllegalArgumentException error){return false;}}
    private void fail(){if(disposed)return;failed=true;releaseWeb();loading.setVisibility(VISIBLE);progress.setVisibility(GONE);status.setText(words.get("generic_preview_error"));toggle.setText(words.get("generic_preview_retry"));}
    private void hidePreview(){releaseWeb();failed=false;loading.setVisibility(GONE);progress.setVisibility(GONE);status.setText("");toggle.setText(words.get("generic_preview_load"));}
    private void releaseWeb(){removeCallbacks(timeout);WebView current=web;web=null;frame.setVisibility(GONE);if(current!=null){frame.removeView(current);current.stopLoading();current.setWebViewClient(new WebViewClient());current.destroy();}}
    @Override protected void onDetachedFromWindow(){disposed=true;releaseWeb();super.onDetachedFromWindow();}
}
