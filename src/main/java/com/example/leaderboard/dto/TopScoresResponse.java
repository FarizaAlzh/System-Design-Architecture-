package com.example.leaderboard.dto;

import java.util.List;

public record TopScoresResponse(List<LeaderboardEntry> data) {
}
