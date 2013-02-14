package niocrawler.page;

import niocrawler.HttpFetcher;
import niocrawler.Job;
import niocrawler.LinksStorage;
import niocrawler.fetcher.HttpFetcherFactory;
import niocrawler.storage.LinksStorageMemImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.*;

public class PageHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PageHandler.class);

    private BlockingQueue<Page> pagesQueue;
    private BlockingQueue<URI> linksQueue;
    private LinksStorage linksStorage;
    private Job job;

    public PageHandler(Job job) {
        this.linksQueue = new LinkedBlockingQueue<URI>();
        this.linksQueue.add(job.startPage());
        this.pagesQueue = new LinkedBlockingQueue<Page>();
        this.linksStorage = new LinksStorageMemImpl();
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

    public void start() {
        start(HttpFetcherFactory.newFetcherFor(getPagesQueue()));
    }

    public void start(HttpFetcher fetcher) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this);
        fetcher.fetch(getLinksQueue());
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
                }
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

    public BlockingQueue<Page> getPagesQueue() {
        return pagesQueue;
    }

    public BlockingQueue<URI> getLinksQueue() {
        return linksQueue;
    }
}
