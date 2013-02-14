package niocrawler;

import niocrawler.page.Page;
import niocrawler.page.PageURI;

import java.net.URI;

public interface Job {
    boolean visit(PageURI url);

    void process(Page page);

    URI startPage();
}
