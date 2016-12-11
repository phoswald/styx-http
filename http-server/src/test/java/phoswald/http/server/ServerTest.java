package phoswald.http.server;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static phoswald.http.server.Server.route;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;

import phoswald.http.server.utils.PortFinder;
import phoswald.http.server.utils.ServerRule;

public class ServerTest {

    private int port = new PortFinder().port();

    @Rule
    public ServerRule serverRule = new ServerRule(
            server -> server.port(port).routes(
                    route().path("/text").to((req, res) -> res.write("RESPONSE_STRING_ÄÖÜ_€")),
                    route().path("/binary").to((req, res) -> res.write(new byte[] { 1, 2, 3, 4 })),
                    route().path("/query").to((req, res) -> res.write("looking for " + req.param("filter").orElse("") + "."))));

    @Test
    public void getText_found_success() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/text").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("RESPONSE_STRING_ÄÖÜ_€", readResponseString(connection, StandardCharsets.UTF_8));
    }

    @Test
    public void getBinary_found_success() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/binary").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, readResponseBytes(connection));
    }

    @Test
    public void get_notExisting_notFound() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/notExisting").openConnection();
        assertEquals(404, connection.getResponseCode());
    }

    @Test
    public void getWithParams_paramSpecified_success() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/query?filter=foo").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("looking for foo.", readResponseString(connection, StandardCharsets.UTF_8));
    }

    @Test
    public void getWithParams_paramMissing_success() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/query").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("looking for .", readResponseString(connection, StandardCharsets.UTF_8));
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
