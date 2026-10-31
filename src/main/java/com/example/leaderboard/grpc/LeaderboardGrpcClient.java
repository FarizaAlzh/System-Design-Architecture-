package com.example.leaderboard.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LeaderboardGrpcClient implements AutoCloseable {
    private static final int DEADLINE_MILLIS = 2000; // Temporary: to be tuned by measurements.
    private static final int MAX_RETRIES = 3;
    private static final int BASE_BACKOFF_MILLIS = 100;
    private static final Random RANDOM = new Random();

    private final ManagedChannel channel;
    private final LeaderboardGrpc.LeaderboardBlockingStub blockingStub;

    public LeaderboardGrpcClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = LeaderboardGrpc.newBlockingStub(channel);
    }

    // updateScore is safe to retry only because the request carries an idempotency key.
    // The caller must send the SAME key on every retry; a new key would add points twice.
    public UserScoreReply updateScore(String userId, int points, String idempotencyKey) {
        UpdateScoreRequest request = UpdateScoreRequest.newBuilder()
                .setUserId(userId)
                .setPoints(points)
                .setIdempotencyKey(idempotencyKey)
                .build();

        return callWithRetry(() -> blockingStub
                .withDeadlineAfter(DEADLINE_MILLIS, TimeUnit.MILLISECONDS)
                .updateScore(request));
    }

    // Read-only call: retrying is safe because it does not change any data.
    public TopScoresReply getTopScores(int limit) {
        TopScoresRequest request = TopScoresRequest.newBuilder()
                .setLimit(limit)
                .build();

        return callWithRetry(() -> blockingStub
                .withDeadlineAfter(DEADLINE_MILLIS, TimeUnit.MILLISECONDS)
                .getTopScores(request));
    }

    // Read-only call: retrying is safe because it does not change any data.
    public UserScoreReply getUserScore(String userId) {
        UserScoreRequest request = UserScoreRequest.newBuilder()
                .setUserId(userId)
                .build();

        return callWithRetry(() -> blockingStub
                .withDeadlineAfter(DEADLINE_MILLIS, TimeUnit.MILLISECONDS)
                .getUserScore(request));
    }

    private <T> T callWithRetry(GrpcCall<T> call) {
        int attempt = 0;

        while (true) {
            try {
                return call.execute();
            } catch (StatusRuntimeException exception) {
                Status.Code code = exception.getStatus().getCode();
                if ((code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) && attempt < MAX_RETRIES) {
                    attempt++;
                    // Exponential backoff with an added random spread up to BASE_BACKOFF_MILLIS.
                    // This is NOT full jitter: the random part is small and does not reach the full exponential value.
                    long backoffWithJitter = (long) (Math.pow(2, attempt) * BASE_BACKOFF_MILLIS)
                            + RANDOM.nextInt(BASE_BACKOFF_MILLIS + 1);
                    sleepQuietly(backoffWithJitter);
                    continue;
                }
                throw exception;
            }
        }
    }

    private void sleepQuietly(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying gRPC call", exception);
        }
    }

    @Override
    public void close() throws Exception {
        channel.shutdownNow();
    }

    @FunctionalInterface
    private interface GrpcCall<T> {
        T execute();
    }
}
