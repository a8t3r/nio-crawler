package niocrawler.fetcher;

import niocrawler.HttpFetcher;
import niocrawler.page.Page;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * @author Alexandr Kolosov
 * @since 2/14/13
 */
public class HttpFetcherFactory {

    public static HttpFetcher newFetcherFor(BlockingQueue<Page> pages) {
        try {
            HttpFetcherNIOImpl fetcher = new HttpFetcherNIOImpl();
            fetcher.init(pages);
            return fetcher;
        } catch (IOException e) {
            throw new IllegalStateException("Can't open selector", e);
        }
    }
}
