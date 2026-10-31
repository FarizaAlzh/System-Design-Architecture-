package com.example.leaderboard.service;

import com.example.leaderboard.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InMemoryLeaderboardService implements LeaderboardService {
    // The public methods are synchronized, so a plain HashMap and HashSet are safe here.
    // This keeps the in-memory mock simple and easy to explain in class.
    private final Map<String, Integer> scoresByUser = new HashMap<>();
    private final Map<String, UserScore> processedIdempotencyKeys = new HashMap<>();

    @Override
    public synchronized UserScore updateScore(String userId, int points, String idempotencyKey) {
        if (processedIdempotencyKeys.containsKey(idempotencyKey)) {
            return processedIdempotencyKeys.get(idempotencyKey);
        }

        int currentScore = scoresByUser.getOrDefault(userId, 0);
        int updatedScore = currentScore + points;
        scoresByUser.put(userId, updatedScore);

        UserScore userScore = getUserScore(userId);
        processedIdempotencyKeys.put(idempotencyKey, userScore);

        return userScore;
    }

    @Override
    public synchronized List<UserScore> getTopN(int limit) {
        return scoresByUser.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(entry -> new UserScore(entry.getKey(), entry.getValue(), calculateRank(entry.getKey())))
                .toList();
    }

    @Override
    public synchronized UserScore getUserRank(String userId) {
        if (!scoresByUser.containsKey(userId)) {
            throw new UserNotFoundException("User does not exist");
        }
        return getUserScore(userId);
    }

    private UserScore getUserScore(String userId) {
        int score = scoresByUser.getOrDefault(userId, 0);
        return new UserScore(userId, score, calculateRank(userId));
    }

    private int calculateRank(String userId) {
        int userScore = scoresByUser.getOrDefault(userId, 0);
        long higherScoreCount = scoresByUser.values().stream()
                .filter(score -> score > userScore)
                .count();

        // Rank is 1 + the number of users with a strictly higher score.
        // If two users have the same score, they share the same rank.
        return (int) higherScoreCount + 1;
    }
}
