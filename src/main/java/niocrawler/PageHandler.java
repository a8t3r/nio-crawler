package niocrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class PageHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PageHandler.class);

    private BlockingQueue<Page> pagesQueue;
    private BlockingQueue<URI> linksQueue;
    private LinksStorage linksStorage;
    private Job job;

    public PageHandler(BlockingQueue<Page> pagesQueue, BlockingQueue<URI> linksQueue, Job job, LinksStorage linksStorage) {
        this.pagesQueue = pagesQueue;
        this.linksQueue = linksQueue;
        this.linksStorage = linksStorage;
        this.job = job;
    }

    /**
     * Add this url to the queue.
     * <p/>
     * The url will only be added if it can be visited AND it is not already in
     * the url storage (meaning it has not been previously visited).
     *
     * @param url
     */
    private void add(URI url) {
        if (job.visit(url) && linksStorage.add(url)) {
            linksQueue.add(url);
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            Page page;
            try {
                page = pagesQueue.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.info("Interrupted");
                return;
            }

            logger.debug("Handling " + page.getUri());
            page.process();

            int statusCode = page.getStatusCode();
            if (statusCode == 200) {
                // Let the user's job process this page
                job.process(page);

                String contentType = page.getHeader("Content-Type");
                if (contentType.startsWith("text/html")) {
                    List<URI> links = page.getLinks();
                    for (URI link : links) {
                        add(link);
                    }
                }
            } else if (statusCode == 301 || statusCode == 302) {
                // TODO handle case when location is relative

                String header = page.getHeader("Location");
                try {
                    URI location = new URI(header);
                    add(location);
                } catch (URISyntaxException e) {
                    logger.debug("Skip {} because of incorrect form", header);
                    continue;
                }
            } else {
                // Log that page was not fetched successfully
                logger.debug("Skip [{}] because of status code {}", page.getUri(), statusCode);
            }
        }
    }
}
