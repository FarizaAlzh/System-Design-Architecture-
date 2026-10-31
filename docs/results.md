# Speed test results (1000 requests for each protocol)

| Protocol | Tool | p50 | p99 |
|---|---|---|---|
| REST (port 8080) | curl | 0.88 ms | 1.33 ms |
| gRPC (port 9090) | grpcurl | 14.00 ms | 26.00 ms |

Test conditions:
- Computer: MacBook Air (Apple Silicon)
- The app was started with: mvn spring-boot:run
- Storage: in memory (mock)
- Requests were sent one after another from one client

Limitation:
The gRPC time includes starting the grpcurl program and reading leaderboard.proto
for every request. So the gRPC numbers are higher than the protocol alone.

Message size (same data: user alice, 10 points):
- JSON: 31 bytes
- Protobuf: 9 bytes
