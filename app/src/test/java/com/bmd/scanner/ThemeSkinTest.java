package com.bmd.scanner;
import org.junit.Test;
import static org.junit.Assert.*;
public class ThemeSkinTest {
    @Test public void everyThemeHasReadableTextAndSecondaryLabels(){for(String id:ThemeSkin.IDS){ThemeSkin s=ThemeSkin.forId(id);assertTrue(id+" foreground",ThemeSkin.contrast(s.text,s.top)>=4.5);assertTrue(id+" detail",ThemeSkin.contrast(s.secondary,s.top)>=4.5);assertTrue(id+" page",ThemeSkin.contrast(s.text,s.background)>=4.5);}}
    @Test public void everyAdvertisedThemeRestoresItsOwnPalette(){assertEquals(8,ThemeSkin.IDS.length);java.util.Set<Integer> accents=new java.util.HashSet<>();for(String id:ThemeSkin.IDS){ThemeSkin skin=ThemeSkin.forId(id);assertEquals(id,skin.id);assertTrue(accents.add(skin.accent));assertTrue(ThemeSkin.labelKey(id).startsWith("theme_"));}}
    @Test public void unknownPreferenceFallsBackToBmd(){assertSame(ThemeSkin.BMD,ThemeSkin.forId(null));assertSame(ThemeSkin.BMD,ThemeSkin.forId("future-theme"));assertSame(ThemeSkin.BMD,ThemeSkin.forId("turquoise"));}
}
