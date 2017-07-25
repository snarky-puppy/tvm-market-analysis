package com.tvm.yahoo;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by horse on 26/7/17.
 */
public class News {
    private static final Logger logger = LoggerFactory.getLogger(News.class);

    private String URL = "https://au.finance.yahoo.com/q/h?s=%s";

    private Map<String, List<String>> categories = new HashMap<>();

    WebClient getWebClient() {
        WebClient client = new WebClient(BrowserVersion.CHROME); //, "127.0.0.1", 3128);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.getOptions().setCssEnabled(false);
        return client;
    }

    private void loadCategories() {
        Map<String, List<String>> categories = new HashMap<>();

        for(File file : new File("YahooCompanyData").listFiles()) {

            if(file.isDirectory())
                continue;

            String fileName = file.toString();
            System.out.println(file.toString());

            String category = FilenameUtils.getBaseName(fileName);

            categories.put(category, getSymbols(fileName));
        }
    }

    public static List<String> getSymbols(String fileName) {
        try {
            List<String> rv = new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line;

            while((line = br.readLine()) != null) {
                String[] data = line.split("[,\\t]");
                if(data[0] != null)
                    rv.add(data[0]);
            }

            return rv;

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    private void scrape() throws IOException {
        WebClient webClient = getWebClient();

        for(String category : categories.keySet()) {
            for(String symbol : categories.get(category)) {
                HtmlPage page = webClient.getPage(String.format(URL, symbol));
                page.getElem
            }
        }
    }


    public static void main(String[] args) throws IOException {
        News news = new News();
        news.scrape();
    }
}
