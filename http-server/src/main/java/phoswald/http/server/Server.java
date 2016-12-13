package phoswald.http.server;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

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

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private Channel channel;

    private boolean secure = false;
    private int port = 80;
    private BiConsumer<Request, Response> handler;

    public Server() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        logger.info("Started server NIO groups.");
    }

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

    public void start() {
        Optional<SslContext> sslContext = createSslContext();
        ServerBootstrap bootstrap = new ServerBootstrap().
                group(bossGroup, workerGroup).
                channel(NioServerSocketChannel.class).
                handler(new LoggingHandler(LogLevel.INFO)).
                childHandler(new ServerChannelInitializer(sslContext, handler));

        channel = bootstrap.bind(port).syncUninterruptibly().channel();
        logger.info("Started " + protocol() + " server on port " + port);
    }

    public void run() {
        start();
        channel.closeFuture().syncUninterruptibly();
    }

    @Override
    public void close() {
        if(channel != null) {
            logger.info("Stopping " + protocol() + " server on port " + port);
            channel.close().syncUninterruptibly();
        }
        logger.info("Stopping server NIO groups."); // TODO: shutdown semantics and performance?
        bossGroup.shutdownGracefully()/*.syncUninterruptibly()*/;
        workerGroup.shutdownGracefully()/*.syncUninterruptibly()*/;
    }

    public static Route.Builder route() {
        return new Route.Builder();
    }

    private String protocol() {
        return secure ? "HTTPS" : "HTTP";
    }

    private Optional<SslContext> createSslContext() {
        try {
            if(secure) {
                SelfSignedCertificate certificate = new SelfSignedCertificate("localhost");
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
