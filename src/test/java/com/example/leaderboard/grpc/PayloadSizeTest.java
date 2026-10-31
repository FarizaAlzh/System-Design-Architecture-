package com.example.leaderboard.grpc;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class PayloadSizeTest {

    // Compares the same data in protobuf and JSON.
    // The idempotency key is left out: in REST it travels in a header, not in the body.
    @Test
    void printPayloadSizes() {
        UpdateScoreRequest request = UpdateScoreRequest.newBuilder()
                .setUserId("alice")
                .setPoints(10)
                .build();
        String json = "{\"user_id\":\"alice\",\"points\":10}";

        System.out.println("PROTOBUF_BYTES=" + request.getSerializedSize());
        System.out.println("JSON_BYTES=" + json.getBytes(StandardCharsets.UTF_8).length);
    }
}
