package com.example.leaderboard.grpc;

import com.example.leaderboard.exception.UserNotFoundException;
import com.example.leaderboard.service.LeaderboardService;
import com.example.leaderboard.service.UserScore;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.List;

public class LeaderboardGrpcService extends LeaderboardGrpc.LeaderboardImplBase {
    private final LeaderboardService leaderboardService;

    public LeaderboardGrpcService(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @Override
    public void updateScore(UpdateScoreRequest request, StreamObserver<UserScoreReply> responseObserver) {
        try {
            validateUpdateScoreRequest(request);
            UserScore userScore = leaderboardService.updateScore(
                    request.getUserId(),
                    request.getPoints(),
                    request.getIdempotencyKey()
            );

            responseObserver.onNext(UserScoreReply.newBuilder()
                    .setUserId(userScore.userId())
                    .setScore(userScore.score())
                    .setRank(userScore.rank())
                    .build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(exception.getMessage()).asRuntimeException());
        } catch (UserNotFoundException exception) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(exception.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getTopScores(TopScoresRequest request, StreamObserver<TopScoresReply> responseObserver) {
        try {
            if (request.getLimit() < 1) {
                throw new IllegalArgumentException("limit must be at least 1");
            }

            List<UserScore> userScores = leaderboardService.getTopN(request.getLimit());
            TopScoresReply.Builder builder = TopScoresReply.newBuilder();

            for (UserScore userScore : userScores) {
                builder.addData(LeaderboardEntry.newBuilder()
                        .setRank(userScore.rank())
                        .setUserId(userScore.userId())
                        .setScore(userScore.score())
                        .build());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(exception.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getUserScore(UserScoreRequest request, StreamObserver<UserScoreReply> responseObserver) {
        try {
            UserScore userScore = leaderboardService.getUserRank(request.getUserId());
            responseObserver.onNext(UserScoreReply.newBuilder()
                    .setUserId(userScore.userId())
                    .setScore(userScore.score())
                    .setRank(userScore.rank())
                    .build());
            responseObserver.onCompleted();
        } catch (UserNotFoundException exception) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(exception.getMessage()).asRuntimeException());
        }
    }

    private void validateUpdateScoreRequest(UpdateScoreRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("user_id missing or empty");
        }

        if (request.getPoints() <= 0) {
            throw new IllegalArgumentException("points must be positive");
        }

        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotency_key missing or empty");
        }
    }
}
