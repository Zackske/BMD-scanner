package com.bmd.scanner;

/** Native names, original offline flags and colour-only language ambience. */
final class LanguageCatalog {
    static final String[] CODES={"nl","en","uk","tr","it","de","fr","es","pt","hi","pl","ro","ar","vi","cs"};
    static final String[] NAMES={"Nederlands","English","Українська","Türkçe","Italiano","Deutsch","Français","Español","Português","हिन्दी","Polski","Română","العربية","Tiếng Việt","Čeština"};
    static final int[] FLAGS={R.drawable.picker_flag_nl,R.drawable.picker_flag_en,R.drawable.picker_flag_uk,R.drawable.picker_flag_tr,R.drawable.picker_flag_it,R.drawable.picker_flag_de,R.drawable.picker_flag_fr,R.drawable.picker_flag_es,R.drawable.picker_flag_pt,R.drawable.picker_flag_hi,R.drawable.picker_flag_pl,R.drawable.picker_flag_ro,R.drawable.picker_flag_ar,R.drawable.picker_flag_vi,R.drawable.picker_flag_cs};
    static final int[][] COLOURS={{0xffc73443,0xffe6edf3,0xff2551a1},{0xff204b93,0xffe4edf8,0xffd2364d},{0xff246de7,0xff3196eb,0xfff6ca37},{0xffd8233c,0xffec4054,0xfff5e9ec},{0xff0d9b69,0xffe7eee9,0xffd93447},{0xff29323e,0xffd43540,0xffefb932},{0xff2b55bb,0xffe9eef7,0xffe6434a},{0xffcb3049,0xffedc13d,0xffbd243f},{0xff148357,0xffc52c46,0xffe1b333},{0xffed9b43,0xffe9eeea,0xff26975b},{0xffeef0f3,0xffdc2448,0xffb51c3d},{0xff2453b3,0xffedc43b,0xffd73243},{0xff117d4e,0xffeaf2ec,0xff0a683f},{0xffd8313c,0xffe9c43c,0xffb91e35},{0xffe8edf5,0xffd92f45,0xff2854a4}};
    static int index(String code){for(int i=0;i<CODES.length;i++)if(CODES[i].equals(code))return i;return 0;}
    static boolean rtl(String code){return "ar".equals(code);}
}
