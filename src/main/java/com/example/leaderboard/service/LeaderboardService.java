package com.example.leaderboard.service;

import java.util.List;

public interface LeaderboardService {
    UserScore updateScore(String userId, int points, String idempotencyKey);

    List<UserScore> getTopN(int limit);

    UserScore getUserRank(String userId);
}
