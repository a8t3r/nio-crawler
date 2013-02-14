package niocrawler.utils;

import java.net.URI;

/**
 * @author Alexandr Kolosov
 * @since 2/14/13
 */
public class UrlUtils {

    public static boolean sameHost(URI first, URI second) {
        if (first == null || second == null) {
            return false;
        }

        return first.getHost().equals(second.getHost());
    }
}
