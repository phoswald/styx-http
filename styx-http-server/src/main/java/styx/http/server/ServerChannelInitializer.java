package styx.http.server;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;

class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final Optional<SslContext> sslContext;
    private final Function<Request, CompletableFuture<Response>> handler;

    ServerChannelInitializer(Optional<SslContext> sslContext, Function<Request, CompletableFuture<Response>> handler) {
        this.sslContext = Objects.requireNonNull(sslContext);
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        if (sslContext.isPresent()) {
            pipeline.addLast(sslContext.get().newHandler(channel.alloc()));
        }
        pipeline.addLast(new HttpRequestDecoder());
        //p.addLast(new HttpObjectAggregator(1048576));
        pipeline.addLast(new HttpResponseEncoder());
        //p.addLast(new HttpContentCompressor());
        pipeline.addLast(new ServerChannelHandler(handler));
    }
}
