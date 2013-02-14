package niocrawler.job;

import niocrawler.Job;
import niocrawler.page.Page;
import niocrawler.utils.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Alexandr Kolosov
 * @since 2/14/13
 */
public class SimpleJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(SimpleJob.class);

    private final URI startPage;

    public SimpleJob(String startPage) {
        try {
            this.startPage = new URI(startPage);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Incorrect start page", e);
        }
    }

    @Override
    public boolean visit(URI url) {
        return UrlUtils.sameHost(startPage, url);
    }

    @Override
    public void process(Page page) {
        logger.debug(page.getBody());
    }

    @Override
    public URI startPage() {
        return startPage;
    }
}
