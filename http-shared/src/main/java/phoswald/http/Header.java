package phoswald.http;

import java.util.Objects;

public class Header {

    private final String name;
    private final String value;

    public Header(String name, String value) {
        this.name = Objects.requireNonNull(name);
        this.value = Objects.requireNonNull(value);
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }
}
