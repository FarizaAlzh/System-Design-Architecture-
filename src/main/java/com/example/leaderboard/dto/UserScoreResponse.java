package com.example.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserScoreResponse(
        @JsonProperty("user_id") String userId,
        int score,
        int rank
) {
}
