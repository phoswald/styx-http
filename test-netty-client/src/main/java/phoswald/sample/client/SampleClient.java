package phoswald.sample.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLException;

import phoswald.http.client.MyClient;
import phoswald.http.client.MyResponse;

public final class SampleClient {

    private static final String URL = System.getProperty("url", "http://127.0.0.1:8080/");

    public static void main(String[] args) throws URISyntaxException, SSLException, InterruptedException {
        URI uri = new URI(URL);
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
            throw new IllegalArgumentException("Only http and https is supported");
        }

        try(MyClient client = new MyClient()) {

            CompletableFuture<?> f1 = client.request().
                    secure(ssl).host(host).port(port).path(uri.getRawPath()).get().
                    thenAccept(SampleClient::dumpResponse).
                    exceptionally(SampleClient::dumpException);

            CompletableFuture<?> f2 = client.request().
                    secure(ssl).host(host).port(port).path(uri.getRawPath()).get().
                    thenAccept(SampleClient::dumpResponse).
                    exceptionally(SampleClient::dumpException);

            f1.join();
            f2.join();
        }
    }

    private static synchronized void dumpResponse(MyResponse response) {
        System.out.println("> " + response.status() + " " + response.version() + " (chunked=" + response.chunked() + ")");
        for(MyResponse.Header header : response.headers()) {
            System.out.println("> " + header.name() + ": " + header.value());
        }
        System.out.println("| " + response.content(Charset.forName("UTF-8")).replace("\n", "\n| "));
    }

    private static synchronized Void dumpException(Throwable exception) {
        exception.printStackTrace();
        return null;
    }
}
