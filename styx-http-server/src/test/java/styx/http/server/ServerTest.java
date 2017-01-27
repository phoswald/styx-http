package styx.http.server;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static styx.http.server.Server.route;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;

import styx.http.server.utils.PortFinder;
import styx.http.server.utils.ServerRule;

public class ServerTest {

    private int port = new PortFinder().port();

    @Rule
    public ServerRule serverRule = new ServerRule(
            server -> server.port(port).routes(
                    route().path("/text").to((req, res) -> res.write("RESPONSE_STRING_ÄÖÜ_€")),
                    route().path("/textTyped").to((req, res) -> res.contentType("text/html").charset(StandardCharsets.US_ASCII).write("<html></html>")),
                    route().path("/binary").to((req, res) -> res.write(new byte[] { 1, 2, 3, 4 })),
                    route().path("/binaryTyped").to((req, res) -> res.contentType("application/pdf").write(new byte[] { 1, 2, 3, 4 })),
                    route().path("/file").to((req, res) -> res.writeFile(Paths.get("src/test/resources/test.txt"))),
                    route().path("/resource").toResource("/test.txt"),
                    route().path("/resources/**").toResource("/"),
                    route().path("/fileSystem/**").toFileSystem("src"),
                    route().path("/query").to((req, res) -> res.write("looking for " + req.param("filter").orElse("") + ".")),
                    route().path("/noContent").to((req, res) -> res.status(201))));

    @Test
    public void getText_found_success() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/text").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("text/plain; charset=UTF-8", connection.getContentType());
        assertEquals("RESPONSE_STRING_ÄÖÜ_€", readResponseString(connection, StandardCharsets.UTF_8));
    }

    @Test
    public void getTextTyped_found_success() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/textTyped").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("text/html; charset=US-ASCII", connection.getContentType());
        assertEquals("<html></html>", readResponseString(connection, StandardCharsets.US_ASCII));
    }

    @Test
    public void getBinary_found_success() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/binary").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("application/binary", connection.getContentType());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, readResponseBytes(connection));
    }

    @Test
    public void getBinaryTyped_found_success() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/binaryTyped").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("application/pdf", connection.getContentType());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, readResponseBytes(connection));
    }

    @Test
    public void getFile_found_success() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/file").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("text/plain", connection.getContentType());
        assertEquals("TEST FILESYSTEM CONTENT\n", readResponseString(connection, StandardCharsets.UTF_8));
    }

    @Test
    public void getResource_found_success() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/resource").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("text/plain", connection.getContentType());
        assertEquals("TEST CLASSPATH CONTENT\n", readResponseString(connection, StandardCharsets.UTF_8));
    }

    @Test
    public void get_resources_found_success() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/resources/test.txt").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("text/plain", connection.getContentType());
        assertEquals("TEST CLASSPATH CONTENT\n", readResponseString(connection, StandardCharsets.UTF_8));
    }

    @Test
    public void get_fileSystem_success() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/fileSystem/test/resources/test.txt").openConnection();
        assertEquals(200, connection.getResponseCode());
        assertEquals("text/plain", connection.getContentType());
        assertEquals("TEST FILESYSTEM CONTENT\n", readResponseString(connection, StandardCharsets.UTF_8));
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

    @Test
    public void get_noContent_notFound() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/noContent").openConnection();
        assertEquals(201, connection.getResponseCode());
        assertNull(connection.getContentType());
    }

    @Test
    public void get_notExisting_notFound() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/notExisting").openConnection();
        assertEquals(404, connection.getResponseCode());
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
