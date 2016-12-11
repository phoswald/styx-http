package phoswald.sample.server;

import static phoswald.http.server.Route.bind;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import phoswald.http.Cookie;
import phoswald.http.Header;
import phoswald.http.QueryParam;
import phoswald.http.server.Request;
import phoswald.http.server.Response;
import phoswald.http.server.Server;

public final class SampleServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));

    public static void main(String[] args) throws Exception {
        new SampleServer().run();
    }

    private void run() throws Exception {

        System.err.println("Open your web browser and navigate to " +
                (SSL? "https" : "http") + "://127.0.0.1:" + PORT + '/');

        try(Server server = new Server()) {
            server.secure(SSL).port(PORT).
                routes(
                    bind().path("/").to(this::printRequestInfo),
                    bind().path("/dump").to(this::printRequestInfo),
                    bind().path("/favicon.ico").to(this::printIcon),
                    bind().path("/ping").to((req, res) -> { res.append("Hello, world!"); }),
                    bind().path("/time").to((req, res) -> { res.append(LocalDateTime.now().toString()); })).
                start();
        }
    }

    private void printRequestInfo(Request request, Response response) {
        response.append("WELCÖME TO THE WILD WILD WEB SERVER\r\n");
        response.append("===================================\r\n");

        response.append("PROTOCOL: ").append(request.protocol()).append("\r\n");
        response.append("HOSTNAME: ").append(request.host()).append("\r\n");
        response.append("PATH:     ").append(request.path()).append("\r\n\r\n");

        for (QueryParam param: request.params()) {
            response.append("PARAM ").append(param.name()).append("=").append(param.value()).append("\r\n");
        }

        for (Header header: request.headers()) {
            response.append("HEADER ").append(header.name()).append(": ").append(header.value()).append("\r\n");
        }

        for(Cookie cookie : request.cookies()) {
            response.append("COOKIE ").append(cookie.name()).append("=").append(cookie.value()).append("\r\n");
        }

        response.contentType("text/plain", StandardCharsets.UTF_8);

        if(request.cookies().isEmpty()) {
            // set some cookies if none are present
            response.cookie("my-server-session", "blubber");
            response.cookie("my-server-other", "foobar");
        } else {
            // send back cookies sent by client
            for(Cookie cookie : request.cookies()) {
                response.cookie(cookie.name(), cookie.value());
            }
        }

        if(request.contentLength() > 0) {
            Charset charset = request.charset().orElse(StandardCharsets.US_ASCII);
            String content = request.content(charset);
            response.append("CONTENT: ");
            response.append(content);
            response.append("\r\n");
            response.append("END OF CONTENT\r\n");
        }
    }

    private void printIcon(Request request, Response response) {
        try(InputStream stream = getClass().getResourceAsStream("/favicon.ico")) {
            byte[] content = new byte[stream.available()];
            stream.read(content);
            response.append(content);
            response.contentType("image/x-icon");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
