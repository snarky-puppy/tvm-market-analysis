package com.tvm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Created by horse on 25/12/16.
 */
public class EdgarScraper {

    private static final Logger logger = LogManager.getLogger(EdgarScraper.class);

    private static final String outputPath = "data";

    ObjectMapper mapper;
    Random random;

    public static void main(String[] args) throws Exception {
        EdgarScraper scraper = new EdgarScraper();
        scraper.scrapeAll("COMPANIES.csv");
    }

    EdgarScraper() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        random = new Random();
    }

    WebClient getWebClient() {
        WebClient client = new WebClient(BrowserVersion.CHROME);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.getOptions().setCssEnabled(false);
        return client;
    }

    private String getXPathText(HtmlPage page, String xpath) {
        try {
            return ((HtmlElement) page.getFirstByXPath(xpath)).getTextContent().trim();
        } catch(NullPointerException e) {
            //System.out.println(page.getWebResponse().getContentAsString());
            throw e;
        }
    }
    private String getXPathTextSafe(HtmlPage page, String xpath) {
        try {
            HtmlElement el = (HtmlElement) page.getFirstByXPath(xpath);
            if(el == null)
                return "";
            else
                return el.getTextContent().trim();
        } catch(NullPointerException e) {
            //System.out.println(page.getWebResponse().getContentAsString());
            throw e;
        }
    }

    private void scrapeAll(String inputFile) throws IOException, InterruptedException {
        List<String> allSymbols = Files.readAllLines(Paths.get(inputFile));

        Files.createDirectories(Paths.get(outputPath));

        ExecutorService executorService = Executors.newFixedThreadPool(16);

        for(String stock : allSymbols) {
            Path file = Paths.get(outputPath, stock + ".json");
            if (Files.exists(file))
                continue;

            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {

                        scrape(stock, file);
                        //Thread.sleep(30*1000);

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
    }

    private void scrape(String stock, Path file) throws IOException {
        System.out.println("== Checking " + stock);

        Data data = null;

        try {
            data = scrapeStock(stock);
        } catch (FailingHttpStatusCodeException e) {
            int code = e.getStatusCode();
            System.out.println("== Failed with " + code);
            if (code == 400 || code == 403)
                System.exit(1);
            //return;
        }

        if (data == null)
            Files.write(file, "{}\n".getBytes());
        else
            mapper.writeValue(file.toFile(), data);
    }

    private Data scrapeStock(String stock) throws IOException {
        System.out.println("== GET "+stock);
        Data data = new Data();
        HtmlPage page = getWebClient().getPage("https://www.sec.gov/cgi-bin/browse-edgar?company="+stock+"&owner=include&count=100&action=getcompany");

        data.code = page.getWebResponse().getStatusCode();
        data.company = stock;

        if(page.getFirstByXPath("//*[@class=\"noCompanyMatch\"]") != null) {
            data.code = 404;

        } else if(page.getFirstByXPath("//*[@class=\"companyMatch\"]") != null) {
            data.code = 40404;

        } else {

            boolean go = true;
            HtmlForm form;
            int pageNum = 1;

            do {

                System.out.println("Page "+pageNum+" : "+page.getUrl());

                if(page.getForms().size() <= 1) {
                    go = false;
                    continue;
                }

                form = page.getForms().get(1);

                HtmlTable table = page.getFirstByXPath("//*[@id=\"seriesDiv\"]/table");

                boolean header = true;
                for (HtmlTableRow r : table.getRows()) {
                    if(header) {
                        header = false;
                        continue;
                    }
                    Row row = new Row();
                    row.filing = r.getCell(0).asText();
                    row.format = r.getCell(1).asText();
                    row.desc = r.getCell(2).asText();
                    row.date = r.getCell(3).asText();
                    row.fileNum = r.getCell(4).asText();
                    data.rows.add(row);
                }

                List<HtmlInput> list = form.getInputsByValue("Next 100");
                if(list == null || list.size() == 0) {
                    go = false;
                } else {
                    String url = list.get(0).getOnClickAttribute().replace("parent.location='","").replace("'", "");
                    page = getWebClient().getPage("https://www.sec.gov"+url);
                    pageNum++;
                }

                // reached the end
                if(page.getFirstByXPath("//*[text()=\"Invalid parameter\"]") != null) {
                    System.out.println("Reached the end");
                    go = false;
                }
            } while (go);
        }

        return data;
    }
}
