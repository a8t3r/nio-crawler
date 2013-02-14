package niocrawler.page;

import niocrawler.parser.HttpParser;
import niocrawler.parser.HttpParserData;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Page {

    private static final Logger logger = LoggerFactory.getLogger(Page.class);

    private URI uri;
    private byte[] data;
    private int statusCode;
    private Map<String, String> headers;
    private String body;
    private int level;

    public Page(PageURI pageURI, byte[] data) {
        this.uri = pageURI.getUri();
        this.level = pageURI.getLevel();
        this.data = data;
    }

    public void parseData() {
        HttpParser parser = new HttpParser();
        HttpParserData parserData = parser.parse(data);

        this.body = parserData.getBody();
        this.headers = parserData.getHttpFields();
        this.statusCode = parserData.getStatusCode();
    }

    public URI getUri() {
        return uri;
    }

    public String getBody() {
        return body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public int getLevel() {
        return level;
    }

    public List<URI> getLinks() {
        Document doc = Jsoup.parse(body);
        Elements elems = doc.select("a[href]");

        List<URI> links = new ArrayList<URI>();
        for (Element elem : elems) {
            try {
                String href = elem.attr("href");

                // see
                // http://stackoverflow.com/questions/724043/http-url-address-encoding-in-java
                URI link = new URI(href.trim().replaceAll("\\s", "%20"));
                link = new URI(link.toASCIIString());

                link = uri.resolve(link);

                if (StringUtils.isNotBlank(link.getHost())) {
                    links.add(link);
                }
                // else: this is probably not a http link, skip it
            } catch (URISyntaxException e) {
                logger.debug(e.getMessage());
            }
        }
        return links;
    }
}
