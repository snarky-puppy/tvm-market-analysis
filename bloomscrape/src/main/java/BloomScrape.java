import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.BrowserVersion;
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

/**
 * Bloomberg scraper
 *
 * Created by horse on 25/10/16.
 */
public class BloomScrape {

    private static final Logger logger = LogManager.getLogger(BloomScrape.class);

    private static final String outputPath = "data";

    ObjectMapper mapper;
    WebClient client;
    Random random;

    private class BloomData {
        public String symbol;
        public String ticker;
        public String companyName;
        public String market;
        public String sector;
        public String industry;
        public String subIndustry;
    }

    public static void main(String[] args) throws Exception {
        BloomScrape bloomScrape = new BloomScrape();
        bloomScrape.scrapeAll("all_symbols2.txt");
    }

    BloomScrape() {
        mapper = new ObjectMapper();
        random = new Random();
        client = new WebClient(BrowserVersion.CHROME);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setThrowExceptionOnScriptError(false);
    }

    private String getXPathText(HtmlPage page, String xpath) {
        return ((HtmlElement)page.getFirstByXPath(xpath)).getTextContent();
    }

    private void scrapeAll(String inputFile) throws IOException, InterruptedException {

        int cnt = 0;
        List<String> allSymbols = Files.readAllLines(Paths.get(inputFile));

        Files.createDirectories(Paths.get(outputPath));

        for(String stock : allSymbols) {
            Path file = Paths.get(outputPath, stock+".json");
            if(Files.exists(file))
                continue;

            System.out.println("== Checking "+stock);

            HtmlPage page = client.getPage("http://www.bloomberg.com/quote/"+stock+":US");

            if(page.getFirstByXPath("//*[text()=\" Fund Managers \"]") == null) {

                BloomData data = new BloomData();
                data.symbol = stock;
                data.ticker = getXPathText(page, "//*[@class=\"ticker\"]");
                data.companyName = getXPathText(page, "//*[@class=\"name\"]");
                data.market = getXPathText(page, "//*[@class=\"exchange\"]");
                data.sector = getXPathText(page, "//*[text()=\" Sector \"]/following-sibling::div");
                data.industry = getXPathText(page, "//*[text()=\" Industry \"]/following-sibling::div");
                data.subIndustry = getXPathText(page, "//*[text()=\" Sub-Industry \"]/following-sibling::div");

                mapper.writeValue(file.toFile(), data);
            } else
                Files.write(file, "{}\n".getBytes());

            int delayTime = 20+random.nextInt(10);
            System.out.printf("Sleeping %d seconds\n", delayTime);
            Thread.sleep(delayTime*1000);

            if(cnt++ > 100) {
                System.out.printf("Sleeping an additional 5 minutes\n");
                Thread.sleep(1000*60*5);
                cnt = 0;
            }
        }

    }
}
