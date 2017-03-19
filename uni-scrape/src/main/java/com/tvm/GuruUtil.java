package com.tvm;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by horse on 19/3/17.
 */
public class GuruUtil {

    static private String loginUrl = "http://www.gurufocus.com/forum/login.php?0";
    static private String user = "vinniek";
    static private String passwd = "tvmtvmtvm1";

    static private WebClient getWebClient() {
        WebClient client = new WebClient(BrowserVersion.CHROME); //, "127.0.0.1", 3128);
        client.getOptions().setJavaScriptEnabled(true);
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.getOptions().setCssEnabled(true);
        return client;
    }


    static public WebClient getLoggedInWebClient() {
        try {
            WebClient webClient = getWebClient();
            HtmlPage page = webClient.getPage(loginUrl);
            HtmlForm form = page.getForms().get(0);
            form.getInputByName("username").setValueAttribute(user);
            form.getInputByName("password").setValueAttribute(passwd);
            page = form.getInputByValue("Log In").click();
            return webClient;
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    public static List<String> getSymbols(String fileName) {
        try {
            List<String> rv = new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader(fileName));
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

    /*
    public static void getStyle(WebClient webClient, HtmlElement element) {
        element.getStyleElement()
        WebWindow window = webClient.getCurrentWindow();
        Window jscript = (Window) window.getScriptObject();
        HTMLElement element = (HTMLElement) jscript.makeScriptableFor(div);
        ComputedCSSStyleDeclaration style = jscript.jsxFunction_getComputedStyle(element, null);
        System.out.println(style);

    }
    */
}
