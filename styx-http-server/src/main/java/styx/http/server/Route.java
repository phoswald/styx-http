package styx.http.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
                if(route.path.equals(request.path()) ||  // TODO C L E A N U P
                        (route.path.endsWith("/**") && request.path().startsWith(route.path.substring(0, route.path.length()-2)))) {
                    route.handler.accept(request, response);
                    return;
                }
            }
            response.status(404).contentType("text/html").
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

        public Route toFileSystem(Path base) {
            return new Route(path, (req, res) -> {
                Path file = base.resolve(req.path().substring(this.path.length()-2)); // TODO C L E A N U P
                System.out.println("HANDLING: req.path()=" + req.path());
                System.out.println("          this.path =" + this.path);
                System.out.println("          base      =" + base);
                System.out.println("          ---->>>>> =" + file);
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
                    String fileName = file.getFileName().toString();
                    String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                    switch(extension.toLowerCase()) {
                        case "txt":  res.contentType("text/plain"); break;
                        case "htm":
                        case "html": res.contentType("text/html"); break;
                        case "xml":  res.contentType("application/xml"); break;
                        case "jpg":
                        case "jpeg": res.contentType("image/jpeg"); break;
                        case "ico":  res.contentType("image/x-icon"); break;
                    }
                    res.writeFile(file);
                } else {
                    res.status(404);
                }
            });
        }
    }
}
