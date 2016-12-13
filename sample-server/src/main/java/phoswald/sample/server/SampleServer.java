package phoswald.sample.server;

import static phoswald.http.server.Server.route;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import phoswald.http.Cookie;
import phoswald.http.Header;
import phoswald.http.QueryParam;
import phoswald.http.server.Request;
import phoswald.http.server.Response;
import phoswald.http.server.Server;

public final class SampleServer {

    private final boolean ssl = Boolean.parseBoolean(System.getProperty("ssl", "false"));
    private final int port = Integer.parseInt(System.getProperty("port", ssl ? "8443" : "8080"));
    private final Path content = Paths.get(System.getProperty("content", ".")).toAbsolutePath();

    public static void main(String[] args) throws Exception {
        new SampleServer().run();
    }

    private void run() {
        System.out.println("Open your web browser and navigate to " +
                (ssl ? "https" : "http") + "://127.0.0.1:" + port + '/');

        try(Server server = new Server()) {
            server.secure(ssl).port(port).routes(
                    route().path("/").to((req, res) -> res.contentType("text/html").writeResource("/index.html")),
                    route().path("/favicon.ico").to((req, res) -> res.contentType("image/x-icon").writeResource("/favicon.ico")),
                    route().path("/dump").to(this::dumpRequest),
                    route().path("/ping").to((req, res) -> res.write("Hello, world!\n")),
                    route().path("/greet").to((req, res) -> res.write("Hello, " + req.param("name").orElse("stranger") + "!\n")),
                    route().path("/time").to((req, res) -> res.write(LocalDateTime.now().toString() + "\n")),
                    route().path("/content/**").toFileSystem(content)).
                run();
        }
    }

    private void dumpRequest(Request request, Response response) {
        response.write("PROTOCOL: ").write(request.protocol()).write("\n");
        response.write("HOSTNAME: ").write(request.host()).write("\n");
        response.write("PATH:     ").write(request.path()).write("\n\n");

        for (QueryParam param: request.params()) {
            response.write("PARAM ").write(param.name()).write("=").write(param.value()).write("\n");
        }

        for (Header header: request.headers()) {
            response.write("HEADER ").write(header.name()).write(": ").write(header.value()).write("\n");
        }

        for(Cookie cookie : request.cookies()) {
            response.write("COOKIE ").write(cookie.name()).write("=").write(cookie.value()).write("\n");
        }

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
            response.write("CONTENT: ");
            response.write(content.replace("\n", "\n         "));
            response.write("END\n");
        }
    }
}
