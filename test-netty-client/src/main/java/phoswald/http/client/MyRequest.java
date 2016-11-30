package phoswald.http.client;

import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
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
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class MyRequest {

    private final MyClient client;
    private String host = "localhost";
    private int port = 80;
    private String path = "/";

    private MyRequest(MyClient client) {
        this.client = client;
    }

    public static MyRequest build(MyClient client /* TODO remove this argument */) {
        return new MyRequest(client);
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

    public CompletableFuture<MyResponse> get(boolean useSsl) throws InterruptedException, SSLException {
        MyResponseHandler handler = new MyResponseHandler();
        Bootstrap bootstrap = new Bootstrap().
            group(client.getGroup()).
            channel(NioSocketChannel.class).
            handler(new MyClientInitializer(handler, createSslContext(useSsl)));

        // Make the connection attempt.
        Channel ch = bootstrap.connect(host, port).sync().channel();

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

        ch.writeAndFlush(request);

        //ch.closeFuture().sync();

        return handler.future();
    }

    private static SslContext createSslContext(boolean useSsl) throws SSLException {
        if(useSsl) {
            return SslContextBuilder.forClient().
                    trustManager(InsecureTrustManagerFactory.INSTANCE).
                    build();
        } else {
            return null;
        }
    }
}
