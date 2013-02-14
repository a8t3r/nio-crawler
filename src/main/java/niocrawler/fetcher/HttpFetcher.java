package niocrawler.fetcher;

import java.net.URI;
import java.util.concurrent.BlockingQueue;

public interface HttpFetcher {
    void fetch(BlockingQueue<URI> linksQueue);
}
