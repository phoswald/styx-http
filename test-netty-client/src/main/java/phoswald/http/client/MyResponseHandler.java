package phoswald.http.client;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;

class MyResponseHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final CompletableFuture<MyResponse> future;

    private String status;
    private String version;
    private boolean chunked;
    private final List<MyHeader> headers = new ArrayList<>();
    private final List<MyCookie> cookies = new ArrayList<>();
    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    MyResponseHandler(CompletableFuture<MyResponse> future) {
        this.future = future;
    }

    @Override
    public void channelRead0(ChannelHandlerContext context, HttpObject message) {
        if (message instanceof HttpResponse) {
            HttpResponse message2 = (HttpResponse) message;

            status = message2.status().toString();
            version = message2.protocolVersion().toString();
            chunked = HttpUtil.isTransferEncodingChunked(message2);

            for (Map.Entry<String, String> header : message2.headers()) {
                headers.add(new MyHeader(header.getKey(), header.getValue()));
            }

            for(String header : message2.headers().getAll(HttpHeaderNames.SET_COOKIE)) {
                Cookie cookie = ClientCookieDecoder.STRICT.decode(header);
                cookies.add(new MyCookie(cookie.name(), cookie.value()));
            }
        }
        if (message instanceof HttpContent) {
            HttpContent message2 = (HttpContent) message;

            if(message2.content().isReadable()) {
                byte[] data = new byte[message2.content().readableBytes()];
                message2.content().readBytes(data);
                content.write(data, 0, data.length);
            }

            if (message2 instanceof LastHttpContent) {
                future.complete(new MyResponse(status, version, chunked, headers, cookies, content));
                context.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        future.completeExceptionally(cause);
        context.close();
    }
}
