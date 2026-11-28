package com.bmd.scanner;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import android.net.Uri;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Runs the actual bundled page inside the installed Android APK, with networking blocked. */
@RunWith(AndroidJUnit4.class)
public class CustomerPortalTest {
    private CustomerRegister register()throws Exception{
        JSONObject maps;
        try(java.io.InputStream input=DeviceSupport.context().getAssets().open("customer-portal/maps.json")){maps=new JSONObject(CustomerRegister.readText(input));}
        return new CustomerRegister(DeviceSupport.context(),maps.getJSONObject("countries"));
    }
    private String fixture(int count)throws Exception{
        JSONArray rows=new JSONArray();
        for(int i=0;i<count;i++)rows.put(new JSONObject().put("id","fixture-"+i).put("name","Synthetic Customer "+i).put("displayName","Demo Client "+i)
            .put("customerId",i<2?"TEST-SHARED":"TEST-"+i).put("email","customer.register@example.invalid").put("country",i%2==0?"NL":"CH").put("shippingGroup",i%2==0?"eu":"non-eu"));
        return new JSONObject().put("schema",1).put("customers",rows).toString();
    }
    @Before public void seedPrivateSyntheticRegister()throws Exception{register().importText(fixture(84));}
    @After public void reset(){new File(DeviceSupport.context().getNoBackupFilesDir(),CustomerRegister.FILE_NAME).delete();DeviceSupport.clear();}
    @Test public void portalUsesGpuAndReusesCustomerTilesWhenOpeningDetails()throws Exception{
        try(ActivityScenario<CustomerPortalActivity> scenario=launch("bmd","nl")){
            ready(scenario);
            scenario.onActivity(a->{
                assertTrue("Portal window must support GPU rendering",a.portal.isHardwareAccelerated());
                assertEquals("Portal must not force CPU rasterization",View.LAYER_TYPE_NONE,a.portal.getLayerType());
            });
            assertEquals("true",js(scenario,"window.originalCustomerTile=document.querySelector('.cp-customer');window.originalCustomerTile.click();true"));
            assertEquals("true",js(scenario,"document.querySelector('.cp-customer')===window.originalCustomerTile"));
            assertEquals("true",js(scenario,"window.BmdCustomerPortal.back();document.querySelector('.cp-customer')===window.originalCustomerTile"));
            assertEquals("false",js(scenario,"document.querySelector('.cp-grid').hidden"));
            shot(scenario,"portal-gpu-customer-tiles");
        }
    }
    private ActivityScenario<CustomerPortalActivity> launch(String theme,String language){
        DeviceSupport.context().getSharedPreferences("MainActivity",0).edit().clear().putString("language",language).putString("colourTheme",theme).commit();
        return ActivityScenario.launch(new Intent(DeviceSupport.context(),CustomerPortalActivity.class).putExtra("language",language));
    }
    @Test public void menuTitlesFitTheirMeasuredBoundsInEveryLanguage()throws Exception{
        try(ActivityScenario<CustomerPortalActivity> scenario=launch("bmd","nl")){
            ready(scenario);
            scenario.onActivity(activity->{
                for(String language:MainActivity.LANGS){
                    Configuration config=new Configuration(activity.getResources().getConfiguration());config.setLocale(Locale.forLanguageTag(language));
                    android.content.Context words=activity.createConfigurationContext(config);
                    for(int resource:new int[]{R.string.ui_settings,R.string.ui_scan,R.string.ui_portal})for(int widthDp:new int[]{320,360,448}){
                        MenuHeader header=new MenuHeader(words,"portal",words.getString(resource),words.getString(R.string.ui_back),()->{});
                        int width=Math.round(widthDp*activity.getResources().getDisplayMetrics().density);
                        header.setPadding(32,16,32,0);header.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));header.layout(0,0,width,header.getMeasuredHeight());
                        View back=header.findViewWithTag("menu-back"),icon=header.findViewWithTag("menu-header-icon-portal");
                        int expectedGap=Math.round(24*activity.getResources().getDisplayMetrics().density);
                        assertEquals("Every submenu keeps the Settings gap",expectedGap,icon.getLeft()-back.getRight());
                        assertTrue("Title follows the icon",header.title.getLeft()>=icon.getRight());
                        assertTrue("Title stays inside header",header.title.getRight()<=header.getWidth()-header.getPaddingRight());
                        android.text.Layout layout=header.title.getLayout();assertNotNull(layout);
                        assertEquals("Every title character is laid out in "+language,header.title.length(),layout.getLineEnd(layout.getLineCount()-1));
                        assertTrue("Title height must include all text lines in "+language,layout.getHeight()<=header.title.getHeight());
                        for(int line=0;line<layout.getLineCount();line++)assertTrue("Title width must include all text in "+language+" caption="+header.title.getText()+" viewport="+widthDp+" line="+layout.getLineWidth(line)+" available="+(header.title.getWidth()-header.title.getPaddingLeft()-header.title.getPaddingRight()),layout.getLineWidth(line)<=header.title.getWidth()-header.title.getPaddingLeft()-header.title.getPaddingRight()+1);
                    }
                }
            });
        }
    }
    private String js(ActivityScenario<CustomerPortalActivity> scenario,String code)throws Exception{
        AtomicReference<String> result=new AtomicReference<>();CountDownLatch latch=new CountDownLatch(1);
        scenario.onActivity(a->a.portal.evaluateJavascript(code,v->{result.set(v);latch.countDown();}));
        assertTrue("Android WebView responds",latch.await(8,TimeUnit.SECONDS));return result.get();
    }
    private void ready(ActivityScenario<CustomerPortalActivity> scenario)throws Exception{
        long end=SystemClock.elapsedRealtime()+30000;
        while(SystemClock.elapsedRealtime()<end){if("true".equals(js(scenario,"Boolean(window.BmdCustomerPortal&&window.BmdCustomerPortal.ready)")))return;SystemClock.sleep(100);}
        fail("The offline customer portal did not finish loading");
    }
    private String brand(String language){Configuration c=new Configuration(DeviceSupport.context().getResources().getConfiguration());c.setLocale(Locale.forLanguageTag(language));return DeviceSupport.context().createConfigurationContext(c).getString(R.string.ui_short_name);}
    private void shot(ActivityScenario<CustomerPortalActivity> scenario,String name)throws Exception{
        CountDownLatch rendered=new CountDownLatch(1);
        scenario.onActivity(a->a.portal.postVisualStateCallback(SystemClock.uptimeMillis(),new android.webkit.WebView.VisualStateCallback(){@Override public void onComplete(long id){rendered.countDown();}}));
        assertTrue("Web content must be ready before taking a screenshot",rendered.await(15,TimeUnit.SECONDS));
        DeviceSupport.shot(scenario,name);
    }
    @Test public void all84OfflineCustomersRemainSeparateAndCorrect()throws Exception{
        try(ActivityScenario<CustomerPortalActivity> scenario=launch("bmd","nl")){
            ready(scenario);scenario.onActivity(a->{assertTrue(a.portal.getSettings().getBlockNetworkLoads());assertFalse(a.portal.getSettings().getAllowFileAccess());assertFalse(a.portal.getSettings().getAllowContentAccess());});
            Set<String> ids=new HashSet<>();int pages=0,shared=0;
            while(true){
                JSONArray rows=new JSONArray(js(scenario,"Array.from(document.querySelectorAll('.cp-customer'),b=>({id:b.dataset.customer,full:b.getAttribute('aria-label')}))"));
                for(int i=0;i<rows.length();i++){
                    JSONObject row=rows.getJSONObject(i);assertTrue("Stable separate customer identity",ids.add(row.getString("id")));
                    assertTrue(row.getString("full").startsWith("Synthetic Customer "));
                    if(row.getString("id").equals("fixture-0")||row.getString("id").equals("fixture-1")){
                        js(scenario,"document.querySelector('[data-customer=\""+row.getString("id")+"\"]').click();true");
                        assertEquals("true",js(scenario,"document.querySelector('.cp-customer-details').textContent.includes('TEST-SHARED')"));shared++;
                        js(scenario,"window.BmdCustomerPortal.back()");
                    }
                }
                pages++;break;
            }
            assertEquals(84,ids.size());assertEquals(1,pages);assertEquals(2,shared);
            js(scenario,"window.continuousFirstTile=document.querySelector('.cp-customer');document.querySelector('.cp-next').click();true");
            assertEquals("true",js(scenario,"document.querySelectorAll('.cp-customer').length===84&&document.querySelector('.cp-customer')===window.continuousFirstTile"));
            assertEquals("1",js(scenario,"window.BmdCustomerPortal.snapshot().page"));
            long imagesEnd=SystemClock.elapsedRealtime()+5000;while(SystemClock.elapsedRealtime()<imagesEnd&&!"true".equals(js(scenario,"Array.from(document.images).every(i=>i.complete&&i.naturalWidth>0)")))SystemClock.sleep(100);
            assertEquals("true",js(scenario,"Array.from(document.images).every(i=>i.complete&&i.naturalWidth>0)"));
            shot(scenario,"portal-continuous-list");
        }
    }
    @Test public void allSixThemesAndTenLanguagesFitTheRealPhoneViewport()throws Exception{
        for(String theme:ThemeSkin.IDS){
            try(ActivityScenario<CustomerPortalActivity> scenario=launch(theme,"nl")){
                ready(scenario);
                for(String language:MainActivity.LANGS){
                    assertEquals("true",js(scenario,"window.BmdCustomerPortal.applyAppearance('"+theme+"','"+language+"',"+JSONObject.quote(brand(language))+")"));
                    assertEquals(JSONObject.quote(theme),js(scenario,"document.querySelector('.cp-screen').dataset.theme"));
                    assertEquals(JSONObject.quote(language),js(scenario,"document.querySelector('.cp-screen').lang"));
                    assertEquals("true",js(scenario,"Array.from(document.querySelectorAll('.cp-name,.cp-shipping-group,.cp-country,.cp-title,.cp-nav,.cp-appbar'),e=>!e.getClientRects().length||e.scrollWidth<=e.clientWidth+2).every(Boolean)"));
                    assertEquals("true",js(scenario,"Array.from(document.querySelectorAll('.cp-customer'),c=>c.querySelector('.cp-country').getBoundingClientRect().bottom<=c.querySelector('.cp-name').getBoundingClientRect().top+1&&c.querySelector('.cp-name').getBoundingClientRect().bottom<=c.querySelector('.cp-shipping-group').getBoundingClientRect().top+1).every(Boolean)"));
                    assertEquals("Customer captions stay inside their own tile","true",js(scenario,"Array.from(document.querySelectorAll('.cp-customer'),c=>{const b=c.getBoundingClientRect();return Array.from(c.querySelectorAll('.cp-name,.cp-country,.cp-shipping-group'),e=>{const r=e.getBoundingClientRect();return r.left>=b.left-1&&r.right<=b.right+1&&r.top>=b.top-1&&r.bottom<=b.bottom+1;}).every(Boolean);}).every(Boolean)"));
                }
                js(scenario,"window.BmdCustomerPortal.applyAppearance('"+theme+"','nl','ScanTovenaar')");
                shot(scenario,"portal-theme-"+theme);
            }
        }
    }
    // All 8 themes x 15 languages x 6 shipping cases run here; each WebView reply
    // still has its own 8-second limit. Allow the complete matrix to finish.
    @Test(timeout=600000) public void shippingTermsAndProviderStayInSeparateColumnsAcrossThemesAndLanguages()throws Exception{
        JSONObject data=new JSONObject(fixture(6));JSONArray rows=data.getJSONArray("customers");
        rows.getJSONObject(0).put("shipping",new JSONArray().put("EX-WORKS").put("Customer arranges Pick-UP - Forwarder"));
        rows.getJSONObject(1).put("shipping",new JSONArray().put("EX-WORKS").put("Customer arranges Pick-UP - DHL"));
        rows.getJSONObject(2).put("shipping",new JSONArray().put("Delivered Duty Paid").put("Use Blue Medical account"));
        rows.getJSONObject(3).put("shipping",new JSONArray().put("EX-WORKS").put("FedEx").put("999000111"));
        rows.getJSONObject(4).put("shipping",new JSONArray().put("EX-WORKS").put("Customer arranges Pick-UP - Forwarder/UPS"));
        rows.getJSONObject(5).put("shipping",new JSONArray().put("EX-WORKS").put("Customer arranges Pick-UP - Forwarder/DHL"));
        register().importText(data.toString());
        for(String theme:ThemeSkin.IDS){
            try(ActivityScenario<CustomerPortalActivity> scenario=launch(theme,"nl")){
                ready(scenario);
                for(String language:MainActivity.LANGS){
                    js(scenario,"window.BmdCustomerPortal.applyAppearance('"+theme+"','"+language+"',"+JSONObject.quote(brand(language))+")");
                    String result=js(scenario,"(()=>{for(let mode=0;mode<6;mode++){document.querySelector('[data-customer=\"fixture-'+mode+'\"]').click();const card=document.querySelector('.cp-shipping-summary'),term=card.querySelector('.cp-shipping-terms'),main=card.querySelector('.cp-shipping-route-main'),detail=card.querySelector('.cp-shipping-route-detail'),routes=card.querySelector('.cp-shipping-routes');if(!term||!main)return 'missing shipping fields '+mode;const t=term.getBoundingClientRect(),r=routes.getBoundingClientRect(),c=card.getBoundingClientRect();if(t.right>r.left+1||r.right>c.right+1||card.scrollWidth>card.clientWidth+1)return 'shipping columns overflow '+mode;if(main.scrollWidth>main.clientWidth+1||detail&&detail.scrollWidth>detail.clientWidth+1)return 'shipping text overflow '+mode;if(detail){const m=main.getBoundingClientRect(),d=detail.getBoundingClientRect();if(d.top<m.bottom-1||Math.abs(d.right-m.right)>1)return 'provider is not below and right aligned '+mode;}if(mode===0&&(!detail||!detail.textContent.trim()))return 'forwarder missing';if(mode===1&&(!detail||detail.textContent!=='DHL'||card.textContent.includes('UPS')))return 'pickup carrier changed';if(mode===2&&(term.textContent!=='Delivered Duty Paid'||card.textContent.includes('EX-WORKS')||card.textContent.includes('UPS')||detail))return 'DDP account has invented carrier';if(mode===3&&(!detail||!detail.textContent.includes('999000111')))return 'account number missing';if(mode>=4&&(!detail||!detail.textContent.endsWith(mode===4?'/UPS':'/DHL')||!['nl','en'].includes(document.querySelector('.cp-screen').lang)&&detail.textContent.includes('Forwarder')))return 'compound pickup provider is not localized '+mode;window.BmdCustomerPortal.back();}return true;})()");
                    assertEquals("Shipping row in "+theme+" / "+language,"true",result);
                }
                js(scenario,"window.BmdCustomerPortal.applyAppearance('"+theme+"','nl','ScanTovenaar');document.querySelector('[data-customer=\"fixture-0\"]').click();true");
                shot(scenario,"portal-shipping-layout-"+theme);
            }
        }
    }
    @Test public void shippingDocumentsKeepTheirOrderPouchesAndSignatureRequirements()throws Exception{
        JSONObject data=new JSONObject(fixture(1));JSONObject row=data.getJSONArray("customers").getJSONObject(0);
        row.put("shipping",new JSONArray().put("CoO (Certificate of Origin)").put("EUR.1").put("Shipmentlist").put("Costumer Invoice").put("Invoice NEFEZA in Excel format").put("HAWB (House Air Way Bill)").put("CoA (Certificate of Analysis)"));
        row.put("extraShipping",new JSONArray().put("EX-WORKS").put("* Customer arranges Pick-UP").put("Documents directly to costumer in DHL envelope:").put("Shipmentlist (Signature + Stamp)").put("Invoice (Signature + Stamp)").put("Original CoO (Signature + Stamp)").put("Original CoA (Signature + Stamp)").put("Copy EUR.1 (Signature + Stamp)"));
        row.put("documents",new JSONArray().put("Documents on top of Box").put("3x Shipmentlist (Signature+Stamp)").put("3x Invoice (Signature+Stamp)").put("Original EUR.1 (Signature + Stamp)").put("No Documents in box!"));
        register().importText(data.toString());
        try(ActivityScenario<CustomerPortalActivity> scenario=launch("bmd","nl")){
            ready(scenario);js(scenario,"document.querySelector('.cp-customer').click();document.querySelector('[data-shipping-tab=\"2\"]').click();true");
            assertEquals(JSONObject.quote("Benodigde documenten"),js(scenario,"document.querySelector('[data-shipping-tab=\"2\"]').textContent.replace(/\\u00ad/g,'').trim()"));
            assertEquals("true",js(scenario,"Array.from(document.querySelectorAll('.cp-shipping-panel:not([hidden]) li'),e=>e.textContent).join('|')==='CoO|EUR.1|CoC|Verzendlijst|Klantfactuur|NEFEZA-factuur in Excel-formaat|(H)AWB'"));
            js(scenario,"document.querySelector('[data-shipping-tab=\"3\"]').click();true");
            assertEquals("true",js(scenario,"Array.from(document.querySelectorAll('.cp-pouch-item>span'),e=>e.textContent).join('|')==='3 × Factuur|3 × Verzendlijst|Originele EUR.1'"));
            assertEquals("true",js(scenario,"document.querySelectorAll('.cp-pouch-item small').length===3&&Array.from(document.querySelectorAll('.cp-pouch-item small'),e=>e.textContent==='Handtekening + stempel').every(Boolean)"));
            shot(scenario,"portal-document-pouches");
            js(scenario,"document.querySelector('[data-shipping-tab=\"4\"]').click();true");
            assertEquals("true",js(scenario,"document.querySelectorAll('.cp-requirement-item small').length===5&&Array.from(document.querySelectorAll('.cp-requirement-item small'),e=>parseFloat(getComputedStyle(e).fontSize)<parseFloat(getComputedStyle(e.parentElement.querySelector('span')).fontSize)).every(Boolean)"));
            shot(scenario,"portal-extra-info");
            js(scenario,"document.querySelector('[data-detail-tab=\"1\"]').click();true");
            assertEquals("true",js(scenario,"document.querySelector('.cp-customer-details').getClientRects().length===0&&document.querySelector('.cp-order-empty').getClientRects().length>0"));
        }
    }
    @Test public void detailNamesBackAndRecreationPreserveTheSelectedCustomer()throws Exception{
        try(ActivityScenario<CustomerPortalActivity> scenario=launch("translumina","en")){
            ready(scenario);js(scenario,"document.querySelector('.cp-next').click();true");SystemClock.sleep(800);
            js(scenario,"document.querySelector('[data-list-index=\"8\"]').click();true");
            String title=js(scenario,"document.querySelector('#cp-detail-title').textContent");
            assertEquals("true",js(scenario,"document.querySelector('.cp-grid').hidden"));
            assertEquals("true",js(scenario,"window.BmdCustomerPortal.snapshot().selected!==null"));
            assertEquals("true",js(scenario,"(()=>{const email=document.querySelector('.cp-email-text');return email.textContent==='customer.register@example.invalid'&&getComputedStyle(email).whiteSpace==='nowrap'&&email.scrollWidth<=email.clientWidth+2;})()"));
            assertEquals("2",js(scenario,"document.querySelectorAll('.cp-detail-tab').length"));
            assertEquals("5",js(scenario,"document.querySelectorAll('.cp-shipping-tab').length"));
            js(scenario,"document.querySelector('[data-shipping-tab=\"4\"]').click();true");
            assertEquals("4",js(scenario,"window.BmdCustomerPortal.snapshot().shippingTab"));
            SystemClock.sleep(200);scenario.recreate();ready(scenario);
            assertEquals(title,js(scenario,"document.querySelector('#cp-detail-title').textContent"));
            assertEquals("true",js(scenario,"document.querySelector('.cp-grid').hidden"));
            assertEquals("4",js(scenario,"window.BmdCustomerPortal.snapshot().shippingTab"));
            assertEquals("1",js(scenario,"document.querySelectorAll('.cp-detail-panel:not([hidden])').length"));
            js(scenario,"document.querySelector('[data-detail-tab=\"1\"]').click();true");
            assertEquals("true",js(scenario,"document.querySelector('.cp-customer-details').getClientRects().length===0"));
            SystemClock.sleep(200);scenario.recreate();ready(scenario);
            assertEquals("1",js(scenario,"window.BmdCustomerPortal.snapshot().detailTab"));
            assertEquals("true",js(scenario,"document.querySelector('.cp-customer-details').getClientRects().length===0&&!document.querySelector('.cp-order-empty').hidden"));
            scenario.onActivity(a->{View back=DeviceSupport.tag(a.getWindow().getDecorView(),"customer-portal-back");assertNotNull(back);back.performClick();});
            long end=SystemClock.elapsedRealtime()+5000;while(SystemClock.elapsedRealtime()<end&&"true".equals(js(scenario,"document.querySelector('.cp-grid').hidden")))SystemClock.sleep(100);
            assertEquals("false",js(scenario,"document.querySelector('.cp-grid').hidden"));
            assertEquals("1",js(scenario,"window.BmdCustomerPortal.snapshot().page"));
            shot(scenario,"portal-return-to-list");
        }
    }
    @Test public void emptyRegisterImportCancellationAndTranslations()throws Exception{
        new File(DeviceSupport.context().getNoBackupFilesDir(),CustomerRegister.FILE_NAME).delete();
        try(ActivityScenario<CustomerPortalActivity> scenario=launch("bmd","nl")){
            ready(scenario);
            assertEquals("0",js(scenario,"document.querySelectorAll('.cp-customer').length"));
            assertEquals("true",js(scenario,"document.querySelector('.cp-paging').hidden"));
            for(String language:MainActivity.LANGS){
                js(scenario,"window.BmdCustomerPortal.applyAppearance('bmd','"+language+"',"+JSONObject.quote(brand(language))+")");
                assertEquals("true",js(scenario,"document.querySelector('.cp-import-button').textContent.length>5"));
                assertEquals("true",js(scenario,"document.querySelector('.cp-import-button').scrollWidth<=document.querySelector('.cp-import-button').clientWidth+2"));
            }
            js(scenario,"document.querySelector('.cp-import-button').click();true");
            UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());device.waitForIdle();SystemClock.sleep(500);device.pressBack();ready(scenario);
            assertEquals("0",js(scenario,"document.querySelectorAll('.cp-customer').length"));
            shot(scenario,"portal-private-import");
        }
    }
    @Test public void validDocumentImportPersistsAndInvalidImportKeepsPriorData()throws Exception{
        File input=new File(DeviceSupport.context().getCacheDir(),"synthetic-register.json");
        try(ActivityScenario<CustomerPortalActivity> scenario=launch("bmd","nl")){
            ready(scenario);Files.write(input.toPath(),fixture(1).getBytes(StandardCharsets.UTF_8));
            scenario.onActivity(a->a.importCustomers(Uri.fromFile(input)));
            long end=SystemClock.elapsedRealtime()+15000;
            while(SystemClock.elapsedRealtime()<end&&!"1".equals(js(scenario,"document.querySelectorAll('.cp-customer').length")))SystemClock.sleep(100);
            assertEquals("1",js(scenario,"document.querySelectorAll('.cp-customer').length"));
            Files.write(input.toPath(),"{\"schema\":99}".getBytes(StandardCharsets.UTF_8));
            scenario.onActivity(a->a.importCustomers(Uri.fromFile(input)));
            end=SystemClock.elapsedRealtime()+10000;
            while(SystemClock.elapsedRealtime()<end&&!"false".equals(js(scenario,"document.querySelector('.cp-import-error').hidden")))SystemClock.sleep(100);
            assertEquals("false",js(scenario,"document.querySelector('.cp-import-error').hidden"));
            assertEquals(1,register().read().length());scenario.recreate();ready(scenario);
            assertEquals("1",js(scenario,"document.querySelectorAll('.cp-customer').length"));
            for(String invalid:new String[]{fixture(1).replace("fixture-0","bad'id"),fixture(1).replace("NL","ZZ"),fixture(1).replace("eu","unknown"),fixture(1).replace("\"Demo Client 0\"","{}")}){
                try{register().importText(invalid);fail("Invalid document accepted");}catch(IllegalArgumentException expected){}
                assertEquals(1,register().read().length());
            }
            try{CustomerRegister.readText(new java.io.ByteArrayInputStream(new byte[CustomerRegister.MAX_BYTES+1]));fail("Oversized input accepted");}catch(IllegalArgumentException expected){}
        }finally{input.delete();}
    }
    private void search(ActivityScenario<CustomerPortalActivity> scenario,String value)throws Exception{
        js(scenario,"(()=>{const input=document.querySelector('.cp-search-input');input.value="+JSONObject.quote(value)+";input.dispatchEvent(new Event('input',{bubbles:true}));return true;})()");
    }
    @Test public void searchFindsNamesCountriesAndLinkedNumbersAndSurvivesRecreation()throws Exception{
        try(ActivityScenario<CustomerPortalActivity> scenario=launch("bmd","nl")){
            ready(scenario);
            assertEquals("0",js(scenario,"document.querySelectorAll('.cp-wordmark').length"));
            assertEquals(JSONObject.quote("Klantenportaal"),js(scenario,"document.querySelector('.cp-title').textContent.trim()"));
            search(scenario,"Demo Client 83");
            assertEquals("1",js(scenario,"document.querySelectorAll('.cp-customer').length"));
            js(scenario,"document.querySelector('.cp-customer').click();true");
            assertEquals(JSONObject.quote("Synthetic Customer 83"),js(scenario,"document.querySelector('#cp-detail-title').textContent"));
            js(scenario,"window.BmdCustomerPortal.back()");
            assertEquals(JSONObject.quote("Demo Client 83"),js(scenario,"document.querySelector('.cp-search-input').value"));
            search(scenario,"TEST-SHARED");
            assertEquals("2",js(scenario,"document.querySelectorAll('.cp-customer').length"));
            SystemClock.sleep(150);scenario.recreate();ready(scenario);
            assertEquals("2",js(scenario,"document.querySelectorAll('.cp-customer').length"));
            assertEquals(JSONObject.quote("TEST-SHARED"),js(scenario,"document.querySelector('.cp-search-input').value"));
            search(scenario,"Zwitserland");
            assertEquals("true",js(scenario,"document.querySelector('.cp-total').textContent.startsWith('42 ')"));
            search(scenario,"__no_customer_matches__");
            assertEquals("0",js(scenario,"document.querySelectorAll('.cp-customer').length"));
            assertEquals("false",js(scenario,"document.querySelector('.cp-no-results').hidden"));
            assertEquals("true",js(scenario,"document.querySelector('.cp-paging').hidden"));
            search(scenario,"");
            assertEquals("Clearing search restores the complete continuous customer list","84",js(scenario,"document.querySelectorAll('.cp-customer').length"));
            assertEquals("false",js(scenario,"document.querySelector('.cp-next').disabled"));
        }
    }
    @Test public void homeTileOpensThePortalInsteadOfWorkInProgress()throws Exception{
        DeviceSupport.clear();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{View tile=DeviceSupport.tag(a.getWindow().getDecorView(),"photo");assertNotNull(tile);tile.performClick();});
            UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            assertTrue("Home customer tile opens the real portal",device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.desc("Terug")),15000));
            device.pressBack();
        }
    }
}
