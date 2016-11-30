package phoswald.sample.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLException;

import phoswald.http.client.MyClient;
import phoswald.http.client.MyRequest;

public final class SampleClient {

    private static final String URL = System.getProperty("url", "http://127.0.0.1:8080/");

    public static void main(String[] args) throws URISyntaxException, SSLException, InterruptedException {
        URI uri = new URI(URL);
        String scheme = Optional.ofNullable(uri.getScheme()).orElse("http");
        String host = Optional.ofNullable(uri.getHost()).orElse("127.0.0.1");
        int port = uri.getPort();
        boolean useSsl;
        if(scheme.equals("http")) {
            if(port == -1) {
                port = 80;
            }
            useSsl = false;
        } else if(scheme.equals("https")) {
            if(port == -1) {
                port = 443;
            }
            useSsl = true;
        } else {
            throw new IllegalArgumentException("Only http and https is supported");
        }

        try(MyClient client = new MyClient()) {

            CompletableFuture<?> f1 = MyRequest.build(client).
                    host(host).port(port).path(uri.getRawPath()).
                    get(useSsl).
                    thenAccept(r -> System.out.println(r.toString())).
                    exceptionally(e -> { e.printStackTrace(); return null; });

            CompletableFuture<?> f2 = MyRequest.build(client).
                    host(host).port(port).path(uri.getRawPath()).
                    get(useSsl).
                    thenAccept(r -> System.out.println(r.toString())).
                    exceptionally(e -> { e.printStackTrace(); return null; });

            f1.join();
            f2.join();
        }
    }
}
