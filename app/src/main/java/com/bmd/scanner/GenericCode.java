package com.bmd.scanner;

import java.net.URI;

/** Raw reader results never become reviewed products or shipment evidence. */
final class GenericCode {
    final String value,format;
    GenericCode(String value,String format){if(value==null||value.isEmpty())throw new IllegalArgumentException("empty code");this.value=value;this.format=format==null?"":format;}
    String key(){return format+"\u0000"+value;}
    /** Only explicit web links have an Open action; other QR payloads remain copyable text. */
    String webLink(){
        try{URI uri=new URI(value.trim());String scheme=uri.getScheme();if(("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme))&&uri.getHost()!=null&&!uri.getHost().isEmpty()&&uri.getRawUserInfo()==null)return uri.toASCIIString();}catch(Exception ignored){}
        return "";
    }
}
