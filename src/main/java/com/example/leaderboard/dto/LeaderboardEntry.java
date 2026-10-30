package com.example.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LeaderboardEntry(
        int rank,
        @JsonProperty("user_id") String userId,
        int score
) {
}
