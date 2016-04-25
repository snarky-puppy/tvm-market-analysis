package com.newsflash;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.newsflash.util.ConfigLoader;
import com.newsflash.util.SysUtils;

public class App 
{
	
	private static final Logger LOGGER = Logger.getLogger(App.class);
	
	public static final String companies_file = ConfigLoader.getString("companies_file");
	public static final String keywords_file = ConfigLoader.getString("keywords_file");
	
	public static final String output_file = ConfigLoader.getString("output_file");
	
	static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
	static final SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy");
	
    public static void main( String[] args )
    {
    	LOGGER.info("google search scraper starting...");
    	
    	try {
    		
    		LOGGER.info("companies_file=" + companies_file);
    		LOGGER.info("keywords_file=" + keywords_file);
    		LOGGER.info("output_file=" + output_file);
    		
    		// load keywords list
    		List<String> keywords = Files.readAllLines(Paths.get(keywords_file));
    		
    		CsvWriter csvWriter = new CsvWriter(output_file);
    		csvWriter.writeRecord(new String[] {"Company", "Matching keyword(s)", "Title", "Publish Date", "URL"});
    		
    		// read csv files
    		CsvReader csvReader = new CsvReader(companies_file);
    		csvReader.readHeaders();
    		
    		while (csvReader.readRecord()) {
    			
    			try {
	    			String company = csvReader.get(0);
	    			String startDate = csvReader.get(1);
	    			String endDate = csvReader.get(2);
	    			
	    			LOGGER.info("company=" + company);
	    			LOGGER.info("startDate=" + startDate);
	    			LOGGER.info("endDate=" + endDate);
	    			
	    			Date sDate = sdf.parse(startDate);
	    			Date eDate = sdf.parse(endDate);
	    			
	    			String date1 = sdf1.format(sDate);
	    			String date2 = sdf1.format(eDate);
	    			
	    			String URL = "https://www.google.com/search?q=" + company + "&tbs=cdr:1,cd_min:" + date1 + ",cd_max:" + date2;
	    			
	    			LOGGER.info("URL=" + URL);
	    			
	    			HtmlPage page = getPage(URL);
	    			
	    			List<HtmlElement> results = (List<HtmlElement>) page.getByXPath("//div[@class='g']");
	    			
	    			// for each result
	    			for (HtmlElement result: results) {
	    				
	    				HtmlElement h3 = result.getFirstByXPath(".//h3[@class='r']//a");
	    				String title = h3.asText();
	    				String matchingKeywords = "";
	    				
	    				for (String keyword: keywords) {
	    					if (title.toLowerCase().contains(keyword.toLowerCase())) {
	    						
	    						matchingKeywords = keyword;
	    						//break;
	    						
	    					}
	    				}
	    				
//	    				String href = h3.getAttribute("href");
	    				String pageURL = h3.getAttribute("href");
	    				String publishDate = "";
	    				try {
	    					HtmlElement f = result.getFirstByXPath(".//div[@class='s']//span[@class='st']//span[@class='f']");
	    					publishDate = f.asText().replace("-", "").trim();
	    				} catch (Exception e) {
	    					
	    				}
	    				
	    				System.out.println(company);
	    				System.out.println(matchingKeywords);
	    				System.out.println(title);
	    				System.out.println(publishDate);
	    				System.out.println(pageURL);
	    				
	    				csvWriter.writeRecord(new String[] {company, matchingKeywords, title, publishDate, pageURL});
	    				csvWriter.flush();
	    			}

	    			// random delay from 0 to 10 seconds
	    			int delayTime = new Random().nextInt(10);
	    			
	    			LOGGER.info("random delayTime=" + delayTime + " second(s)");
	    			
	    			Thread.sleep(delayTime*1000);
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    			
    		}
    		
    		csvReader.close();
    		
    		csvWriter.close();
    	} catch (Exception e) {
    		LOGGER.error(SysUtils.getStackTrace(e));
    	}
    	
    	LOGGER.info("google search scraper done");
    }
    
    public static WebClient getRandomWebClient() {
    	BrowserVersion version[] = new BrowserVersion[]{BrowserVersion.FIREFOX_45, BrowserVersion.CHROME, BrowserVersion.FIREFOX_38, BrowserVersion.INTERNET_EXPLORER_11, BrowserVersion.INTERNET_EXPLORER, BrowserVersion.EDGE, BrowserVersion.BEST_SUPPORTED};
		
		int index = new Random().nextInt(version.length);
		BrowserVersion browser = version[index];
		
		LOGGER.info("random browser=" + browser);
		WebClient webClient = new WebClient(browser);
		
		return webClient;
    }
    
    public static HtmlPage getPage(String url) {
    
    	WebClient webClient = getRandomWebClient();
    	
    	HtmlPage page = null;
    	try {
    		page = webClient.getPage(url);
		} catch (FailingHttpStatusCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	webClient.close();
    	
		return page;
    }
    
    public static String extractURL(String href) {
    	
		int beginIndex = href.indexOf("http:");
		int endIndex = href.length();
		
		return href.substring(beginIndex, endIndex);
		
    }
}
