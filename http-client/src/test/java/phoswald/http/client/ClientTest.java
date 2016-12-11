package phoswald.http.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;

import phoswald.http.client.utils.PortFinder;
import phoswald.http.client.utils.ServerRule;

public class ClientTest {

    private int port = new PortFinder().port();

    @Rule
    public ServerRule serverRule = new ServerRule(
            server -> server.port(port).handler((req, res) -> res.write("RESPONSE_STRING_ÄÖÜ_€")));

    @Test
    public void getText_found_success() {
        try(Client testee = new Client()) {
            Response response = testee.request().
                    host("localhost").port(port).path("/ping").get().
                    toCompletableFuture().join();
            assertNotNull(response);
            assertEquals("200 OK", response.status());
            assertEquals("RESPONSE_STRING_ÄÖÜ_€", response.content(StandardCharsets.UTF_8));
        }
    }
}
