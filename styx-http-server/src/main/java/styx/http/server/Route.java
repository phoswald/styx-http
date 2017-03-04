package styx.http.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

import styx.http.QueryParam;
import styx.http.SessionVariable;

public class Route {

    private final Path path;
    private final BiConsumer<Request, Response> handler;
    private final boolean requireSession;
    private final String requireSessionRedirectTo;

    private Route(Path path, BiConsumer<Request, Response> handler, boolean requireSession, String requireSessionRedirectTo) {
        this.path = Objects.requireNonNull(path);
        this.handler = Objects.requireNonNull(handler);
        this.requireSession = requireSession;
        this.requireSessionRedirectTo = requireSessionRedirectTo;
    }

    static Function<Request, CompletableFuture<Response>> createHandler(Route[] routes, Server server) {
        return (request) -> handle(routes, server, request);
    }

    private static CompletableFuture<Response> handle(Route[] routes, Server server, Request request) {
        Path requestPath = Paths.get(request.path());
        for(Route route : routes) {
            Optional<List<QueryParam>> match = match(route.path, requestPath);
            if(match.isPresent()) {
                return route.handle(server, request, match.get());
            }
        }
        Response response = new Response();
        response.status(404);
        return CompletableFuture.completedFuture(response);
    }

    private CompletableFuture<Response> handle(Server server, Request request, List<QueryParam> urlParams) {
        Optional<List<SessionVariable>> sessionParams;
        if(server.getSessionHandler().isPresent()) {
            sessionParams = request.cookies().stream().
                    filter(c -> c.name().equals(SessionHandler.COOKIE_NAME)).
                    findFirst().
                    flatMap(c -> server.getSessionHandler().get().decodeAndVerifyCookie(c.value()));
        } else {
            sessionParams = Optional.empty();
        }
        Response response = new Response();
        if(requireSession && !sessionParams.isPresent()) {
            if(requireSessionRedirectTo != null) {
                return CompletableFuture.completedFuture(response.redirect(requireSessionRedirectTo));
            } else {
                return CompletableFuture.completedFuture(response.status(401));
            }
        }
        request.addUrlParams(urlParams);
        request.setSessionVariables(sessionParams.orElse(Collections.emptyList()));
        handler.accept(request, response);
        return CompletableFuture.completedFuture(response);
    }

    static Optional<List<QueryParam>> match(String pattern, String actual) {
        return match(Paths.get(pattern), Paths.get(actual));
    }

    static Optional<List<QueryParam>> match(Path pattern, Path actual) {
        int i = 0;
        List<QueryParam> matches = new ArrayList<>();
        while(i < pattern.getNameCount() && i < actual.getNameCount()) {
            String patternName = pattern.getName(i).toString();
            String actualName = actual.getName(i).toString();
            if(patternName.startsWith("{") && patternName.endsWith("}")) {
                matches.add(new QueryParam(patternName, actualName));
            } else if(patternName.equals("**")) {
                matches.add(new QueryParam("**", actual.subpath(i, actual.getNameCount()).toString()));
                return Optional.of(matches);
            } else if(!Objects.equals(patternName, actualName)) {
                return Optional.empty();
            }
            i++;
        }
        if(i == pattern.getNameCount() && i == actual.getNameCount()) {
            return Optional.of(matches);
        } else if(i+1 == pattern.getNameCount() && pattern.getName(i).toString().equals("**") && i == actual.getNameCount()) {
            matches.add(new QueryParam("**", "."));
            return Optional.of(matches);
        } else {
            return Optional.empty();
        }
    }

    public static class Builder {
        private Path path;
        private boolean requireSession;
        private String requireSessionRedirectTo;

        public Builder path(String path) {
            this.path = Paths.get(path);
            return this;
        }

        public Builder requireSession() {
            return requireSession(null);
        }

        public Builder requireSession(String redirectTo) {
            this.requireSession = true;
            this.requireSessionRedirectTo = redirectTo;
            return this;
        }

        public Route to(BiConsumer<Request, Response> handler) {
            return build(path, handler);
        }

        public Route toResource(String base) {
            return toResource(Paths.get(base));
        }

        public Route toResource(Path base) {
            return build(path, (req, res) -> {
                Path file = base.resolve(req.param("**").orElse(""));
                res.writeResource(file);
            });
        }

        public Route toFileSystem(String base) {
            return toFileSystem(Paths.get(base));
        }

        public Route toFileSystem(Path base) {
            return build(path, (req, res) -> {
                Path file = base.resolve(req.param("**").orElse(""));
                if(Files.isDirectory(file)) {
                    res.contentType("text/html");
                    res.write("<html>");
                    res.write("<head><title>" + file + "</title></head>");
                    res.write("<body>");
                    res.write("<h1>" + file + "</h1>");
                    if(!file.equals(base)) {
                        res.write("<a href='../'>..</a><br>");
                    }
                    try {
                        Files.list(file).
                            filter(f -> !f.getFileName().toString().startsWith(".")).
                            forEach(f -> res.write("<a href='" + f.getFileName() + (Files.isDirectory(f) ? "/" : "") + "'>" + f.getFileName() + "</a><br>"));
                    } catch (IOException e) {
                        res.status(500).write(e.toString());
                    }
                    res.write("</body>");
                    res.write("</html>");
                } else if(Files.isRegularFile(file)) {
                    res.writeFile(file);
                } else {
                    res.status(404);
                }
            });
        }

        private Route build(Path path, BiConsumer<Request, Response> handler) {
            return new Route(path, handler, requireSession, requireSessionRedirectTo);
        }
    }
}
