package styx.http.server;

import java.util.function.BiConsumer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;

class ServerChannelHandler extends SimpleChannelInboundHandler<Object> {

    private final BiConsumer<Request, Response> handler;
    private RequestBuilder builder = new RequestBuilder();

    ServerChannelHandler(BiConsumer<Request, Response> handler) {
        this.handler = handler;
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
                Request request = builder.build();
                Response response = new Response();
                handler.accept(request, response);
                response.send(context, builder.isValid(), builder.keepAlive());
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
