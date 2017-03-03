package styx.http.server;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import styx.http.QueryParam;

public interface SecurityProvider {

    public static final String COOKIE_NAME = "styx_session";

    public String signCookie(List<QueryParam> sessionParams, Instant expiry);

    public Optional<List<QueryParam>> checkCookie(String cookie);

    public default void login(Response res, Duration expiry, QueryParam... sessionParams) {
        res.cookie(COOKIE_NAME, signCookie(Arrays.asList(sessionParams), Instant.now().plus(expiry)));
    }
}
