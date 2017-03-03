package styx.http.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import styx.http.QueryParam;

public class Route {

    private final Path path;
    private final BiConsumer<Request, Response> handler;

    private Route(Path path, BiConsumer<Request, Response> handler) {
        this.path = path;
        this.handler = handler;
    }

    static BiConsumer<Request, Response> combine(Route[] routes) {
        return (request, response) -> {
            Path requestPath = Paths.get(request.path());
            for(Route route : routes) {
                Optional<List<QueryParam>> match = match(route.path, requestPath);
                if(match.isPresent()) {
                    request.addParams(match.get());
                    route.handler.accept(request, response);
                    return;
                }
            }
            response.status(404);
        };
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
        private SecurityProvider secure;
        private String secureRedirectTo;

        public Builder path(String path) {
            this.path = Paths.get(path);
            return this;
        }

        public Builder secure(SecurityProvider secure) {
            return secure(secure, null);
        }

        public Builder secure(SecurityProvider secure, String redirectTo) {
            this.secure = Objects.requireNonNull(secure);
            this.secureRedirectTo = redirectTo;
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
            BiConsumer<Request, Response> handler2;
            if(secure != null) {
                handler2 = (req, res) -> {
                    Optional<List<QueryParam>> sessionParams = req.cookies().stream().
                            filter(c -> c.name().equals(SecurityProvider.COOKIE_NAME)).
                            findFirst().
                            flatMap(c -> secure.checkCookie(c.value()));
                    if(!sessionParams.isPresent()) {
                        if(secureRedirectTo != null) {
                            res.redirect(secureRedirectTo);
                        } else {
                            res.status(401);
                        }
                        return;
                    }
                    req.addParams(sessionParams.get());
                    handler.accept(req, res);
                };
            } else {
                handler2 = handler;
            }
            return new Route(path, handler2);
        }
    }
}
