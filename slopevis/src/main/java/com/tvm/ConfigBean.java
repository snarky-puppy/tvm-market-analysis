package com.tvm;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/**
 * Configuration bean
 *
 * Created by horse on 25/10/16.
 */
public class ConfigBean {

    ConfigBean() {
        symbols = new ArrayList<>();
    }

    public Path dataDir;
    public double minDolVol;
    public double daysDolVol;
    public double slopeCutoff;
    public double maxHoldDays;
    public double stopPc;
    public double targetPc;
    public Date fromDate;
    public Date toDate;
    public List<String> symbols;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ConfigBean{");
        sb.append("dataDir=").append(dataDir);
        sb.append(", minDolVol=").append(minDolVol);
        sb.append(", daysDolVol=").append(daysDolVol);
        sb.append(", slopeCutoff=").append(slopeCutoff);
        sb.append(", maxHoldDays=").append(maxHoldDays);
        sb.append(", stopPc=").append(stopPc);
        sb.append(", targetPc=").append(targetPc);
        sb.append(", fromDate=").append(fromDate);
        sb.append(", toDate=").append(toDate);
        sb.append(", symbols=").append(symbols);
        sb.append('}');
        return sb.toString();
    }
}
