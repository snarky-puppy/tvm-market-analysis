package com.tvm;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

/**
 * Created by matt on 6/02/17.
 */
public class GuruAllSymbolsByRegion {
    private static final Logger logger = LoggerFactory.getLogger(GuruAllSymbolsByRegion.class);

    WebClient getWebClient() {
        WebClient client = new WebClient(BrowserVersion.CHROME);//, "127.0.0.1", 3128);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.getOptions().setCssEnabled(false);
        return client;
    }

    public void scrape() throws IOException {
        String loginUrl = "http://www.gurufocus.com/forum/login.php?0";
        String url = "http://www.gurufocus.com/stock_list.php";

        WebClient webClient = getWebClient();
        HtmlPage page = webClient.getPage(loginUrl);
        HtmlForm form = page.getForms().get(0);
        form.getInputByName("username").setValueAttribute("vinniek");
        form.getInputByName("password").setValueAttribute("tvmtvmtvm1");
        page = form.getInputByValue("Log In").click();

        System.out.println("new page="+page);

        page = webClient.getPage(url);
        HtmlAnchor a = null;

        //BufferedOutputStream bos = new BufferedOutputStream(new File("all_symbols.csv"));
        FileWriter fileWriter = new FileWriter("guru_all_symbols.csv");
        fileWriter.write("Symbol,Company Name,Country\n");
        do {

            HtmlTable table = page.getFirstByXPath("//table[@id=\"R1\"]");
            boolean first = true;
            for(HtmlTableRow row : table.getRows()) {
                if(first) {
                    first = false;
                    continue;
                }
                String pair = row.getCell(0).getTextContent();
                String company = row.getCell(1).getTextContent();

                int dotIdx = pair.lastIndexOf(".");
                if(dotIdx != -1) {
                    pair = new StringBuilder(pair)
                            .replace(dotIdx, dotIdx + 1, ",").toString();
                } else
                    pair = pair + ",US";


                fileWriter.write(String.format("%s,%s\n", pair, company));
            }


            a = page.getFirstByXPath("//img[@alt=\"Next Page\"]/parent::a");
            System.out.println("a="+a);
            if(a != null) {
                try {
                    page = a.click();
                } catch(Exception e) {
                    e.printStackTrace();

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                }
                System.out.println("page="+page);
            }
        } while(a != null);

        fileWriter.close();
    }

    public static void main(String[] args) throws IOException {
        GuruAllSymbolsByRegion guru = new GuruAllSymbolsByRegion();
        guru.scrape();
    }
}
