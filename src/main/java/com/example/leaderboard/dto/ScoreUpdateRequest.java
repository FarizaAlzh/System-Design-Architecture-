package com.example.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScoreUpdateRequest(
        @JsonProperty("user_id") String userId,
        Integer points
) {
}
