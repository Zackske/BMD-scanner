package com.bmd.scanner;

/** The same ordered modes drive the toolbar, settings and physical-code decoder. */
final class ScanModes {
    static final String[] ORDER={"qr","bar","both"};
    static String normalize(String value){for(String mode:ORDER)if(mode.equals(value))return mode;return "both";}
    static int index(String value){String valid=normalize(value);for(int i=0;i<ORDER.length;i++)if(ORDER[i].equals(valid))return i;return 2;}
    static String next(String value){return ORDER[(index(value)+1)%ORDER.length];}
}

