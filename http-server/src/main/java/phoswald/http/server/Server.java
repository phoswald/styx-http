package phoswald.http.server;

import java.util.Optional;
import java.util.function.BiConsumer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import phoswald.http.HttpException;

public class Server implements AutoCloseable {

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private boolean secure = false;
    private int port = 80;
    private BiConsumer<Request, Response> handler;

    public Server() { }

    public Server secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public Server port(int port) {
        this.port = port;
        return this;
    }

    public Server routes(Route... routes) {
        this.handler = Route.combine(routes);
        return this;
    }

    public Server handler(BiConsumer<Request, Response> handler) {
        this.handler = handler;
        return this;
    }

    public void run() {
        Optional<SslContext> sslContext = createSslContext();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ServerChannelInitializer(sslContext, handler));

        try {
            Channel channel = bootstrap.bind(port).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            throw new HttpException(null, e);
        }
    }

    @Override
    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public static Route.Builder route() {
        return new Route.Builder();
    }

    private Optional<SslContext> createSslContext() {
        try {
            if(secure) {
                SelfSignedCertificate certificate = new SelfSignedCertificate();
                return Optional.of(SslContextBuilder.forServer(
                        certificate.certificate(), certificate.privateKey()).build());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new HttpException("Failed to create SSL context.", e);
        }
    }
}
