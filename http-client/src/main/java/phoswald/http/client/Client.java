package phoswald.http.client;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import phoswald.http.HttpException;

public class Client implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private final EventLoopGroup eventLoopGroup;
    private final SslContext sslContext;

    public Client() {
        this.eventLoopGroup = new NioEventLoopGroup();
        this.sslContext = createSslContext();
        logger.info("Started client NIO group.");
    }

    @Override
    public void close() {
        logger.info("Stopping client NIO group."); // TODO: shutdown semantics and performance?
        eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).syncUninterruptibly();
    }

    public Request request() {
        return new Request(eventLoopGroup, sslContext);
    }

    private static SslContext createSslContext() {
        try {
            return SslContextBuilder.forClient().
                    trustManager(InsecureTrustManagerFactory.INSTANCE).
                    build();
        } catch (Exception e) {
            throw new HttpException("Failed to create SSL context.", e);
        }
    }
}
