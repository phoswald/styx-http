package phoswald.http.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
import phoswald.http.MyCookie;
import phoswald.http.MyHeader;
import phoswald.http.MyParam;

public class MyRequest {

    private final EventLoopGroup eventLoopGroup;
    private final SslContext sslContext;

    private boolean secure = false;
    private String host = "localhost";
    private int port = 80;
    private String path = "/";
    private final List<MyParam> params = new ArrayList<>();
    private final List<MyHeader> headers = new ArrayList<>();
    private final List<MyCookie> cookies = new ArrayList<>();

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

    public MyRequest param(String name, String value) {
        this.params.add(new MyParam(name, value));
        return this;
    }

    public MyRequest header(String name, String value) {
        this.headers.add(new MyHeader(name, value));
        return this;
    }

    public MyRequest cookie(MyCookie cookie) {
        this.cookies.add(cookie);
        return this;
    }

    public MyRequest cookie(List<MyCookie> cookies) {
        this.cookies.addAll(cookies);
        return this;
    }

    public CompletableFuture<MyResponse> get() {
        return send(HttpMethod.GET);
    }

    private CompletableFuture<MyResponse> send(HttpMethod method) {
        // Build the URI by concatenating path and params
        QueryStringEncoder queryStringEncoder = new QueryStringEncoder(path);
        for(MyParam param : params) {
            queryStringEncoder.addParam(param.name(), param.value());
        }

        // Prepare the HTTP request.
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, queryStringEncoder.toString());

        // Set user-defined headers first
        for(MyHeader header : headers) {
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

        CompletableFuture<MyResponse> responseFuture = new CompletableFuture<MyResponse>();

        Bootstrap bootstrap = new Bootstrap().
            group(eventLoopGroup).
            channel(NioSocketChannel.class).
            handler(new MyChannelInitializer(
                    secure ? Optional.of(sslContext) : Optional.empty(),
                    new MyResponseHandler(responseFuture)));

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
