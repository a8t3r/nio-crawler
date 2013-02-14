package niocrawler.fetcher;

import niocrawler.page.Page;
import niocrawler.utils.HttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class HttpFetcherNIOImpl implements HttpFetcher {

    private static final Logger logger = LoggerFactory.getLogger(HttpFetcherNIOImpl.class);
    public static final int CONNECTION_LIMIT = 10;

    private Selector selector;
    private ByteBuffer readBuffer;
    private Map<URI, ByteBuffer> writeBuffers;
    private Map<URI, ByteArrayOutputStream> streams;
    private BlockingQueue<Page> pagesQueue;
    private HttpRequestBuilder httpRequestBuilder;

    public HttpFetcherNIOImpl(BlockingQueue<Page> pagesQueue) throws IOException {
        this.readBuffer = ByteBuffer.allocate(8192);
        this.writeBuffers = new HashMap<URI, ByteBuffer>();
        this.streams = new HashMap<URI, ByteArrayOutputStream>();
        this.httpRequestBuilder = new HttpRequestBuilder();
        this.pagesQueue = pagesQueue;
        this.selector = SelectorProvider.provider().openSelector();
    }

    @Override
    public void fetch(BlockingQueue<URI> linksQueue) {
        while (!Thread.interrupted()) {
            try {
                initiateNewConnections(linksQueue);

                int nb = selector.select(1000);
                if (nb == 0 && linksQueue.isEmpty()) {
                    break;
                }

                // Iterate over the set of keys for which events are available
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isConnectable()) {
                        connect(key);
                    } else if (key.isWritable()) {
                        write(key);
                    } else if (key.isReadable()) {
                        read(key);
                    }
                }
            } catch (Exception e) {
                logger.error("Unexpected exception", e);
                break;
            }
        }
    }

    private void initiateNewConnections(BlockingQueue<URI> linksQueue) throws IOException {
        // Initiate a maximum of N new connections
        int i = CONNECTION_LIMIT;
        while (i -- > 0) {
            // Fetch without blocking
            URI url = linksQueue.poll();

            // We will retry at next iteration
            if (url == null) {
                break;
            }

            // Create a non-blocking socket channel
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);

            // Kick off connection establishment
            int port = url.getPort() > 0 ? url.getPort() : 80;
            socketChannel.connect(new InetSocketAddress(url.getHost(), port));

            SelectionKey key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
            key.attach(url);

            streams.put(url, new ByteArrayOutputStream());
        }
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.finishConnect();

        // We now want to write
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void write(SelectionKey key) throws IOException {
        URI url = (URI) key.attachment();
        SocketChannel socketChannel = (SocketChannel) key.channel();

        ByteBuffer writeBuffer = writeBuffers.get(url);
        if (writeBuffer == null) {
            String getRequest = httpRequestBuilder.buildGet(url);
            writeBuffer = ByteBuffer.wrap(getRequest.getBytes());
            writeBuffers.put(url, writeBuffer);
        }

        socketChannel.write(writeBuffer);
        if (!writeBuffer.hasRemaining()) {
            // Remove write buffer
            writeBuffers.remove(url);

            // We now want to read
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void read(SelectionKey key) throws IOException {
        URI url = (URI) key.attachment();
        SocketChannel socketChannel = (SocketChannel) key.channel();

        readBuffer.clear();
        int numRead = socketChannel.read(readBuffer);
        if (numRead > 0) {
            streams.get(url).write(readBuffer.array(), 0, numRead);
        } else {
            // Reading is complete
            key.channel().close();
            key.cancel();

            // Add to page queue
            ByteArrayOutputStream stream = streams.remove(url);
            Page page = new Page(url, stream.toByteArray());
            pagesQueue.add(page);
        }
    }
}
