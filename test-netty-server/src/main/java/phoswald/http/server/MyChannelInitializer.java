package phoswald.http.server;

import java.util.Optional;
import java.util.function.Function;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;

public class MyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final Optional<SslContext> sslContext;
    private final Function<MyRequest, MyResponse> handler;

    public MyChannelInitializer(Optional<SslContext> sslContext, Function<MyRequest, MyResponse> handler) {
        this.sslContext = sslContext;
        this.handler = handler;
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
        pipeline.addLast(new MyRequestHandler(handler));
    }
}
