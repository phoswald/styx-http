package phoswald.http.server;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import phoswald.http.Cookie;
import phoswald.http.Header;
import phoswald.http.QueryParam;

class RequestHandler extends SimpleChannelInboundHandler<Object> {

    private final BiConsumer<Request, Response> handler;

    private String protocol;
    private String host;
    private String path;
    private boolean keepAlive;
    private final List<QueryParam> params = new ArrayList<>();
    private final List<Header> headers = new ArrayList<>();
    private final List<Cookie> cookies = new ArrayList<>();
    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    RequestHandler(BiConsumer<Request, Response> handler) {
        this.handler = handler;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context) {
        context.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Object message) {
        if (message instanceof HttpRequest) {
            HttpRequest message2 = (HttpRequest) message;
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(message2.uri());

            if (HttpUtil.is100ContinueExpected(message2)) {
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
                context.write(response);
            }

            protocol = message2.protocolVersion().toString();
            host = message2.headers().get(HttpHeaderNames.HOST, "unknown");
            path = queryStringDecoder.path();
            keepAlive = HttpUtil.isKeepAlive(message2);

            Map<String, List<String>> paramMap = queryStringDecoder.parameters();
            for (Entry<String, List<String>> paramEntry : paramMap.entrySet()) {
                for (String value : paramEntry.getValue()) {
                    params.add(new QueryParam(paramEntry.getKey(), value));
                }
            }

            for (Map.Entry<String, String> header : message2.headers()) {
                headers.add(new Header(header.getKey(), header.getValue()));
            }

            String cookieString = message2.headers().get(HttpHeaderNames.COOKIE);
            if (cookieString != null) {
                for (io.netty.handler.codec.http.cookie.Cookie cookie: ServerCookieDecoder.STRICT.decode(cookieString)) {
                    cookies.add(new Cookie(cookie.name(), cookie.value()));
                }
            }
        }

        if (message instanceof HttpContent) {
            HttpContent message2 = (HttpContent) message;

            if (message2.content().isReadable()) {
                byte[] data = new byte[message2.content().readableBytes()];
                message2.content().readBytes(data);
                content.write(data, 0, data.length);
            }

            if (message instanceof LastHttpContent) {
                Request request = new Request(protocol, host, path, params, headers, cookies, content);
                Response response = new Response();
                handler.accept(request, response);
                response.send(context, keepAlive, message2.decoderResult().isSuccess());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
        context.close();
    }
}
