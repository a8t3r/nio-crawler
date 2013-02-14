package niocrawler;

import niocrawler.page.Page;
import niocrawler.page.PageURI;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public interface HttpFetcher {

    void init(BlockingQueue<Page> pagesQueue) throws IOException;

    void fetch(BlockingQueue<PageURI> linksQueue);
}
