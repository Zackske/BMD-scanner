package com.bmd.scanner;

import java.nio.charset.StandardCharsets;
import java.util.List;

final class CsvData {
    private CsvData() {}
    static byte[] encode(List<String> values){
        StringBuilder out=new StringBuilder("\ufeff");
        for(String value:values)out.append('"').append(value.replace("\"","\"\"")).append('"').append("\r\n");
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }
}
