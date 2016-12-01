package phoswald.http.client;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class MyClient implements AutoCloseable {

    private final EventLoopGroup eventLoopGroup;
    private final SslContext sslContext;

    public MyClient() {
        this.eventLoopGroup = new NioEventLoopGroup();
        this.sslContext = createSslContext();
        System.out.println("[CLIENT NIO GROUP: START]");
    }

    @Override
    public void close() {
        eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).
                syncUninterruptibly();
        System.out.println("[CLIENT NIO GROUP: STOP]");
    }

    public MyRequest request() {
        return new MyRequest(eventLoopGroup, sslContext);
    }

    private static SslContext createSslContext() {
        try {
            return SslContextBuilder.forClient().
                    trustManager(InsecureTrustManagerFactory.INSTANCE).
                    build();
        } catch (SSLException e) {
            throw new MyException("Failed to create SSL context.", e);
        }
    }
}
