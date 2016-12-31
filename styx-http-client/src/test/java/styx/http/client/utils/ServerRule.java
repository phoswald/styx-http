package styx.http.client.utils;

import java.util.function.Consumer;

import org.junit.rules.ExternalResource;

import styx.http.server.Server;

public class ServerRule extends ExternalResource {

    private final Consumer<Server> configuration;
    private Server server;

    public ServerRule(Consumer<Server> configuration) {
        this.configuration = configuration;
    }

    @Override
    public void before() {
        server = new Server();
        configuration.accept(server);
        server.start();
    }

    @Override
    protected void after() {
        server.close();
    }
}
