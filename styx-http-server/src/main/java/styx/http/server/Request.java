package styx.http.server;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import styx.http.Cookie;
import styx.http.Header;
import styx.http.QueryParam;

public class Request {

    private final String protocol;
    private final String host;
    private final String path;
    private final List<QueryParam> params;
    private final List<Header> headers;
    private final List<Cookie> cookies;
    private final byte[] content;

    public Request(String protocol, String host, String path,
            List<QueryParam> params,
            List<Header> headers,
            List<Cookie> cookies,
            ByteArrayOutputStream content) {
        this.protocol = Objects.requireNonNull(protocol);
        this.host = Objects.requireNonNull(host);
        this.path = Objects.requireNonNull(path);
        this.params = Collections.unmodifiableList(params);
        this.headers = Collections.unmodifiableList(headers);
        this.cookies = Collections.unmodifiableList(cookies);
        this.content = content.toByteArray();
    }

    public String protocol() {
        return protocol;
    }

    public String host() {
        return host;
    }

    public String path() {
        return path;
    }

    public List<QueryParam> params() {
        return params;
    }

    public Optional<String> param(String name) {
        return params.stream().
                filter(p -> p.name().equals(name)).
                map(p -> p.value()).
                findFirst();
    }

    public List<Header> headers() {
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

    public List<Cookie> cookies() {
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
