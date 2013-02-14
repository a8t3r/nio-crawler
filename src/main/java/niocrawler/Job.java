package niocrawler;

import niocrawler.page.Page;

import java.net.URI;

public interface Job {
    boolean visit(URI url);

    void process(Page page);
}
