package phoswald.http.client;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

public class MyResponse {

    private String status;
    private String version;
    private boolean chunked;
    private final List<Header> headers;
    private byte[] content;

    MyResponse(String status, String version, boolean chunked, List<Header> headers, ByteArrayOutputStream content) {
        this.status = status;
        this.version = version;
        this.chunked = chunked;
        this.headers = Collections.unmodifiableList(headers);
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

    public List<Header> headers() {
        return headers;
    }

    public String content(Charset charset) {
        return new String(content, charset);
    }

    public static class Header {
        private final String name;
        private final String value;

        public Header(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String name() {
            return name;
        }

        public String value() {
            return value;
        }
    }
}
