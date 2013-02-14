package niocrawler.fetcher;

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
            return new HttpFetcherNIOImpl(pages);
        } catch (IOException e) {
            throw new IllegalStateException("Can't open selector", e);
        }
    }
}
