package com.tvm;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by matt on 6/02/17.
 */
public class GuruFinancialsBatch {
    private static final Logger logger = LoggerFactory.getLogger(GuruFinancialsBatch.class);

    String url = "http://www.gurufocus.com/download_financials_batch.php";


    WebClient getWebClient() {
        WebClient client = new WebClient(BrowserVersion.CHROME);//, "127.0.0.1", 3128);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.getOptions().setCssEnabled(false);
        return client;
    }

    List<String> getSymbols() {
        try {
            List<String> rv = new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader("input/symbols-MS-5.csv"));
            boolean first = true;
            String line;

            while((line = br.readLine()) != null) {
                if(first) {
                    first = false;
                    continue;
                }
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

    public void scrape() throws IOException, InterruptedException {
        String loginUrl = "http://www.gurufocus.com/forum/login.php?0";

        List<String> symbols = getSymbols();

        WebClient webClient = getWebClient();
        HtmlPage page = webClient.getPage(loginUrl);
        HtmlForm form = page.getForms().get(0);
        form.getInputByName("username").setValueAttribute("vinniek");
        form.getInputByName("password").setValueAttribute("tvmtvmtvm1");
        page = form.getInputByValue("Log In").click();

        System.out.println("new page="+page);

        ExecutorService executorService = Executors.newFixedThreadPool(32);

        for(String symbol : symbols) {
            String file = symbol.replace("/", "_")+".xls";
            if(Files.exists(Paths.get(file))) {
                System.out.println(file+" exists");
                continue;
            }
            executorService.submit(new Runnable() {
                @Override
                public void run() {

                    scrapeSymbol(webClient, symbol, file);
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);


    }

    public void scrapeSymbol(WebClient webClient, String symbol, String file) {
        try {
            HtmlPage page = webClient.getPage(url);
            HtmlForm form = page.getFirstByXPath("//form[@id='fm_download1']");

            System.out.println("Downloading "+symbol);
            HtmlTextArea area = form.getTextAreaByName("symbols");
            area.setText(symbol);
            HtmlSubmitInput button = page.getFirstByXPath("//input[@id='button_download1']");
            IOUtils.copy(button.click().getWebResponse().getContentAsStream(), new FileOutputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        GuruFinancialsBatch guru = new GuruFinancialsBatch();
        guru.scrape();
    }
}
