package styx.http.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import styx.http.QueryParam;

public class RouteTest {

    @Test
    public void match_endingSlash_noDifference() {
        assertTrue(Route.match("/foo",  "/foo" ).isPresent());
        assertTrue(Route.match("/foo/", "/foo" ).isPresent());
        assertTrue(Route.match("/foo",  "/foo/").isPresent());
        assertTrue(Route.match("/foo/", "/foo/").isPresent());
    }

    @Test
    public void match_notMatching_notPresent() {
        assertFalse(Route.match("/", "/other").isPresent());
        assertFalse(Route.match("/dir", "/").isPresent());
        assertFalse(Route.match("/dir", "/other").isPresent());
        assertFalse(Route.match("/dir/file.html", "/dir").isPresent());
    }

    @Test
    public void match_matchingExactly_empty() {
        List<QueryParam> result = Collections.emptyList();
        assertEqualParams(result, Route.match("/", "/").get());
        assertEqualParams(result, Route.match("/dir", "/dir").get());
        assertEqualParams(result, Route.match("/dir/file.html", "/dir/file.html").get());
    }

    @Test
    public void match_matchingWithSimpleTail_tailReturned() {
        List<QueryParam> result = Collections.singletonList(new QueryParam("**", "file.html"));
        assertEqualParams(result, Route.match("/**", "/file.html").get());
        assertEqualParams(result, Route.match("/dir/**", "/dir/file.html").get());
        assertEqualParams(result, Route.match("/dir/dir2/**", "/dir/dir2/file.html").get());
    }

    @Test
    public void match_matchingWithLongTail_tailReturned() {
        List<QueryParam> result = Collections.singletonList(new QueryParam("**", "subdir/file.html"));
        assertEqualParams(result, Route.match("/**", "/subdir/file.html").get());
        assertEqualParams(result, Route.match("/dir/**", "/dir/subdir/file.html").get());
        assertEqualParams(result, Route.match("/dir/dir2/**", "/dir/dir2/subdir/file.html").get());
    }

    @Test
    public void match_matchingWithEmptyTail_tailReturned() {
        List<QueryParam> result = Collections.singletonList(new QueryParam("**", "."));
        assertEqualParams(result, Route.match("/**", "/").get());
        assertEqualParams(result, Route.match("/dir/**", "/dir").get());
        assertEqualParams(result, Route.match("/dir/**", "/dir/").get());
        assertEqualParams(result, Route.match("/dir/dir2/**", "/dir/dir2").get());
        assertEqualParams(result, Route.match("/dir/dir2/**", "/dir/dir2/").get());
    }

    @Test
    public void match_matchingWithVariable_variableReturned() {
        List<QueryParam> result = Collections.singletonList(new QueryParam("{id}", "1234"));
        assertEqualParams(result, Route.match("/{id}", "/1234").get());
        assertEqualParams(result, Route.match("/dir/{id}", "/dir/1234").get());
        assertEqualParams(result, Route.match("/{id}/sub", "/1234/sub").get());
    }

    @Test
    public void match_matchingWithVariableAndTail_bothReturned() {
        List<QueryParam> result = Arrays.asList(new QueryParam("{id}", "1234"), new QueryParam("**", "subdir/subdir2"));
        assertEqualParams(result, Route.match("/{id}/**", "/1234/subdir/subdir2").get());
        assertEqualParams(result, Route.match("/dir/{id}/**", "/dir/1234/subdir/subdir2").get());
        assertEqualParams(result, Route.match("/dir/{id}/**", "/dir/1234/subdir/subdir2/").get());
    }

    private void assertEqualParams(List<QueryParam> expected, List<QueryParam> actual) {
        for(int i = 0; i < expected.size() && i < actual.size(); i++) {
            assertEquals(actual.toString(), expected.get(i).name(), actual.get(i).name());
            assertEquals(actual.toString(), expected.get(i).value(), actual.get(i).value());
        }
        assertEquals(actual.toString(), expected.size(), actual.size());
    }

}
