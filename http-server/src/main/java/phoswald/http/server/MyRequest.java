package phoswald.http.server;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import phoswald.http.MyCookie;
import phoswald.http.MyHeader;
import phoswald.http.MyParam;

public class MyRequest {

    private final String protocol;
    private final String host;
    private final String path;
    private final List<MyParam> params;
    private final List<MyHeader> headers;
    private final List<MyCookie> cookies;
    private final byte[] content;

    public MyRequest(String protocol, String host, String path,
            List<MyParam> params,
            List<MyHeader> headers,
            List<MyCookie> cookies,
            ByteArrayOutputStream content) {
        this.protocol = protocol;
        this.host = host;
        this.path = path;
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

    public List<MyParam> params() {
        return params;
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
