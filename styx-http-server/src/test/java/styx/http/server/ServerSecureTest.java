package styx.http.server;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static styx.http.server.Server.route;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;

import org.junit.Rule;
import org.junit.Test;

import styx.http.QueryParam;
import styx.http.server.utils.PortFinder;
import styx.http.server.utils.ServerRule;
import styx.http.server.utils.TrustAllCertsRule;

public class ServerSecureTest {

    private int port = new PortFinder().port();

    private SecurityProvider sp = new DefaultSecurityProvider();

    @Rule
    public TrustAllCertsRule trustRule = new TrustAllCertsRule();

    @Rule
    public ServerRule serverRule = new ServerRule(
            server -> server.secure(true).port(port).routes(
                    route().path("/text").to((req, res) -> res.write("RESPONSE_STRING_ÄÖÜ_€")),
                    route().secure(sp).path("/secret").to((req, res) -> res.write("Je t'aime, " + req.param("user").orElse(null) + ".")),
                    route().path("/login").to((req, res) -> { sp.login(res, Duration.ofMinutes(60), new QueryParam("user", req.param("who").get())); })));

    @Test
    public void getText_found_success() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://localhost:" + port + "/text").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("RESPONSE_STRING_ÄÖÜ_€", readResponseString(connection, StandardCharsets.UTF_8));
    }

    @Test
    public void getText_securedNoCookie_unauthorized() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://localhost:" + port + "/secret").openConnection();
        assertEquals(401, connection.getResponseCode());
        assertEquals(0, connection.getContentLength());
    }

    @Test
    public void getText_securedInvalidCookie_unauthorized() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://localhost:" + port + "/secret").openConnection();
        connection.setRequestProperty("Cookie", SecurityProvider.COOKIE_NAME + "=" + "XXXX");
        assertEquals(401, connection.getResponseCode());
        assertEquals(0, connection.getContentLength());
    }

    @Test
    public void getText_securedValidCookie_success() throws IOException {
        Instant expiry = Instant.now().plusSeconds(1000);
        String cookie = sp.signCookie(Arrays.asList(new QueryParam("user", "Philip")), expiry);
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://localhost:" + port + "/secret").openConnection();
        connection.setRequestProperty("Cookie", SecurityProvider.COOKIE_NAME + "=" + cookie);
        assertEquals(200, connection.getResponseCode());
        assertTrue(connection.getContentLength() > 0);
        assertEquals("Je t'aime, Philip.", readResponseString(connection, StandardCharsets.UTF_8));
    }

    @Test
    public void getText_cookieFromLogin_success() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://localhost:" + port + "/login?who=Guest").openConnection();
        assertEquals(200, connection.getResponseCode());
        String header = connection.getHeaderField("Set-Cookie");
        assertThat(header, startsWith(SecurityProvider.COOKIE_NAME + "="));
        String cookie = header.substring(SecurityProvider.COOKIE_NAME.length() + 1);

        connection = (HttpsURLConnection) new URL("https://localhost:" + port + "/secret").openConnection();
        connection.setRequestProperty("Cookie", SecurityProvider.COOKIE_NAME + "=" + cookie);
        assertEquals(200, connection.getResponseCode());
        assertTrue(connection.getContentLength() > 0);
        assertEquals("Je t'aime, Guest.", readResponseString(connection, StandardCharsets.UTF_8));
    }

    private String readResponseString(HttpURLConnection connection, Charset charset) throws IOException {
        return new String(readResponseBytes(connection), charset);
    }

    private byte[] readResponseBytes(HttpURLConnection connection) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try(InputStream response = connection.getInputStream()) {
            int b;
            while((b = response.read()) != -1) {
                bytes.write(b);
            }
        }
        return bytes.toByteArray();
    }
}
