package phoswald.http.client;

import java.util.concurrent.TimeUnit;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class MyClient implements AutoCloseable {

    private final EventLoopGroup group;

    public MyClient() {
        group = new NioEventLoopGroup();
        System.out.println("NIO GROUP: START");
    }

    @Override
    public void close() {
        group.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).
                syncUninterruptibly();
        System.out.println("NIO GROUP: STOP");
    }

    EventLoopGroup getGroup() {
        return group;
    }
}
