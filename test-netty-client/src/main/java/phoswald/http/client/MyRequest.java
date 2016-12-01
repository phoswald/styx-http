package phoswald.http.client;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.ssl.SslContext;

public class MyRequest {

    private final EventLoopGroup eventLoopGroup;
    private final SslContext sslContext;
    private boolean secure = false;
    private String host = "localhost";
    private int port = 80;
    private String path = "/";

    MyRequest(EventLoopGroup group, SslContext sslContext) {
        this.sslContext = sslContext;
        this.eventLoopGroup = group;
    }

    public MyRequest secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public MyRequest host(String host) {
        this.host = host;
        return this;
    }

    public MyRequest port(int port) {
        this.port = port;
        return this;
    }

    public MyRequest path(String path) {
        this.path = path;
        return this;
    }

    public CompletableFuture<MyResponse> get() {
        // Prepare the HTTP request.
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
        request.headers().set(HttpHeaderNames.HOST, host);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

        // Set some example cookies.
        request.headers().set(
                HttpHeaderNames.COOKIE,
                ClientCookieEncoder.STRICT.encode(
                        new DefaultCookie("my-cookie", "foo"),
                        new DefaultCookie("another-cookie", "bar")));

        CompletableFuture<MyResponse> responseFuture = new CompletableFuture<MyResponse>();

        Bootstrap bootstrap = new Bootstrap().
            group(eventLoopGroup).
            channel(NioSocketChannel.class).
            handler(new MyChannelInitializer(
                    secure ? Optional.of(sslContext) : Optional.empty(),
                    new MyResponseHandler(responseFuture)));

        // Make the connection attempt.
        // Channel channel = bootstrap.connect(host, port).sync().channel();
        bootstrap.connect(host, port).addListener((ChannelFuture channelFuture) -> {
            if(channelFuture.isSuccess()) {
                channelFuture.channel().writeAndFlush(request);
            } else {
                responseFuture.completeExceptionally(channelFuture.cause());
            }
        });
        //ch.closeFuture().sync();

        return responseFuture;
    }
}
