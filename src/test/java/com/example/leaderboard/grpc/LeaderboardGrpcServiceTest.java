package com.example.leaderboard.grpc;

import com.example.leaderboard.service.InMemoryLeaderboardService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeaderboardGrpcServiceTest {
    private Server server;
    private ManagedChannel channel;
    private LeaderboardGrpc.LeaderboardBlockingStub stub;

    @BeforeEach
    void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new LeaderboardGrpcService(new InMemoryLeaderboardService()))
                .build();
        server.start();

        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
        stub = LeaderboardGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    // Checks that repeated idempotency keys do not add points twice.
    @Test
    void sameIdempotencyKeyAddsPointsOnce() {
        stub.updateScore(UpdateScoreRequest.newBuilder()
                .setUserId("alice")
                .setPoints(10)
                .setIdempotencyKey("key-1")
                .build());

        UserScoreReply secondReply = stub.updateScore(UpdateScoreRequest.newBuilder()
                .setUserId("alice")
                .setPoints(5)
                .setIdempotencyKey("key-1")
                .build());

        assertEquals(10, secondReply.getScore());
    }

    // Checks that invalid points are rejected as INVALID_ARGUMENT.
    @Test
    void invalidPointsReturnInvalidArgument() {
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.updateScore(
                UpdateScoreRequest.newBuilder()
                        .setUserId("alice")
                        .setPoints(0)
                        .setIdempotencyKey("key-2")
                        .build()));

        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
    }

    // Checks that an empty idempotency key is rejected as INVALID_ARGUMENT.
    @Test
    void emptyIdempotencyKeyReturnsInvalidArgument() {
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.updateScore(
                UpdateScoreRequest.newBuilder()
                        .setUserId("alice")
                        .setPoints(10)
                        .setIdempotencyKey("")
                        .build()));

        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
    }

    // Checks that missing users are rejected as NOT_FOUND.
    @Test
    void unknownUserReturnsNotFound() {
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.getUserScore(
                UserScoreRequest.newBuilder()
                        .setUserId("nobody")
                        .build()));

        assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
    }

    // Checks that a zero limit is rejected as INVALID_ARGUMENT.
    @Test
    void topScoresWithZeroLimitReturnsInvalidArgument() {
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.getTopScores(
                TopScoresRequest.newBuilder()
                        .setLimit(0)
                        .build()));

        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
    }
}
