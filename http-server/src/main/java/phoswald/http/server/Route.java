package phoswald.http.server;

import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

public class Route {

    private final String path;
    private final BiConsumer<Request, Response> handler;

    private Route(String path, BiConsumer<Request, Response> handler) {
        this.path = path;
        this.handler = handler;
    }

    static BiConsumer<Request, Response> combine(Route[] routes) {
        return (request, response) -> {
            for(Route route : routes) {
                if(route.path.equals(request.path())) {
                    route.handler.accept(request, response);
                    return;
                }
            }
            response.status(404).contentType("text/html", StandardCharsets.UTF_8).
                write("<html>\n").
                write("  <head><title>Page not found</title></head>\n").
                write("  <body>\n").
                write("    <h1>HTTP Error 404</h1>\n").
                write("    <p>Page not found</p>\n").
                write("  </body>\n").
                write("</html>\n");
        };
    }

    public static class Builder {
        private String path;

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Route to(BiConsumer<Request, Response> handler) {
            return new Route(path, handler);
        }
    }
}
