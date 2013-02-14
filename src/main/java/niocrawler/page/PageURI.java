package niocrawler.page;

import java.net.URI;

/**
 * @author Alexandr Kolosov
 * @since 2/14/13
 */
public class PageURI {

    private int level;
    private URI uri;

    public PageURI(URI uri, int level) {
        this.uri = uri;
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public URI getUri() {
        return uri;
    }
}