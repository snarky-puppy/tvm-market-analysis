package com.newsflash.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {

	public static Properties prop = new Properties();
	
	static {
		
		try {
			FileInputStream input = new FileInputStream("config.properties");
			prop.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getString(String key) {
		return prop.getProperty(key);
	}
}
