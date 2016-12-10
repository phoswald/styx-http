package phoswald.http.client;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MyResponse {

    private String status;
    private String version;
    private boolean chunked;
    private final List<MyHeader> headers;
    private final List<MyCookie> cookies;
    private byte[] content;

    MyResponse(String status, String version, boolean chunked,
            List<MyHeader> headers,
            List<MyCookie> cookies,
            ByteArrayOutputStream content) {
        this.status = status;
        this.version = version;
        this.chunked = chunked;
        this.headers = Collections.unmodifiableList(headers);
        this.cookies = Collections.unmodifiableList(cookies);
        this.content = content.toByteArray();
    }

    public String status() {
        return status;
    }

    public String version() {
        return version;
    }

    public boolean chunked() {
        return chunked;
    }

    public List<MyHeader> headers() {
        return headers;
    }

    public Optional<String> header(String name) {
        return headers.stream().
                filter(h -> h.name().equalsIgnoreCase(name)).
                map(h -> h.value()).
                findFirst();
    }

    public Optional<Charset> charset() {
        return header("content-type").
                filter(s -> s.contains("charset=")).
                map(s -> s.substring(s.indexOf("charset=") + 8)).
                map(Charset::forName);
    }

    public List<MyCookie> cookies() {
        return cookies;
    }

    public int contentLength() {
        return content.length;
    }

    public byte[] content() {
        return content;
    }

    public String content(Charset charset) {
        return new String(content, charset);
    }
}
