package com.tvm;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by horse on 25/12/16.
 */
public class Data {
    public int code;
    public String symbol;
    public String company;

    @JsonProperty("rows")
    List<Row> rows = new ArrayList<>();

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();

        if(rows.size() == 0) {
            append(sb, Integer.toString(code));
            append(sb, symbol);
            append(sb, company);
            sb.append("\n");

        } else {
            for(Row r : rows) {
                append(sb, Integer.toString(code));
                append(sb, symbol);
                append(sb, company);
                sb.append(r.toString());
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    static void append(StringBuffer sb, String s) {
        if(s == null)
            sb.append(',');
        else {
            s = s.replaceAll("\n", " ");
            s = s.replaceAll("\"", "");

            sb.append("\""+ s + "\",");
        }
    }
}
