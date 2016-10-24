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

/**
 * Bloomberg scraper
 *
 * Created by horse on 25/10/16.
 */
public class BloomScrape {

    private static final Logger logger = LogManager.getLogger(BloomScrape.class);

    private static final String outputPath = "data";

    private class BloomData {
        public String symbol;
        public String ticker;
        public String companyName;
        public String market;
        public String sector;
        public String industry;
        public String subIndustry;
    }

    public static void main(String[] args) throws IOException {
        BloomScrape bloomScrape = new BloomScrape();
        bloomScrape.scrapeAll("all_symbols2.txt");
    }

    private String getXPathText(HtmlPage page, String xpath) {
        return ((HtmlElement)page.getFirstByXPath(xpath)).getTextContent();
    }

    private void scrapeAll(String inputFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        WebClient client = new WebClient(BrowserVersion.CHROME);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setThrowExceptionOnScriptError(false);

        List<String> allSymbols = Files.readAllLines(Paths.get(inputFile));

        Files.createDirectories(Paths.get(outputPath));

        for(String stock : allSymbols) {
            Path file = Paths.get(outputPath, stock+".json");
            if(Files.exists(file))
                continue;

            System.out.println("== Checking "+stock);

            HtmlPage page = client.getPage("http://www.bloomberg.com/quote/"+stock+":US");

            System.out.println(page);

            BloomData data = new BloomData();
            data.symbol = stock;
            data.ticker = getXPathText(page, "//*[@class=\"ticker\"]");
            data.companyName = getXPathText(page, "//*[@class=\"name\"]");
            data.market = getXPathText(page, "//*[@class=\"exchange\"]");
            data.sector = getXPathText(page, "//*[text()=\" Sector \"]/following-sibling::div");
            data.industry = getXPathText(page, "//*[text()=\" Industry \"]/following-sibling::div");
            data.subIndustry = getXPathText(page, "//*[text()=\" Sub-Industry \"]/following-sibling::div");

            mapper.writeValue(file.toFile(), data);

            System.exit(1);

        }

    }
}
