package com.tvm;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by horse on 1/2/17.
 */
public class YahooEFT {

    public static void main(String[] args) {
        YahooDatabase db;
        try {
            db = new YahooDatabase();
            db.update();
            db.export();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
