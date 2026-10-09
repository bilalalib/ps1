package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ExtractTest {

    /*
     * Testing strategy for getTimespan(tweets):
     *   number of tweets: 1, > 1
     *   order of tweets in the list: in time order, not in time order
     *   timestamps: all the same, some different
     *   (empty list is not tested: the spec does not say what to return)
     *
     * Testing strategy for getMentionedUsers(tweets):
     *   number of tweets: 1, > 1
     *   mentions in a tweet: 0, 1, > 1
     *   same user mentioned: once, more than once with different case
     *   position of mention: start of text, middle, end
     *   "@" preceded by a username character (email address): yes, no
     *   mention followed by punctuation: yes, no
     *   username characters: letters only, with - and _
     *
     * The returned set may use any case for a username, so the tests
     * compare usernames after converting them to lowercase.
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:30:00Z");

    private static final Tweet tweet1 =
            new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 =
            new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);
    private static final Tweet tweet3 =
            new Tweet(3, "alyssa", "@bbitdiddle saving you a seat, ask @Evaluator too", d3);
    private static final Tweet tweet4 =
            new Tweet(4, "evaluator", "thanks @BBitDiddle! mail me at eval@mit.edu", d2);
    private static final Tweet tweet5 =
            new Tweet(5, "cy_d_fect", "see you there @cy-d-fect_2", d1);

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // ---------- getTimespan ----------

    // covers: > 1 tweet, in time order, different timestamps
    @Test
    public void testGetTimespanTwoTweets() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1, tweet2));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    // covers: 1 tweet, so start and end are the same
    @Test
    public void testGetTimespanOneTweet() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet2));

        assertEquals("expected start", d2, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    // covers: > 1 tweet, not in time order
    @Test
    public void testGetTimespanUnorderedTweets() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet3, tweet1, tweet2));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d3, timespan.getEnd());
    }

    // covers: > 1 tweet, all timestamps the same
    @Test
    public void testGetTimespanSameTimestamps() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1, tweet5));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d1, timespan.getEnd());
    }

    // ---------- getMentionedUsers ----------

    // covers: 1 tweet, 0 mentions
    @Test
    public void testGetMentionedUsersNoMention() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet1));

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    // covers: 1 tweet, > 1 mentions, mention at start and in the middle
    @Test
    public void testGetMentionedUsersTwoMentions() {
        Set<String> mentionedUsers =
                toLowerCase(Extract.getMentionedUsers(Arrays.asList(tweet3)));

        assertEquals("expected two users",
                new HashSet<>(Arrays.asList("bbitdiddle", "evaluator")), mentionedUsers);
    }

    // covers: > 1 tweets, same user mentioned with different case
    @Test
    public void testGetMentionedUsersDifferentCase() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet3, tweet4));

        assertEquals("expected each user once", 2, mentionedUsers.size());
        assertEquals("expected two users",
                new HashSet<>(Arrays.asList("bbitdiddle", "evaluator")),
                toLowerCase(mentionedUsers));
    }

    // covers: "@" inside an email address is not a mention, mention followed by punctuation
    @Test
    public void testGetMentionedUsersEmailIsNotMention() {
        Set<String> mentionedUsers =
                toLowerCase(Extract.getMentionedUsers(Arrays.asList(tweet4)));

        assertFalse("mit is not mentioned", mentionedUsers.contains("mit"));
        assertEquals("expected one user",
                new HashSet<>(Arrays.asList("bbitdiddle")), mentionedUsers);
    }

    // covers: mention at the end of the text, username with - and _
    @Test
    public void testGetMentionedUsersSpecialCharacters() {
        Set<String> mentionedUsers =
                toLowerCase(Extract.getMentionedUsers(Arrays.asList(tweet5)));

        assertEquals("expected one user",
                new HashSet<>(Arrays.asList("cy-d-fect_2")), mentionedUsers);
    }

    // usernames are case-insensitive, so compare them in lowercase
    private static Set<String> toLowerCase(Set<String> usernames) {
        Set<String> result = new HashSet<>();
        for (String username : usernames) {
            result.add(username.toLowerCase());
        }
        return result;
    }
}