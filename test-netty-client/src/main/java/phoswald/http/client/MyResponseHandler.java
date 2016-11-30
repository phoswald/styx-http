package phoswald.http.client;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;

class MyResponseHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final CompletableFuture<MyResponse> future = new CompletableFuture<>();

    private String status;
    private String version;
    private boolean chunked;
    private final List<MyResponse.Header> headers = new ArrayList<>();
    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    @Override
    public void channelRead0(ChannelHandlerContext context, HttpObject message) {
        if (message instanceof HttpResponse) {
            HttpResponse message2 = (HttpResponse) message;

            status = message2.status().toString();
            version = message2.protocolVersion().toString();
            chunked = HttpUtil.isTransferEncodingChunked(message2);

            if (!message2.headers().isEmpty()) {
                for (CharSequence name: message2.headers().names()) {
                    for (CharSequence value: message2.headers().getAll(name)) {
                        headers.add(new MyResponse.Header(name.toString(), value.toString()));
                    }
                }
            }
        }
        if (message instanceof HttpContent) {
            HttpContent message2 = (HttpContent) message;

            byte[] data = new byte[message2.content().readableBytes()];
            message2.content().readBytes(data);
            content.write(data, 0, data.length);

            if (message2 instanceof LastHttpContent) {
                future.complete(new MyResponse(status, version, chunked, headers, content));
                context.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        future.completeExceptionally(cause);
        ctx.close();
    }

    CompletableFuture<MyResponse> future() {
        return future;
    }
}
