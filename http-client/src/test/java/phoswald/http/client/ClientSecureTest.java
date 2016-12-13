package phoswald.http.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static phoswald.http.server.Server.route;

import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;

import phoswald.http.client.utils.PortFinder;
import phoswald.http.client.utils.ServerRule;

public class ClientSecureTest {

    private int port = new PortFinder().port();

    @Rule
    public ServerRule serverRule = new ServerRule(
            server -> server.secure(true).port(port).routes(
                    route().path("/text").to((req, res) -> res.write("RESPONSE_STRING_ÄÖÜ_€"))));

    @Test
    public void getText_found_success() {
        try(Client testee = new Client()) {
            Response response = testee.request().
                    secure(true).host("localhost").port(port).path("/text").get().
                    toCompletableFuture().join();
            assertNotNull(response);
            assertEquals(200, response.status());
            assertEquals("RESPONSE_STRING_ÄÖÜ_€", response.content(StandardCharsets.UTF_8));
        }
    }
}
