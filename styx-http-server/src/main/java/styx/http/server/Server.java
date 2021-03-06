package styx.http.server;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
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
import styx.http.HttpException;
import styx.http.SessionVariable;

public class Server implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private Channel channel;

    private boolean secure = false;
    private String host = "localhost";
    private int port = 80;
    private Path certFullChain;
    private Path certPrivateKey;
    private SessionHandler sessionHandler;
    private Function<Request, CompletableFuture<Response>> handler;

    public Server() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        logger.info("Started server NIO groups.");
    }

    public Server secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public Server host(String host) {
        this.host = host;
        return this;
    }

    public Server port(int port) {
        this.port = port;
        return this;
    }

    public Server certificate(Path certFullChain, Path certPrivateKey) {
        this.certFullChain = certFullChain;
        this.certPrivateKey = certPrivateKey;
        return this;
    }

    public Server enableSessions() {
        sessionHandler = new SessionHandler();
        return this;
    }

    public Server routes(Route... routes) {
        this.handler = Route.createHandler(routes, this);
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

    @Deprecated
    public void run() {
        start();
        channel.closeFuture().syncUninterruptibly();
    }

    public void run(CountDownLatch stop) throws InterruptedException {
        start();
        stop.await();
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

    Optional<SessionHandler> getSessionHandler() {
        return Optional.ofNullable(sessionHandler);
    }

    public void login(Response response, Duration expiry, SessionVariable... sessionVariables) {
        response.cookie(SessionHandler.COOKIE_NAME, sessionHandler.encodeAndSignCookie(
                Arrays.asList(sessionVariables), Instant.now().plus(expiry)));
    }

    private String protocol() {
        return secure ? "HTTPS" : "HTTP";
    }

    private Optional<SslContext> createSslContext() {
        try {
            if(secure) {
                if(certFullChain != null && certPrivateKey != null) {
                    logger.info("Using SSL certificate from " + certFullChain + " and " + certPrivateKey);
                    return Optional.of(SslContextBuilder.forServer(
                            certFullChain.toFile(), certPrivateKey.toFile()).build());
                } else {
                    logger.warning("Using self-signed SSL certificate for " + host);
                    SelfSignedCertificate certificate = new SelfSignedCertificate(host);
                    return Optional.of(SslContextBuilder.forServer(
                            certificate.certificate(), certificate.privateKey()).build());
                }
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new HttpException("Failed to create SSL context.", e);
        }
    }
}
