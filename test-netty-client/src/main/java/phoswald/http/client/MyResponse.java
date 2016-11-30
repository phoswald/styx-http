package phoswald.http.client;

public class MyResponse {

    private final String text;

    MyResponse(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
