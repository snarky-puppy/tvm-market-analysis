package com.tvmresearch.lotus.db.model;

import com.tvmresearch.lotus.Database;

import java.sql.Connection;
import java.util.List;

/**
 * Created by horse on 7/04/2016.
 */
public interface TriggerDao {

    //void serialise(Trigger trigger);

    List<Trigger> getTodaysTriggers();

    Trigger load(int id, Connection connection);

    int elapsedDays(Trigger trigger);

    void serialise(List<Trigger> list);

    void serialise(Trigger trigger);
}

