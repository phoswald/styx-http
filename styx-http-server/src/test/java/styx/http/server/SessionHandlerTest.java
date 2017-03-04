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

import styx.http.SessionVariable;

public class SessionHandlerTest {

    private static final List<SessionVariable> SESSION_PARAMS = Arrays.asList(new SessionVariable("uid", "xyz"), new SessionVariable("name", "joe"));
    private static final Instant EXPIRY_IN_FURTURE = LocalDateTime.of(2099, 12, 31, 23, 59, 59).toInstant(ZoneOffset.UTC);
    private static final Instant EXPIRY_IN_PAST = LocalDateTime.of(1999, 12, 31, 23, 59, 59).toInstant(ZoneOffset.UTC);

    private SessionHandler testee = new SessionHandler("");

    @Test
    public void encodeAndSignCookie_valid_sucess() {
        String cookie = testee.encodeAndSignCookie(SESSION_PARAMS, EXPIRY_IN_FURTURE);
        assertEquals("NDEwMjQ0NDc5OT91aWQ9eHl6Jm5hbWU9am9lC7rdDclZO_hU100LDiFJBr4ryYrk1WJLil53V4ApffA", cookie);
    }

    @Test
    public void encodeAndSignCookie_differentSecrets_differentHash() {
        String cookie = testee.encodeAndSignCookie(SESSION_PARAMS, EXPIRY_IN_FURTURE);
        String cookie2 = new SessionHandler("secret").encodeAndSignCookie(SESSION_PARAMS, EXPIRY_IN_FURTURE);
        assertFalse(cookie.equals(cookie2));
        assertEquals("NDEwMjQ0NDc5OT91aWQ9eHl6Jm5hbWU9am9lCdYxj1VmcyBV1HJcYI9H245NtoC169dwvC2ouFFJPHk", cookie2);
    }

    @Test
    public void decodeAndVerifyCookie_valid_success() {
        String cookie = testee.encodeAndSignCookie(SESSION_PARAMS, EXPIRY_IN_FURTURE);
        Optional<List<SessionVariable>> sessionVariables = testee.decodeAndVerifyCookie(cookie);
        assertTrue(sessionVariables.isPresent());
        assertEquals(2, sessionVariables.get().size());
        assertEquals("uid", sessionVariables.get().get(0).name());
        assertEquals("xyz", sessionVariables.get().get(0).value());
        assertEquals("name", sessionVariables.get().get(1).name());
        assertEquals("joe", sessionVariables.get().get(1).value());
    }

    @Test
    public void decodeAndVerifyCookie_expired_failure() {
        String cookie = testee.encodeAndSignCookie(SESSION_PARAMS, EXPIRY_IN_PAST);
        Optional<List<SessionVariable>> sessionVariables = testee.decodeAndVerifyCookie(cookie);
        assertTrue(!sessionVariables.isPresent());
    }

    @Test
    public void decodeAndVerifyCookie_differentSecret_failure() {
        Optional<List<SessionVariable>> sessionVariables = testee.decodeAndVerifyCookie("NDEwMjQ0NDc5OT91aWQ9eHl6Jm5hbWU9am9lCdYxj1VmcyBV1HJcYI9H245NtoC169dwvC2ouFFJPHk");
        assertTrue(!sessionVariables.isPresent());
    }

    @Test
    public void decodeAndVerifyCookie_tooShort_failure() {
        Optional<List<SessionVariable>> sessionVariables = testee.decodeAndVerifyCookie("XXXX");
        assertTrue(!sessionVariables.isPresent());
    }

    @Test
    public void decodeAndVerifyCookie_badBase64_failure() {
        Optional<List<SessionVariable>> sessionVariables = testee.decodeAndVerifyCookie("äöü");
        assertTrue(!sessionVariables.isPresent());
    }
}
