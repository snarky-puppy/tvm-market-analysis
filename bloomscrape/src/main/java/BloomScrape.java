import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
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

/**
 * Bloomberg scraper
 *
 * Created by horse on 25/10/16.
 */
public class BloomScrape {

    private static final Logger logger = LogManager.getLogger(BloomScrape.class);

    private static final String outputPath = "data";

    ObjectMapper mapper;
    Random random;

    public static void main(String[] args) throws Exception {
        BloomScrape bloomScrape = new BloomScrape();
        bloomScrape.scrapeAll("all_symbols2.txt");
    }

    BloomScrape() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        random = new Random();
    }

    WebClient getWebClient() {
        WebClient client = new WebClient(BrowserVersion.CHROME, "localhost", 3128);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setThrowExceptionOnScriptError(false);
        return client;
    }

    private String getXPathText(HtmlPage page, String xpath) {
        try {
            return ((HtmlElement) page.getFirstByXPath(xpath)).getTextContent().replaceFirst(" ", "");
        } catch(NullPointerException e) {
            System.out.println(page.getWebResponse().getContentAsString());
            throw e;
        }
    }

    private void scrapeAll(String inputFile) throws IOException, InterruptedException {
        int cnt = 0;
        List<String> allSymbols = Files.readAllLines(Paths.get(inputFile));

        Files.createDirectories(Paths.get(outputPath));

        ExecutorService executorService = Executors.newFixedThreadPool(8);


        for(String stock : allSymbols) {
            Path file = Paths.get(outputPath, stock + ".json");
            if(Files.exists(file))
                continue;

            executorService.submit(new Runnable() {
                @Override
                public void run() {

                    try {

                        System.out.println("== Checking " + stock);

                        BloomData data = null;

                        try {
                            data = scrapeStock(stock + ":US");
                        } catch (FailingHttpStatusCodeException e) {
                            System.out.println("== Failed with " + e.getStatusCode());
                            return;
                        }


                        if (data == null)
                            Files.write(file, "{}\n".getBytes());
                        else
                            mapper.writeValue(file.toFile(), data);
                    } catch(Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }});

            /*
            int delayTime = random.nextInt(10);
            System.out.printf("Sleeping %d seconds\n", delayTime);
            //Thread.sleep(delayTime*1000);

            if(cnt++ > 100) {
                System.out.printf("Sleeping an additional 5 minutes\n");
                //Thread.sleep(1000*60*5);
                cnt = 0;
            }
            */

        }
    }

    private BloomData scrapeStock(String stock) throws IOException {
        System.out.println("== GET "+stock);
        BloomData data = new BloomData();
        HtmlPage page = getWebClient().getPage("http://www.bloomberg.com/quote/" + stock);

        data.code = page.getWebResponse().getStatusCode();
        data.symbol = stock;

        if(page.getFirstByXPath("//*[contains(text(), \"The search for\")]") != null) {
            data.code = 404;

        } else if(page.getTitleText().contains("Company Profile - Bloomberg")) {
            data.companyName = getXPathText(page, "//*[@class=\"name\"]");
            data.sector = getXPathText(page, "//*[@class=\"sector\"]").replaceFirst("Sector: ", "");
            data.industry = getXPathText(page, "//*[@class=\"industry\"]").replaceFirst("Industry: ", "");
            data.subIndustry = getXPathText(page, "//*[@class=\"sub_industry\"]").replaceFirst("Sub-Industry: ", "");

        } else if (page.getFirstByXPath("//*[text()=\" Fund Managers \"]") == null) {
            data.ticker = getXPathText(page, "//*[@class=\"ticker\"]");
            data.companyName = getXPathText(page, "//*[@class=\"name\"]");
            data.market = getXPathText(page, "//*[@class=\"exchange\"]");
            data.sector = getXPathText(page, "//*[text()=\" Sector \"]/following-sibling::div");
            data.industry = getXPathText(page, "//*[text()=\" Industry \"]/following-sibling::div");
            data.subIndustry = getXPathText(page, "//*[text()=\" Sub-Industry \"]/following-sibling::div");

            HtmlElement acq = page.getFirstByXPath("//*[@class=\"market-status-message_link\"]");
            if(acq != null) {
                data.acquiredBy = scrapeStock(acq.getTextContent());
            }
        } else
            data = null;

        return data;
    }
}