package com.example.leaderboard.controller;

import com.example.leaderboard.dto.LeaderboardEntry;
import com.example.leaderboard.dto.ScoreUpdateRequest;
import com.example.leaderboard.dto.TopScoresResponse;
import com.example.leaderboard.dto.UserScoreResponse;
import com.example.leaderboard.exception.UserNotFoundException;
import com.example.leaderboard.service.LeaderboardService;
import com.example.leaderboard.service.UserScore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/v1")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @PostMapping("/scores")
    public ResponseEntity<UserScoreResponse> updateScore(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ScoreUpdateRequest request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header missing");
        }

        if (request == null || request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_id missing or empty");
        }

        if (request.points() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "points missing");
        }

        if (request.points() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "points must be positive");
        }

        UserScore userScore = leaderboardService.updateScore(request.userId(), request.points(), idempotencyKey);
        return ResponseEntity.ok(new UserScoreResponse(userScore.userId(), userScore.score(), userScore.rank()));
    }

    @GetMapping("/scores")
    public ResponseEntity<TopScoresResponse> getTopScores(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        if (limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be at least 1");
        }

        List<UserScore> scores = leaderboardService.getTopN(limit);
        List<LeaderboardEntry> data = scores.stream()
                .map(score -> new LeaderboardEntry(score.rank(), score.userId(), score.score()))
                .toList();

        return ResponseEntity.ok(new TopScoresResponse(data));
    }

    @GetMapping("/scores/{user_id}")
    public ResponseEntity<UserScoreResponse> getUserScore(@PathVariable("user_id") String userId) {
        UserScore userScore = leaderboardService.getUserRank(userId);
        return ResponseEntity.ok(new UserScoreResponse(userScore.userId(), userScore.score(), userScore.rank()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}
