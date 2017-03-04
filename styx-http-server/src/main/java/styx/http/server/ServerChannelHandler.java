package styx.http.server;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;

class ServerChannelHandler extends SimpleChannelInboundHandler<Object> {

    private final Function<Request, CompletableFuture<Response>> handler;
    private RequestBuilder builder = new RequestBuilder();

    ServerChannelHandler(Function<Request, CompletableFuture<Response>> handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context) {
        context.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Object message) {
        if (message instanceof HttpRequest) {
            builder.handle(context, (HttpRequest) message);
        }

        if (message instanceof HttpContent) {
            builder.handle(context, (HttpContent) message);

            if (message instanceof LastHttpContent) {
                RequestBuilder capturedBuilder = builder;
                Request request = builder.build();
                handler.apply(request).thenAccept(response -> {
                    response.send(context, capturedBuilder.isValid(), capturedBuilder.keepAlive());
                });
                builder = new RequestBuilder();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
        context.close();
    }
}
