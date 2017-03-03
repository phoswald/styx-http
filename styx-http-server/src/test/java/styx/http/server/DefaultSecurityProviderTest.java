package styx.http.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import styx.http.QueryParam;

public class DefaultSecurityProviderTest {

    private static final List<QueryParam> SESSION_PARAMS = Arrays.asList(new QueryParam("uid", "xyz"), new QueryParam("name", "joe"));
    private static final Instant EXPIRY_IN_FURTURE = LocalDateTime.of(2099, 12, 31, 23, 59, 59).toInstant(ZoneOffset.UTC);
    private static final Instant EXPIRY_IN_PAST = LocalDateTime.of(1999, 12, 31, 23, 59, 59).toInstant(ZoneOffset.UTC);

    private SecurityProvider testee = new DefaultSecurityProvider("");

    @Test
    public void signCookie_valid_sucess() {
        String cookie = testee.signCookie(SESSION_PARAMS, EXPIRY_IN_FURTURE);
        assertEquals("NDEwMjQ0NDc5OT91aWQ9eHl6Jm5hbWU9am9lC7rdDclZO_hU100LDiFJBr4ryYrk1WJLil53V4ApffA", cookie);
    }

    @Test
    public void signCookie_differentSecrets_differentHash() {
        String cookie = testee.signCookie(SESSION_PARAMS, EXPIRY_IN_FURTURE);
        String cookie2 = new DefaultSecurityProvider("secret").signCookie(SESSION_PARAMS, EXPIRY_IN_FURTURE);
        assertFalse(cookie.equals(cookie2));
        assertEquals("NDEwMjQ0NDc5OT91aWQ9eHl6Jm5hbWU9am9lCdYxj1VmcyBV1HJcYI9H245NtoC169dwvC2ouFFJPHk", cookie2);
    }

    @Test
    public void checkCookie_valid_success() {
        String cookie = testee.signCookie(SESSION_PARAMS, EXPIRY_IN_FURTURE);
        Optional<List<QueryParam>> sessionParams = testee.checkCookie(cookie);
        assertTrue(sessionParams.isPresent());
        assertEquals(2, sessionParams.get().size());
        assertEquals("uid", sessionParams.get().get(0).name());
        assertEquals("xyz", sessionParams.get().get(0).value());
        assertEquals("name", sessionParams.get().get(1).name());
        assertEquals("joe", sessionParams.get().get(1).value());
    }

    @Test
    public void checkCookie_expired_failure() {
        String cookie = testee.signCookie(SESSION_PARAMS, EXPIRY_IN_PAST);
        Optional<List<QueryParam>> sessionParams = testee.checkCookie(cookie);
        assertTrue(!sessionParams.isPresent());
    }

    @Test
    public void checkCookie_differentSecret_failure() {
        Optional<List<QueryParam>> sessionParams = testee.checkCookie("NDEwMjQ0NDc5OT91aWQ9eHl6Jm5hbWU9am9lCdYxj1VmcyBV1HJcYI9H245NtoC169dwvC2ouFFJPHk");
        assertTrue(!sessionParams.isPresent());
    }

    @Test
    public void checkCookie_tooShort_failure() {
        Optional<List<QueryParam>> sessionParams = testee.checkCookie("XXXX");
        assertTrue(!sessionParams.isPresent());
    }

    @Test
    public void checkCookie_badBase64_failure() {
        Optional<List<QueryParam>> sessionParams = testee.checkCookie("äöü");
        assertTrue(!sessionParams.isPresent());
    }
}
