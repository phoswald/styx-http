package phoswald.http.server;

import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.net.ssl.SSLException;

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

public class MyServer implements AutoCloseable {

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private boolean secure = false;
    private int port = 80;
    private BiConsumer<MyRequest, MyResponse> handler;

    public MyServer() {

    }

    public MyServer secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public MyServer port(int port) {
        this.port = port;
        return this;
    }

    public MyServer handler(BiConsumer<MyRequest, MyResponse> handler) {
        this.handler = handler;
        return this;
    }

    public void start() throws InterruptedException, SSLException, CertificateException {
        final SslContext sslCtx;
        if (secure) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new MyChannelInitializer(Optional.ofNullable(sslCtx), handler));

        Channel ch = b.bind(port).sync().channel();

        ch.closeFuture().sync();
    }

    @Override
    public void close() throws Exception {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
