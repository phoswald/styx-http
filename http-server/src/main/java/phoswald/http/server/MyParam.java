package phoswald.http.server;

public class MyParam {
    private final String name;
    private final String value;

    public MyParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }
}
