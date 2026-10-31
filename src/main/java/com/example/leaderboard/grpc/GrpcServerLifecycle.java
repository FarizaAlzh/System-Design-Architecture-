package com.example.leaderboard.grpc;

import com.example.leaderboard.service.LeaderboardService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class GrpcServerLifecycle {
    private static final int GRPC_PORT = 9090;

    private final LeaderboardService leaderboardService;
    private Server server;

    public GrpcServerLifecycle(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @PostConstruct
    public void start() throws Exception {
        server = ServerBuilder.forPort(GRPC_PORT)
                .addService(new LeaderboardGrpcService(leaderboardService))
                .build()
                .start();
    }

    @PreDestroy
    public void stopServer() {
        if (server != null) {
            server.shutdown();
        }
    }
}
