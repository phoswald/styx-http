package phoswald.http.client;

import java.util.Optional;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.ssl.SslContext;

class MyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final Optional<SslContext> sslContext;
    private final MyResponseHandler responseHandler;

    MyChannelInitializer(Optional<SslContext> sslContext, MyResponseHandler responseHandler) {
        this.sslContext = sslContext;
        this.responseHandler = responseHandler;
    }

    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        if (sslContext.isPresent()) {
            pipeline.addLast(sslContext.get().newHandler(channel.alloc()));
        }
        pipeline.addLast(new HttpClientCodec());
        pipeline.addLast(new HttpContentDecompressor());
        // p.addLast(new HttpObjectAggregator(1048576));
        pipeline.addLast(responseHandler);
    }
}
