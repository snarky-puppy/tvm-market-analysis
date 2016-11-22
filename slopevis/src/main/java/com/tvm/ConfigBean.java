package com.tvm;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import java.io.IOException;
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
        pointDistances = new ArrayList<>();
    }

    public Path dataDir;
    public double minDolVol;
    public int daysDolVol;
    public double slopeCutoff;
    public double maxHoldDays;

    public double stopPc;
    public double targetPc;
    public int tradeStartDays;

    public boolean debug = false;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using=LocalDateDeserializer.class)
    public LocalDate fromDate;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using=LocalDateDeserializer.class)
    public LocalDate toDate;

    public List<String> symbols;
    public List<Integer> pointDistances;


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
        sb.append(", tradeStartDays=").append(tradeStartDays);
        sb.append(", debug=").append(debug);
        sb.append(", fromDate=").append(fromDate);
        sb.append(", toDate=").append(toDate);
        sb.append(", symbols=").append(symbols);
        sb.append(", pointDistances=").append(pointDistances);
        sb.append('}');
        return sb.toString();
    }
}
