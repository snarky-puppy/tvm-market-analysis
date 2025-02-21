package com.tvm;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by matt on 6/02/17.
 */
public class Guru {
    private static final Logger logger = LoggerFactory.getLogger(Guru.class);

    WebClient getWebClient() {
        WebClient client = new WebClient(BrowserVersion.CHROME, "127.0.0.1", 3128);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.getOptions().setCssEnabled(false);
        return client;
    }

    public void scrape() throws IOException {
        String loginUrl = "http://www.gurufocus.com/forum/login.php?0";
        String url = "http://www.gurufocus.com/ListGuru.php";

        WebClient webClient = getWebClient();
        HtmlPage page = webClient.getPage(loginUrl);
        HtmlForm form = page.getForms().get(0);
        form.getInputByName("username").setValueAttribute("vinniek");
        form.getInputByName("password").setValueAttribute("tvmtvmtvm1");
        page = form.getInputByValue("Log In").click();

        System.out.println("new page="+page);

        page = webClient.getPage(url);
        List<?> list = page.getByXPath("//a[@class='gurunames']");
        for(Object obj : list) {
            HtmlAnchor a = (HtmlAnchor)obj;

            String fileName = a.getHrefAttribute().replace("/StockBuy.php?GuruName=", "");
            fileName = fileName + ".csv";

            if(new File(fileName).exists()) {
                System.out.println(fileName+" exists, continuing");
                continue;
            }

            System.out.println(a);
            page = a.click();

            try {
                a = page.getAnchorByText("Download");
                System.out.println(a);
                IOUtils.copy(a.click().getWebResponse().getContentAsStream(), new FileOutputStream(fileName));
            } catch(ElementNotFoundException e) {
                System.out.println("No download link found");
            }


        }
    }

    public static void main(String[] args) throws IOException {
        Guru guru = new Guru();
        guru.scrape();
    }
}
