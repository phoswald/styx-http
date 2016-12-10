package phoswald.http.server;

public class MyResponse {

    private final String content;

    public MyResponse(String content) {
        this.content = content;
    }

    public String content() {
        return content;
    }
}
