package phoswald.http.client;

import java.util.concurrent.CompletableFuture;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

class ClientChannelHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final CompletableFuture<Response> future;
    private ResponseBuilder builder = new ResponseBuilder();

    ClientChannelHandler(CompletableFuture<Response> future) {
        this.future = future;
    }

    @Override
    public void channelRead0(ChannelHandlerContext context, HttpObject message) {
        if (message instanceof HttpResponse) {
            builder.handle((HttpResponse) message);
        }

        if (message instanceof HttpContent) {
            builder.handle((HttpContent) message);

            if (message instanceof LastHttpContent) {
                Response response = builder.build();
                future.complete(response);
                context.close();
                builder = new ResponseBuilder();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        future.completeExceptionally(cause);
        context.close();
    }
}
