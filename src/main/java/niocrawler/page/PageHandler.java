package niocrawler.page;

import niocrawler.Job;
import niocrawler.LinksStorage;
import niocrawler.storage.LinksStorageMemImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PageHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PageHandler.class);

    private BlockingQueue<Page> pagesQueue;
    private BlockingQueue<PageURI> linksQueue;
    private LinksStorage linksStorage;
    private Job job;

    public PageHandler(Job job) {
        this.linksQueue = new LinkedBlockingQueue<PageURI>();
        this.pagesQueue = new LinkedBlockingQueue<Page>();
        this.linksStorage = new LinksStorageMemImpl();
        this.job = job;

        this.linksQueue.add(new PageURI(job.startPage(), 0));
        this.linksStorage.add(job.startPage());
    }

    /**
     * Add this url to the queue.
     *
     * The url will only be added if it can be visited AND it is not already in
     * the url storage (meaning it has not been previously visited).
     */
    private void add(URI url, int level) {
        if (linksStorage.add(url)) {
            PageURI pageURI = new PageURI(url, level);

            if (job.visit(pageURI)) {
                linksQueue.add(pageURI);
            }
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            Page page = null;

            while (page == null) {
                try {
                    page = pagesQueue.poll(1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    logger.info("Interrupted");
                    return;
                }

                if (page == null) {
                    logger.debug("Empty queue");
                    return;
                }
            }

            logger.debug("Handling " + page.getUri());
            page.parseData();

            process(page);
        }
    }

    private void process(Page page) {
        int statusCode = page.getStatusCode();
        if (statusCode == 200) {
            // Let the user's job process this page
            job.process(page);

            String contentType = page.getHeader("Content-Type");
            if (contentType.startsWith("text/html")) {
                List<URI> links = page.getLinks();
                for (URI link : links) {
                    add(link, page.getLevel() + 1);
                }
            }
        } else if (statusCode == 301 || statusCode == 302) {
            // TODO handle case when location is relative

            String header = page.getHeader("Location");
            try {
                URI location = new URI(header);
                add(location, page.getLevel());
            } catch (URISyntaxException e) {
                logger.debug("Skip {} because of incorrect form", header);
                return;
            }
        } else {
            // Log that page was not fetched successfully
            logger.debug("Skip [{}] because of status code {}", page.getUri(), statusCode);
        }
    }

    public BlockingQueue<Page> getPagesQueue() {
        return pagesQueue;
    }

    public BlockingQueue<PageURI> getLinksQueue() {
        return linksQueue;
    }
}
