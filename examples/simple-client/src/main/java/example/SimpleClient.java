package example;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import styx.http.Cookie;
import styx.http.Header;
import styx.http.client.Client;
import styx.http.client.Response;

public final class SimpleClient {

    private final boolean ssl = Boolean.parseBoolean(System.getProperty("ssl", "false"));
    private final String host = System.getProperty("host", "localhost");
    private final int port = Integer.parseInt(System.getProperty("port", ssl ? "8443" : "8080"));
    private final String path = System.getProperty("path", "/dump");

    private final List<Cookie> cookies = new ArrayList<>();

    public static void main(String[] args) {
        new SimpleClient().run();
    }

    private void run() {
        cookies.add(new Cookie("my-client-stuff", "12345676789"));

        try(Client client = new Client()) {

            CompletionStage<?> f1 = client.request().
                    secure(ssl).host(host).port(port).path(path).param("cnt", "1").
                    header("user-agent", "sampleclient/1.0.0").
                    cookie(cookies).
                    get().
                    thenAccept(this::dumpResponse).
                    exceptionally(this::dumpException);

            CompletionStage<?> f2 = client.request().
                    secure(ssl).host(host).port(port).path(path).param("cnt", "2").
                    header("x-blubber", "something").
                    get().
                    thenAccept(this::dumpResponse).
                    exceptionally(this::dumpException);

            f1.toCompletableFuture().join();
            f2.toCompletableFuture().join();
        }
    }

    private synchronized void dumpResponse(Response response) {
        System.out.println("> " + response.status() + " " + response.version() + " (chunked=" + response.chunked() + ")");
        for(Header header : response.headers()) {
            System.out.println("> HEADER " + header.name() + ": " + header.value());
        }
        for(Cookie cookie : response.cookies()) {
            System.out.println("> COOKIE " + cookie.name() + "=" + cookie.value());
        }
        if(response.contentLength() > 0) {
            Charset charset = response.charset().orElseThrow(IllegalArgumentException::new);
            String content = response.content(charset);
            System.out.println("| " + content.replace("\n", "\n| "));
        }
    }

    private synchronized Void dumpException(Throwable exception) {
        exception.printStackTrace();
        return null;
    }
}
