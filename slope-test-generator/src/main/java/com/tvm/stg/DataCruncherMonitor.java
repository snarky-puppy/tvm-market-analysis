package com.tvm.stg;

/**
 * Created by matt on 10/02/17.
 */
public interface DataCruncherMonitor {
    void setNumCrunchJobs(int numCrunchJobs);
    void jobFinished();
}
