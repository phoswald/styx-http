package phoswald.http.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

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
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.ssl.SslContext;
import phoswald.http.Cookie;
import phoswald.http.Header;
import phoswald.http.QueryParam;

public class Request {

    private final EventLoopGroup eventLoopGroup;
    private final SslContext sslContext;

    private boolean secure = false;
    private String host = "localhost";
    private int port = 80;
    private String path = "/";
    private final List<QueryParam> params = new ArrayList<>();
    private final List<Header> headers = new ArrayList<>();
    private final List<Cookie> cookies = new ArrayList<>();

    Request(EventLoopGroup group, SslContext sslContext) {
        this.sslContext = sslContext;
        this.eventLoopGroup = group;
    }

    public Request secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public Request host(String host) {
        this.host = host;
        return this;
    }

    public Request port(int port) {
        this.port = port;
        return this;
    }

    public Request path(String path) {
        this.path = path;
        return this;
    }

    public Request param(String name, String value) {
        this.params.add(new QueryParam(name, value));
        return this;
    }

    public Request header(String name, String value) {
        this.headers.add(new Header(name, value));
        return this;
    }

    public Request cookie(Cookie cookie) {
        this.cookies.add(cookie);
        return this;
    }

    public Request cookie(List<Cookie> cookies) {
        this.cookies.addAll(cookies);
        return this;
    }

    public CompletionStage<Response> get() {
        return send(HttpMethod.GET);
    }

    private CompletionStage<Response> send(HttpMethod method) {
        // Build the URI by concatenating path and params
        QueryStringEncoder queryStringEncoder = new QueryStringEncoder(path);
        for(QueryParam param : params) {
            queryStringEncoder.addParam(param.name(), param.value());
        }

        // Prepare the HTTP request.
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, queryStringEncoder.toString());

        // Set user-defined headers first
        for(Header header : headers) {
            request.headers().add(header.name(), header.value());
        }

        // Set hardcoded headers, might replace user-defined ones
        request.headers().set(HttpHeaderNames.HOST, host);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE); // TODO use keep-alive?
        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

        // Set cookies, might replace user-defined header
        if(!cookies.isEmpty()) {
            List<DefaultCookie> cookieList = cookies.stream().
                map(c -> new DefaultCookie(c.name(), c.value())).
                collect(Collectors.toList());
            request.headers().set(
                    HttpHeaderNames.COOKIE,
                    ClientCookieEncoder.STRICT.encode(cookieList.toArray(new DefaultCookie[0])));
        }

        CompletableFuture<Response> responseFuture = new CompletableFuture<Response>();

        Bootstrap bootstrap = new Bootstrap().
            group(eventLoopGroup).
            channel(NioSocketChannel.class).
            handler(new ClientChannelInitializer(
                    secure ? Optional.of(sslContext) : Optional.empty(),
                    new ClientChannelHandler(responseFuture)));

        // Make the connection attempt.
        //Channel channel = bootstrap.connect(host, port).sync().channel();
        bootstrap.connect(host, port).addListener((ChannelFuture channelFuture) -> {
            if(channelFuture.isSuccess()) {
                channelFuture.channel().writeAndFlush(request);
            } else {
                responseFuture.completeExceptionally(channelFuture.cause());
            }
        });
        //ch.closeFuture().sync(); // TODO: housekeeping of connections?

        return responseFuture;
    }
}
