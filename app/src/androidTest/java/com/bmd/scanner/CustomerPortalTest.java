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
            .put("customerId",i<2?"TEST-SHARED":"TEST-"+i).put("country",i%2==0?"NL":"CH").put("shippingGroup",i%2==0?"eu":"non-eu"));
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
                        assertEquals("true",js(scenario,"document.querySelector('.cp-fields details').textContent.includes('TEST-SHARED')"));shared++;
                        js(scenario,"window.BmdCustomerPortal.back()");
                    }
                }
                pages++;if("true".equals(js(scenario,"document.querySelector('.cp-next').disabled")))break;
                js(scenario,"document.querySelector('.cp-next').click();true");
            }
            assertEquals(84,ids.size());assertEquals(11,pages);assertEquals(2,shared);
            long imagesEnd=SystemClock.elapsedRealtime()+5000;while(SystemClock.elapsedRealtime()<imagesEnd&&!"true".equals(js(scenario,"Array.from(document.images).every(i=>i.complete&&i.naturalWidth>0)")))SystemClock.sleep(100);
            assertEquals("true",js(scenario,"Array.from(document.images).every(i=>i.complete&&i.naturalWidth>0)"));
            shot(scenario,"portal-last-page");
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
    @Test public void detailNamesBackAndRecreationPreserveTheSelectedCustomer()throws Exception{
        try(ActivityScenario<CustomerPortalActivity> scenario=launch("translumina","en")){
            ready(scenario);js(scenario,"document.querySelector('.cp-next').click();document.querySelector('.cp-customer').click();true");
            String title=js(scenario,"document.querySelector('#cp-detail-title').textContent");
            assertEquals("true",js(scenario,"document.querySelector('.cp-grid').hidden"));
            assertEquals("true",js(scenario,"window.BmdCustomerPortal.snapshot().selected!==null"));
            SystemClock.sleep(200);scenario.recreate();ready(scenario);
            assertEquals(title,js(scenario,"document.querySelector('#cp-detail-title').textContent"));
            assertEquals("true",js(scenario,"document.querySelector('.cp-grid').hidden"));
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
            assertEquals("8",js(scenario,"document.querySelectorAll('.cp-customer').length"));
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
