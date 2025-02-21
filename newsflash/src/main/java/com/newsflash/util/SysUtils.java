package com.newsflash.util;
/**
 * This is System Utility Class
 * @author huyhh
 *
 */
import java.io.PrintWriter;
import java.io.StringWriter;

public class SysUtils {

	// Convert an exception stack trace to string
	public static String getStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);

		return sw.toString();
	}

	// Convert an exception stack trace to string
	public static String getStackTrace() {
		StringWriter sw = new StringWriter();
		new Throwable("").printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

}
