package phoswald.http.server;

public class MyPair {
    private final String name;
    private final String value;

    public MyPair(String name, String value) {
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
