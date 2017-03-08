package styx.http.server;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import styx.http.Cookie;
import styx.http.Header;
import styx.http.QueryParam;
import styx.http.SessionVariable;

public class Request {

    private final String protocol;
    private final String host;
    private final String path;
    private final List<QueryParam> params;
    private final List<Header> headers;
    private final List<Cookie> cookies;
    private final List<SessionVariable> session;
    private final byte[] content;

    Request(String protocol, String host, String path,
            List<QueryParam> params,
            List<Header> headers,
            List<Cookie> cookies,
            ByteArrayOutputStream content) {
        this.protocol = Objects.requireNonNull(protocol);
        this.host = Objects.requireNonNull(host);
        this.path = Objects.requireNonNull(path);
        this.params = Objects.requireNonNull(params);
        this.headers = Objects.requireNonNull(headers);
        this.cookies = Objects.requireNonNull(cookies);
        this.session = new ArrayList<>();
        this.content = content.toByteArray();
    }

    void addUrlParams(List<QueryParam> urlParams) {
        params.addAll(urlParams);
    }

    void setSessionVariables(List<SessionVariable> session) {
        this.session.addAll(session);
    }

    public String protocol() {
        return protocol;
    }

    public String method() {
        return "GET"; // TODO support methods other than GET
    }

    public String host() {
        return host;
    }

    public String path() {
        return path;
    }

    public List<QueryParam> params() {
        return Collections.unmodifiableList(params);
    }

    public Optional<String> param(String name) {
        return params.stream().
                filter(p -> p.name().equals(name)).
                map(p -> p.value()).
                findFirst();
    }

    public List<Header> headers() {
        return Collections.unmodifiableList(headers);
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
        return Collections.unmodifiableList(cookies);
    }

    public List<SessionVariable> session() {
        return Collections.unmodifiableList(session);
    }

    public Optional<String> session(String name) {
        return session.stream().
                filter(p -> p.name().equals(name)).
                map(p -> p.value()).
                findFirst();
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
