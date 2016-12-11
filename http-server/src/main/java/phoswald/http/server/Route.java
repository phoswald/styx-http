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
                append("<html>\n").
                append("  <head><title>Page not found</title></head>\n").
                append("  <body>\n").
                append("    <h1>HTTP Error 404</h1>\n").
                append("    <p>Page not found</p>\n").
                append("  </body>\n").
                append("</html>\n");
        };
    }

    public static Builder bind() {
        return new Builder();
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
