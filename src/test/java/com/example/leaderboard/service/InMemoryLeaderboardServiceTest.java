package com.example.leaderboard.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryLeaderboardServiceTest {

    // Checks that repeated idempotency keys do not add points twice.
    @Test
    void sameIdempotencyKeyAddsPointsOnce() {
        InMemoryLeaderboardService service = new InMemoryLeaderboardService();

        service.updateScore("alice", 10, "key-1");
        service.updateScore("alice", 5, "key-1");

        UserScore alice = service.getUserRank("alice");
        assertEquals(10, alice.score());
    }

    // Checks that users with the same score share the same rank.
    @Test
    void tiedScoresShareRank() {
        InMemoryLeaderboardService service = new InMemoryLeaderboardService();

        service.updateScore("alice", 100, "key-1");
        service.updateScore("bob", 100, "key-2");
        service.updateScore("charlie", 50, "key-3");

        UserScore alice = service.getUserRank("alice");
        UserScore bob = service.getUserRank("bob");
        UserScore charlie = service.getUserRank("charlie");

        assertEquals(1, alice.rank());
        assertEquals(1, bob.rank());
        assertEquals(3, charlie.rank());
    }

    // Checks that the top list is sorted by score in descending order.
    @Test
    void topNIsSortedByScoreDescending() {
        InMemoryLeaderboardService service = new InMemoryLeaderboardService();

        service.updateScore("alice", 10, "key-1");
        service.updateScore("bob", 20, "key-2");

        var topScores = service.getTopN(2);

        assertEquals("bob", topScores.get(0).userId());
        assertEquals(1, topScores.get(0).rank());
        assertEquals(20, topScores.get(0).score());

        assertEquals("alice", topScores.get(1).userId());
        assertEquals(2, topScores.get(1).rank());
        assertEquals(10, topScores.get(1).score());
    }
}
