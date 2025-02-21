package com.tvm;

import static com.tvm.Data.append;

/**
 * Created by horse on 25/12/16.
 */
public class Row {
    public String filing;
    public String format;
    public String desc;
    public String date;
    public String fileNum;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        append(sb, filing);
        append(sb, format);
        append(sb, desc);
        append(sb, date);
        append(sb, fileNum);
        return sb.toString();
    }

}
