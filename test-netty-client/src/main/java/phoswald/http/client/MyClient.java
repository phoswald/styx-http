package phoswald.http.client;

import java.util.concurrent.TimeUnit;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class MyClient implements AutoCloseable {

    private final EventLoopGroup group;

    public MyClient() {
        group = new NioEventLoopGroup();
        System.out.println("[CLIENT NIO GROUP: START]");
    }

    @Override
    public void close() {
        group.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).
                syncUninterruptibly();
        System.out.println("[CLIENT NIO GROUP: STOP]");
    }

    public MyRequest request() {
        return new MyRequest(group);
    }
}
