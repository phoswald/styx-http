package phoswald.http.server;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;


public class MyRequest {

    private final String protocol;
    private final String host;
    private final String uri;
    private final List<MyPair> headers;
    private final List<MyPair> params;
    private final byte[] content;

    public MyRequest(String protocol, String host, String uri,
            List<MyPair> headers,
            List<MyPair> params,
            ByteArrayOutputStream content) {
        this.protocol = protocol;
        this.host = host;
        this.uri = uri;
        this.headers = Collections.unmodifiableList(headers);
        this.params = Collections.unmodifiableList(params);
        this.content = content.toByteArray();
    }

    public String protocol() {
        return protocol;
    }

    public String host() {
        return host;
    }

    public String uri() {
        return uri;
    }

    public List<MyPair> headers() {
        return headers;
    }

    public List<MyPair> params() {
        return params;
    }

    public int contentLength() {
        return content.length;
    }

    public String content(Charset charset) {
        return new String(content, charset);
    }
}
