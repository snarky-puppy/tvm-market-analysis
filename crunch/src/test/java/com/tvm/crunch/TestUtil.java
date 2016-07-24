package com.tvm.crunch;

import java.io.File;
import java.util.Scanner;

/**
 * Created by horse on 24/07/2016.
 */
public class TestUtil {

    public File getFile(String fileName) {

        //Get file from resources folder
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(fileName).getFile());





    }
}
