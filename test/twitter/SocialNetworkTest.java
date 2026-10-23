/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class SocialNetworkTest {

    /*
     * TESTING STRATEGY
     *
     * guessFollowsGraph(List<Tweet> tweets)
     *   Partition on number of tweets:        0, 1, >1
     *   Partition on mentions per tweet:      0, 1, >1
     *   Partition on tweets per author:       1, >1 (same author mentions
     *                                         different people in different tweets)
     *   Partition on username case:           mentions/authors in same case,
     *                                         in different case
     *   Partition on special mention forms:   self-mention, e-mail address
     *                                         (not a mention)
     *
     *   The spec is UNDERDETERMINED: an implementation may add follows
     *   relationships from other evidence (hashtags, etc.), and may or may not
     *   list a user as a key when that user follows nobody. So the tests:
     *     - only ever assert that REQUIRED follows are present (never that
     *       the graph is exactly some map),
     *     - for "no evidence" cases assert that no follows exist at all,
     *     - compare usernames in lower case (names are case-insensitive).
     *
     * influencers(Map<String, Set<String>> followsGraph)
     *   Partition on graph size:              empty, 1 user, >1 users
     *   Partition on follower counts:         all zero, one clear winner,
     *                                         all distinct, some tied
     *   Output must contain every distinct username exactly once, in
     *   descending follower count. The order among tied users is not
     *   specified, so tie tests never assert the order inside a tie.
     */

    // ---------- helpers (kept in the test class, NOT in SocialNetwork) ----------

    private static final Instant T = Instant.parse("2016-02-17T10:00:00Z");

    private static Tweet tweet(long id, String author, String text) {
        return new Tweet(id, author, text, T);
    }

    /** Lower-cases all usernames in a graph so tests are case-insensitive. */
    private static Map<String, Set<String>> normalize(Map<String, Set<String>> graph) {
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : graph.entrySet()) {
            Set<String> follows = result.computeIfAbsent(e.getKey().toLowerCase(), k -> new HashSet<>());
            for (String name : e.getValue()) {
                follows.add(name.toLowerCase());
            }
        }
        return result;
    }

    private static boolean follows(Map<String, Set<String>> graph, String a, String b) {
        Set<String> s = normalize(graph).get(a.toLowerCase());
        return s != null && s.contains(b.toLowerCase());
    }

    private static Set<String> setOf(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    // ---------- sanity ----------

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // ---------- guessFollowsGraph ----------

    // Test 1: empty list of tweets
    @Test
    public void testGuessFollowsGraphEmpty() {
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(new ArrayList<>());

        assertTrue("expected empty graph", followsGraph.isEmpty());
    }

    // Test 2: tweets without mentions
    @Test
    public void testGuessFollowsGraphNoMentions() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "alice", "just had lunch"),
                tweet(2, "bob", "good morning everyone"));

        Map<String, Set<String>> graph = normalize(SocialNetwork.guessFollowsGraph(tweets));

        // An author may or may not appear as a key (spec allows both), but
        // nobody may follow anybody since there is no evidence.
        for (Set<String> followed : graph.values()) {
            assertTrue("no mentions, so nobody should follow anybody", followed.isEmpty());
        }
    }

    // Test 3: single mention
    @Test
    public void testGuessFollowsGraphSingleMention() {
        List<Tweet> tweets = Arrays.asList(tweet(1, "alice", "hello @bob"));

        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue("alice should follow bob", follows(graph, "alice", "bob"));
    }

    // Test 4: multiple mentions in one tweet
    @Test
    public void testGuessFollowsGraphMultipleMentions() {
        List<Tweet> tweets = Arrays.asList(tweet(1, "alice", "hi @bob and @carol and @dave"));

        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue(follows(graph, "alice", "bob"));
        assertTrue(follows(graph, "alice", "carol"));
        assertTrue(follows(graph, "alice", "dave"));
    }

    // Test 5: multiple tweets from one user
    @Test
    public void testGuessFollowsGraphMultipleTweetsSameUser() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "alice", "hello @bob"),
                tweet(2, "alice", "meeting @carol today"),
                tweet(3, "alice", "again @bob"));

        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue("alice should follow bob", follows(graph, "alice", "bob"));
        assertTrue("alice should follow carol", follows(graph, "alice", "carol"));
    }

    // Extra: usernames are case-insensitive
    @Test
    public void testGuessFollowsGraphCaseInsensitive() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "ALICE", "hello @BoB"),
                tweet(2, "alice", "hi @bob"));

        Map<String, Set<String>> graph = normalize(SocialNetwork.guessFollowsGraph(tweets));

        assertTrue(follows(graph, "alice", "bob"));
        assertEquals("alice and ALICE are one user, bob one user", 1, graph.get("alice").size());
    }

    // Extra: users can't follow themselves
    @Test
    public void testGuessFollowsGraphSelfMention() {
        List<Tweet> tweets = Arrays.asList(tweet(1, "alice", "note to self @alice"));

        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        assertFalse("users can't follow themselves", follows(graph, "alice", "alice"));
    }

    // Extra: an e-mail address is not a mention
    @Test
    public void testGuessFollowsGraphEmailIsNotMention() {
        List<Tweet> tweets = Arrays.asList(tweet(1, "alice", "write to bitdiddle@mit.edu please"));

        Map<String, Set<String>> graph = normalize(SocialNetwork.guessFollowsGraph(tweets));

        assertFalse(follows(graph, "alice", "mit"));
        assertFalse(follows(graph, "alice", "mit.edu"));
    }

    // Extra: every username in the result is an author or a mention
    @Test
    public void testGuessFollowsGraphOnlyKnownUsernames() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "alice", "hi @bob"),
                tweet(2, "carol", "hey @alice"));
        Set<String> allowed = setOf("alice", "bob", "carol");

        Map<String, Set<String>> graph = normalize(SocialNetwork.guessFollowsGraph(tweets));

        for (Map.Entry<String, Set<String>> e : graph.entrySet()) {
            assertTrue(allowed.contains(e.getKey()));
            assertTrue(allowed.containsAll(e.getValue()));
        }
    }

    // ---------- influencers ----------

    // Test 6: empty graph
    @Test
    public void testInfluencersEmpty() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertTrue("expected empty list", influencers.isEmpty());
    }

    // Test 7: single user without followers.
    // The spec says the result lists ALL distinct usernames in the graph, so
    // the one user still appears, just with zero followers.
    @Test
    public void testInfluencersSingleUserNoFollowers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", new HashSet<>());

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(Arrays.asList("alice"), influencers);
    }

    // Test 8: single influencer
    @Test
    public void testInfluencersSingleInfluencer() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", setOf("bob"));
        followsGraph.put("carol", setOf("bob"));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals("all 3 distinct users listed", 3, influencers.size());
        assertEquals("bob has 2 followers, the most", "bob", influencers.get(0));
    }

    // Test 9: multiple influencers, all different follower counts
    @Test
    public void testInfluencersMultipleInfluencersOrdering() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", setOf("bob", "carol", "dave"));
        followsGraph.put("erin", setOf("bob", "carol"));
        followsGraph.put("frank", setOf("bob"));
        // followers: bob=3, carol=2, dave=1, alice=erin=frank=0

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(6, influencers.size());
        assertEquals("bob", influencers.get(0));
        assertEquals("carol", influencers.get(1));
        assertEquals("dave", influencers.get(2));
    }

    // Test 10: tied influence. Order inside the tie is unspecified.
    @Test
    public void testInfluencersTied() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", setOf("carol"));
        followsGraph.put("bob", setOf("dave"));
        // carol=1, dave=1 (tied), alice=bob=0

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(4, influencers.size());
        assertEquals("carol and dave (tied) fill the first two places",
                setOf("carol", "dave"), new HashSet<>(influencers.subList(0, 2)));
        assertEquals("alice and bob (0 followers) fill the last two",
                setOf("alice", "bob"), new HashSet<>(influencers.subList(2, 4)));
    }

    // Extra: a user who only appears as a followed name is still listed
    @Test
    public void testInfluencersUserOnlyInValues() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", setOf("bob"));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(Arrays.asList("bob", "alice"), influencers);
    }

    // Extra: no duplicates in the output
    @Test
    public void testInfluencersNoDuplicates() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", setOf("bob", "carol"));
        followsGraph.put("bob", setOf("carol"));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(influencers.size(), new HashSet<>(influencers).size());
    }

    /*
     * Warning: all the tests you write here must be runnable against any
     * SocialNetwork class that follows the spec. It will be run against several
     * staff implementations of SocialNetwork, which will be done by overwriting
     * (temporarily) your version of SocialNetwork with the staff's version.
     * DO NOT strengthen the spec of SocialNetwork or its methods.
     * 
     * In particular, your test cases must not call helper methods of your own
     * that you have put in SocialNetwork, because that means you're testing a
     * stronger spec than SocialNetwork says. If you need such helper methods,
     * define them in a different class. If you only need them in this test
     * class, then keep them in this test class.
     */

}