package com.tvm.crunch.apps;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by horse on 23/07/2016.
 */
public class Test {

    public static void main(String[] args) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
        System.out.println(sdf.format(date));

        List<NewsDB.NewsRow> newsResult = new NewsDB().findNews(20110721, "ORCL");
        if(newsResult != null) {
            for(NewsDB.NewsRow row : newsResult) {
                System.out.println(row.news);
            }
        } else
            System.out.println("no news");


    }
}
