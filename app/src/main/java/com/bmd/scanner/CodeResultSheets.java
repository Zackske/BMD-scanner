package com.bmd.scanner;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import java.util.function.Consumer;
import java.util.function.Function;

/** One translated result/website sheet shared by the reader and product-control camera. */
final class CodeResultSheets {
    private CodeResultSheets(){}
    static GenericResultSheet create(Activity activity,GenericCode code,Function<String,String> words,Consumer<String> notice){
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);String url=code.webLink();
        if(!url.isEmpty()){
            LinkPreviewView preview=new LinkPreviewView(activity,url,words::apply);((TextView)preview.findViewWithTag("generic-result-value")).setText(code.value);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.bottomMargin=dp(activity,20);panel.addView(preview,pp);
            View open=GenericResultSheet.action(activity,words.apply("generic_open_link"),"external",true,()->{try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE));}catch(ActivityNotFoundException error){notice.accept(words.apply("generic_error"));}});open.setTag("generic-open-link");LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-1,-2);op.bottomMargin=dp(activity,10);panel.addView(open,op);
        }else{TextView value=GenericResultSheet.label(activity,code.value,18,Color.WHITE,650);value.setTag("generic-result-value");value.setTextIsSelectable(true);value.setPadding(0,0,0,dp(activity,20));panel.addView(value);}
        LinearLayout actions=new LinearLayout(activity);
        View copy=GenericResultSheet.action(activity,words.apply("generic_copy"),"copy",false,()->{ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Activity.CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText(words.apply("generic_result"),code.value));notice.accept(words.apply("generic_copied"));});copy.setTag("generic-copy");LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.rightMargin=dp(activity,5);actions.addView(copy,cp);
        View share=GenericResultSheet.action(activity,words.apply("share"),"share",false,()->{try{activity.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,code.value),words.apply("share")));}catch(ActivityNotFoundException error){notice.accept(words.apply("generic_error"));}});share.setTag("generic-share");LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,-2,1);sp.leftMargin=dp(activity,5);actions.addView(share,sp);panel.addView(actions);
        return new GenericResultSheet(activity,words.apply("generic_result"),code.format,words.apply("close"),panel);
    }
    private static int dp(Activity activity,float value){return GenericResultSheet.dp(activity,value);}
}
