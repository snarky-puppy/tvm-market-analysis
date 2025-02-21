package com.tvminvestments.zscore.scenario;


/**
 * Created by horse on 5/11/14.
 */
public class Scenario {
    public String name;

    // mkII
    public int subScenario;
    public int sampleStart;
    public int trackingStart;
    public int trackingEnd;

    // scenarios mkII
    public Scenario(String name, int subScenario, int sampleStart, int trackingStart, int trackingEnd) {
        this.name = name;
        this.subScenario = subScenario;
        this.sampleStart = sampleStart;
        this.trackingStart = trackingStart;
        this.trackingEnd = trackingEnd;
    }

    @Override
    public String toString() {
        return "Scenario{" +
                "subScenario=" + subScenario +
                ", sampleStart=" + sampleStart +
                ", trackingStart=" + trackingStart +
                ", trackingEnd=" + trackingEnd +
                ", name='" + name + '\'' +
                '}';
    }
}
