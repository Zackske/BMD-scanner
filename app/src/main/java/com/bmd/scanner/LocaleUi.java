package com.bmd.scanner;

import android.app.Activity;
import android.view.View;

final class LocaleUi {
    static void apply(Activity activity,String language){activity.getWindow().getDecorView().setLayoutDirection(LanguageCatalog.rtl(language)?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);}
}
