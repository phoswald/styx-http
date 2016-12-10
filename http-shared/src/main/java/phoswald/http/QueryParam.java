package phoswald.http;

public class QueryParam {
    private final String name;
    private final String value;

    public QueryParam(String name, String value) {
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
