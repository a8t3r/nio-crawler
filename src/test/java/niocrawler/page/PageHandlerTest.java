package niocrawler.page;

import niocrawler.job.SimpleJob;
import org.junit.Test;

/**
 * @author Alexandr Kolosov
 * @since 2/14/13
 */
public class PageHandlerTest {
    @Test
    public void testStart() throws Exception {
        PageHandler handler = new PageHandler(new SimpleJob("http://ru.wikipedia.org/wiki/Main_Page"));
        handler.start();
    }
}
