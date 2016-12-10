package phoswald.sample.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import phoswald.http.MyCookie;
import phoswald.http.MyException;
import phoswald.http.MyHeader;
import phoswald.http.client.MyClient;
import phoswald.http.client.MyResponse;

public final class SampleClient {

    private static final String URL = System.getProperty("url", "http://127.0.0.1:8080/");

    private final List<MyCookie> cookies = new ArrayList<>();

    public static void main(String[] args) throws URISyntaxException {
        new SampleClient().run(new URI(URL));
    }

    private void run(URI uri) {
        String scheme = Optional.ofNullable(uri.getScheme()).orElse("http");
        String host = Optional.ofNullable(uri.getHost()).orElse("127.0.0.1");
        int port = uri.getPort();
        boolean ssl;
        if(scheme.equals("http")) {
            if(port == -1) {
                port = 80;
            }
            ssl = false;
        } else if(scheme.equals("https")) {
            if(port == -1) {
                port = 443;
            }
            ssl = true;
        } else {
            throw new MyException("Only http and https is supported");
        }

        cookies.add(new MyCookie("my-client-stuff", "12345676789"));

        try(MyClient client = new MyClient()) {

            CompletableFuture<?> f1 = client.request().
                    secure(ssl).host(host).port(port).path(uri.getRawPath()).param("cnt", "1").
                    header("user-agent", "sampleclient/1.0.0").
                    cookie(cookies).
                    get().
                    thenAccept(this::dumpResponse).
                    exceptionally(this::dumpException);

            CompletableFuture<?> f2 = client.request().
                    secure(ssl).host(host).port(port).path(uri.getRawPath()).param("cnt", "2").
                    header("x-blubber", "something").
                    get().
                    thenAccept(this::dumpResponse).
                    exceptionally(this::dumpException);

            f1.join();
            f2.join();
        }
    }

    private synchronized void dumpResponse(MyResponse response) {
        System.out.println("> " + response.status() + " " + response.version() + " (chunked=" + response.chunked() + ")");
        for(MyHeader header : response.headers()) {
            System.out.println("> HEADER " + header.name() + ": " + header.value());
        }
        for(MyCookie cookie : response.cookies()) {
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
