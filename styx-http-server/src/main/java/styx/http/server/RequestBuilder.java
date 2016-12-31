package styx.http.server;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import styx.http.Cookie;
import styx.http.Header;
import styx.http.QueryParam;

class RequestBuilder {

    private boolean isValid;
    private boolean keepAlive;

    private String protocol;
    private String host;
    private String path;
    private final List<QueryParam> params = new ArrayList<>();
    private final List<Header> headers = new ArrayList<>();
    private final List<Cookie> cookies = new ArrayList<>();
    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    boolean isValid() {
        return isValid;
    }

    boolean keepAlive() {
        return keepAlive;
    }

    void handle(ChannelHandlerContext context, HttpRequest message) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(message.uri());

        if (HttpUtil.is100ContinueExpected(message)) {
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
            context.write(response);
        }

        keepAlive = HttpUtil.isKeepAlive(message);
        protocol = message.protocolVersion().toString();
        host = message.headers().get(HttpHeaderNames.HOST, "unknown");
        path = queryStringDecoder.path();

        Map<String, List<String>> paramMap = queryStringDecoder.parameters();
        for (Entry<String, List<String>> paramEntry : paramMap.entrySet()) {
            for (String value : paramEntry.getValue()) {
                params.add(new QueryParam(paramEntry.getKey(), value));
            }
        }

        for (Map.Entry<String, String> header : message.headers()) {
            headers.add(new Header(header.getKey(), header.getValue()));
        }

        String cookieString = message.headers().get(HttpHeaderNames.COOKIE);
        if (cookieString != null) {
            for (io.netty.handler.codec.http.cookie.Cookie cookie: ServerCookieDecoder.STRICT.decode(cookieString)) {
                cookies.add(new Cookie(cookie.name(), cookie.value()));
            }
        }
    }

    void handle(ChannelHandlerContext context, HttpContent message) {
        isValid = message.decoderResult().isSuccess();

        if (message.content().isReadable()) {
            byte[] data = new byte[message.content().readableBytes()];
            message.content().readBytes(data);
            content.write(data, 0, data.length);
        }
    }

    Request build() {
        return new Request(protocol, host, path, params, headers, cookies, content);
    }
}
