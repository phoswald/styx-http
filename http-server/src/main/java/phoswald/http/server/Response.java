package phoswald.http.server;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import phoswald.http.Cookie;
import phoswald.http.Header;

public class Response {

    private int status = HttpResponseStatus.OK.code();
    private final List<Header> headers = new ArrayList<>();
    private final List<Cookie> cookies = new ArrayList<>();
    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    Response() { }

    public Response status(int status) {
        this.status = status;
        return this;
    }

    public Response header(String name, String value) {
        headers.add(new Header(name, value));
        return this;
    }

    public Response contentType(String contentType) {
        header(HttpHeaderNames.CONTENT_TYPE.toString(), contentType);
        return this;
    }

    public Response contentType(String contentType, Charset charset) {
        header(HttpHeaderNames.CONTENT_TYPE.toString(), contentType + "; charset=" + charset.name());
        return this;
    }

    public Response cookie(String name, String value) {
        cookies.add(new Cookie(name, value));
        return this;
    }

    public Response write(byte[] content) {
        return write(content, 0, content.length);
    }

    public Response write(byte[] content, int offset, int length) {
        this.content.write(content, offset, length);
        return this;
    }

    public Response write(String content) {
        return write(content.getBytes(StandardCharsets.UTF_8)); // TODO remove charset from here
    }

    public Response write(InputStream stream) {
        try {
            byte[] buffer = new byte[1024];
            while(true) {
                int length = stream.read(buffer);
                if(length > 0) {
                    this.content.write(buffer, 0, length);
                } else if(length == -1) {
                    break;
                }
            }
            return this;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Response writeResource(String name) {
        try(InputStream stream = getClass().getResourceAsStream(name)) {
            if(stream == null) {
                throw new FileNotFoundException("File not found on classpath: " + name);
            }
            return write(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void send(ChannelHandlerContext context, boolean requestValid, boolean keepAlive) {
        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                requestValid ? HttpResponseStatus.valueOf(status) : HttpResponseStatus.BAD_REQUEST,
                Unpooled.copiedBuffer(content.toByteArray()));

        // Set user-defined headers first
        for(Header header : headers) {
            response.headers().add(header.name(), header.value());
        }

        // Set cookies, not replacing user-defined header
        for(Cookie cookie : cookies) {
            response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie.name(), cookie.value()));
        }

        // Set hardcoded headers, might replace user-defined ones
        // Add 'Content-Length' header only for a keep-alive connection.
        // See: http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
        if (keepAlive) {
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // Write the response.
        context.write(response);

        if(!keepAlive) {
            // If keep-alive is off, close the connection once the content is fully written.
            context.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}