package styx.http.client;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static styx.http.server.Server.route;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;

import styx.http.client.Client;
import styx.http.client.Response;
import styx.http.client.utils.PortFinder;
import styx.http.client.utils.ServerRule;

public class ClientTest {

    private int port = new PortFinder().port();

    @Rule
    public ServerRule serverRule = new ServerRule(
            server -> server.port(port).routes(
                    route().path("/text").to((req, res) -> res.write("RESPONSE_STRING_ÄÖÜ_€")),
                    route().path("/binary").to((req, res) -> res.write(new byte[] { 1, 2, 3, 4 })),
                    route().path("/query").to((req, res) -> res.write("looking for " + req.param("filter").orElse("") + "."))));

    @Test
    public void getText_found_success() {
        try(Client testee = new Client()) {
            Response response = testee.request().
                    host("localhost").port(port).path("/text").get().
                    toCompletableFuture().join();
            assertNotNull(response);
            assertEquals(200, response.status());
            assertEquals("RESPONSE_STRING_ÄÖÜ_€", response.content(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void getBinary_found_success() {
        try(Client testee = new Client()) {
            Response response = testee.request().
                    host("localhost").port(port).path("/binary").get().
                    toCompletableFuture().join();
            assertNotNull(response);
            assertEquals(200, response.status());
            assertArrayEquals(new byte[] { 1, 2, 3, 4 }, response.content());
        }
    }

    @Test
    public void get_notExisting_notFound() throws IOException {
        try(Client testee = new Client()) {
            Response response = testee.request().
                    host("localhost").port(port).path("/notExisting").get().
                    toCompletableFuture().join();
            assertEquals(404, response.status());
        }
    }

    @Test
    public void getWithParams_found_success() throws IOException {
        try(Client testee = new Client()) {
            Response response = testee.request().
                    host("localhost").port(port).path("/query").param("filter", "foo").get().
                    toCompletableFuture().join();
            assertEquals(200, response.status());
            assertEquals("looking for foo.", response.content(StandardCharsets.UTF_8));
        }
    }
}
