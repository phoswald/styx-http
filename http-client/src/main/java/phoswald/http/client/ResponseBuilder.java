package phoswald.http.client;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import phoswald.http.Cookie;
import phoswald.http.Header;

class ResponseBuilder {

    private String version;
    private int status;
    private boolean chunked;
    private final List<Header> headers = new ArrayList<>();
    private final List<Cookie> cookies = new ArrayList<>();
    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    void handle(HttpResponse message) {
        version = message.protocolVersion().toString();
        status = message.status().code();
        chunked = HttpUtil.isTransferEncodingChunked(message);

        for (Map.Entry<String, String> header : message.headers()) {
            headers.add(new Header(header.getKey(), header.getValue()));
        }

        for(String header : message.headers().getAll(HttpHeaderNames.SET_COOKIE)) {
            io.netty.handler.codec.http.cookie.Cookie cookie = ClientCookieDecoder.STRICT.decode(header);
            cookies.add(new Cookie(cookie.name(), cookie.value()));
        }
    }

    void handle(HttpContent message) {
        if(message.content().isReadable()) {
            byte[] data = new byte[message.content().readableBytes()];
            message.content().readBytes(data);
            content.write(data, 0, data.length);
        }
    }

    Response build() {
        return new Response(version, status, chunked, headers, cookies, content);
    }
}
