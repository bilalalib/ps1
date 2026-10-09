package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FilterTest {

    /*
     * Testing strategy for writtenBy(tweets, username):
     *   number of tweets: 0, 1+, > 1
     *   number of tweets by username: 0, 1, > 1
     *   case of username: same as author, different from author
     *
     * Testing strategy for inTimespan(tweets, timespan):
     *   tweets inside timespan: none, some, all
     *   tweet exactly on start or end of timespan: yes, no
     *
     * Testing strategy for containing(tweets, words):
     *   number of words: 1, > 1
     *   matching tweets: none, some, all
     *   case of word: same as in tweet, different
     *   word appears only as part of a bigger word: yes, no
     *
     * Every result is also checked for keeping the input order.
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:00:00Z");

    private static final Tweet tweet1 =
            new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 =
            new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);
    private static final Tweet tweet3 =
            new Tweet(3, "Alyssa", "Talking about the pset tonight", d3);

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // ---------- writtenBy ----------

    // covers: 0 tweets
    @Test
    public void testWrittenByEmptyList() {
        List<Tweet> writtenBy = Filter.writtenBy(Collections.<Tweet>emptyList(), "alyssa");

        assertTrue("expected empty list", writtenBy.isEmpty());
    }

    // covers: > 1 tweets, 0 tweets by username
    @Test
    public void testWrittenByNoResult() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "nobody");

        assertTrue("expected empty list", writtenBy.isEmpty());
    }

    // covers: > 1 tweets, 1 tweet by username, same case
    @Test
    public void testWrittenByMultipleTweetsSingleResult() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "bbitdiddle");

        assertEquals("expected singleton list", Arrays.asList(tweet2), writtenBy);
    }

    // covers: > 1 tweets by username, author written in a different case
    @Test
    public void testWrittenByDifferentCaseMultipleResults() {
        List<Tweet> writtenBy =
                Filter.writtenBy(Arrays.asList(tweet1, tweet2, tweet3), "ALYSSA");

        assertEquals("expected two tweets in order", Arrays.asList(tweet1, tweet3), writtenBy);
    }

    // ---------- inTimespan ----------

    // covers: no tweets inside timespan
    @Test
    public void testInTimespanNoResult() {
        Timespan before = new Timespan(Instant.parse("2016-02-16T10:00:00Z"),
                                       Instant.parse("2016-02-16T20:00:00Z"));

        assertTrue("expected empty list",
                Filter.inTimespan(Arrays.asList(tweet1, tweet2), before).isEmpty());
    }

    // covers: some tweets inside, tweets exactly on start and end are included
    @Test
    public void testInTimespanEndpointsIncluded() {
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2, tweet3),
                new Timespan(d2, d3));

        assertEquals("expected tweets on the endpoints", Arrays.asList(tweet2, tweet3), inTimespan);
    }

    // covers: all tweets inside, no tweet on the boundary
    @Test
    public void testInTimespanAllInside() {
        Timespan wide = new Timespan(Instant.parse("2016-02-17T09:00:00Z"),
                                     Instant.parse("2016-02-17T13:00:00Z"));
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2, tweet3), wide);

        assertEquals("expected all tweets in order",
                Arrays.asList(tweet1, tweet2, tweet3), inTimespan);
    }

    // ---------- containing ----------

    // covers: 1 word, all tweets match, same case
    @Test
    public void testContaining() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2),
                Arrays.asList("talk"));

        assertEquals("expected both tweets in order", Arrays.asList(tweet1, tweet2), containing);
    }

    // covers: no tweets match
    @Test
    public void testContainingNoResult() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2, tweet3),
                Arrays.asList("nothing"));

        assertTrue("expected empty list", containing.isEmpty());
    }

    // covers: different case, word only inside a bigger word ("Talking" is not "talk")
    @Test
    public void testContainingCaseAndWholeWords() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2, tweet3),
                Arrays.asList("TALK"));

        assertEquals("expected tweets 1 and 2 only", Arrays.asList(tweet1, tweet2), containing);
    }

    // covers: > 1 words, some tweets match, result keeps tweet order not word order
    @Test
    public void testContainingMultipleWords() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2, tweet3),
                Arrays.asList("pset", "#hype"));

        assertEquals("expected tweets 2 and 3 in order", Arrays.asList(tweet2, tweet3), containing);
    }
}