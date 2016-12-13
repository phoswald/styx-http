package phoswald.http.server;

import static org.junit.Assert.assertEquals;
import static phoswald.http.server.Server.route;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import org.junit.Rule;
import org.junit.Test;

import phoswald.http.server.utils.PortFinder;
import phoswald.http.server.utils.ServerRule;
import phoswald.http.server.utils.TrustAllCertsRule;

public class ServerSecureTest {

    private int port = new PortFinder().port();

    @Rule
    public TrustAllCertsRule trustRule = new TrustAllCertsRule();

    @Rule
    public ServerRule serverRule = new ServerRule(
            server -> server.secure(true).port(port).routes(
                    route().path("/text").to((req, res) -> res.write("RESPONSE_STRING_ÄÖÜ_€"))));

    @Test
    public void getText_found_success() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://localhost:" + port + "/text").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("RESPONSE_STRING_ÄÖÜ_€", readResponseString(connection, StandardCharsets.UTF_8));
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
