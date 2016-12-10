package phoswald.netty.server;

import java.nio.charset.StandardCharsets;

import phoswald.http.server.MyPair;
import phoswald.http.server.MyRequest;
import phoswald.http.server.MyResponse;
import phoswald.http.server.MyServer;

public final class SampleServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));

    public static void main(String[] args) throws Exception {
        new SampleServer().run();
    }

    private void run() throws Exception {

        System.err.println("Open your web browser and navigate to " +
                (SSL? "https" : "http") + "://127.0.0.1:" + PORT + '/');

        try(MyServer server = new MyServer()) {
            server.
                port(PORT).
                secure(SSL).
                handler(this::handle).
                start();
        }
    }

    private MyResponse handle(MyRequest request) {
        StringBuilder buf = new StringBuilder();

        buf.append("WELCOME TO THE WILD WILD WEB SERVER\r\n");
        buf.append("===================================\r\n");

        buf.append("VERSION: ").append(request.protocol()).append("\r\n");
        buf.append("HOSTNAME: ").append(request.host()).append("\r\n");
        buf.append("REQUEST_URI: ").append(request.uri()).append("\r\n\r\n");

        if (!request.headers().isEmpty()) {
            for (MyPair header: request.headers()) {
                buf.append("HEADER: ").append(header.name()).append(" = ").append(header.value()).append("\r\n");
            }
            buf.append("\r\n");
        }

        if (!request.params().isEmpty()) {
            for (MyPair param: request.params()) {
                buf.append("PARAM: ").append(param.name()).append(" = ").append(param.value()).append("\r\n");
            }
            buf.append("\r\n");
        }

        if(request.contentLength() > 0) {
            buf.append("CONTENT: ");
            buf.append(request.content(StandardCharsets.UTF_8));
            buf.append("\r\n");
            buf.append("END OF CONTENT\r\n");
        }

        return new MyResponse(buf.toString());
    }
}
