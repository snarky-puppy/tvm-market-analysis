package com.tvmresearch.lotus.db.model;

import java.util.List;

/**
 *
 * Created by horse on 7/04/2016.
 */
public interface TriggerDao {

    //void serialise(Trigger trigger);

    List<Trigger> getTodaysTriggers();

    Trigger load(int id);

    int elapsedDays(Trigger trigger);

    void serialise(List<Trigger> list);
}

