package phoswald.http.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import phoswald.http.MyCookie;
import phoswald.http.MyHeader;

public class MyResponse {

    private int status = HttpResponseStatus.OK.code();
    private final List<MyHeader> headers = new ArrayList<>();
    private final List<MyCookie> cookies = new ArrayList<>();
    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    MyResponse() { }

    public MyResponse status(int status) {
        this.status = status;
        return this;
    }

    public MyResponse header(String name, String value) {
        headers.add(new MyHeader(name, value));
        return this;
    }

    public MyResponse contentType(String contentType) {
        header(HttpHeaderNames.CONTENT_TYPE.toString(), contentType);
        return this;
    }

    public MyResponse contentType(String contentType, Charset charset) {
        header(HttpHeaderNames.CONTENT_TYPE.toString(), contentType + "; charset=" + charset.name());
        return this;
    }

    public MyResponse cookie(String name, String value) {
        cookies.add(new MyCookie(name, value));
        return this;
    }

    public MyResponse append(byte[] content) {
        try {
            this.content.write(content);
            return this;
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public MyResponse append(String content) {
        return append(content.getBytes(StandardCharsets.UTF_8));
    }

    void send(ChannelHandlerContext context, boolean keepAlive, boolean requestValid) {
        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                requestValid ? HttpResponseStatus.valueOf(status) : HttpResponseStatus.BAD_REQUEST,
                Unpooled.copiedBuffer(content.toByteArray()));

        // Set user-defined headers first
        for(MyHeader header : headers) {
            response.headers().add(header.name(), header.value());
        }

        // Set cookies, not replacing user-defined header
        for(MyCookie cookie : cookies) {
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
