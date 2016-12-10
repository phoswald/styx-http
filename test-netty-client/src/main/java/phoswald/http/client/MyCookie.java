package phoswald.http.client;

public class MyCookie {
    private final String name;
    private final String value;

    public MyCookie(String name, String value) {
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
