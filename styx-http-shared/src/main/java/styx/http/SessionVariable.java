package styx.http;

import java.util.Objects;

public class SessionVariable {

    private final String name;
    private final String value;

    public SessionVariable(String name, String value) {
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
