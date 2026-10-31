# System-Design-Architecture
## Game Leaderboard

This is a small service for a game leaderboard. Players get points, and the service shows
the top players and the rank of each player.

I built the same service in two ways:
- REST over HTTP with JSON (port 8080)
- gRPC with a binary format called protobuf (port 9090)

Both ways use the same code inside. Points added with one way are seen with the other way.

## What the service can do

1. Add points to a player
2. Show the top N players
3. Show the rank of one player

## API

### REST (port 8080)

- `POST /v1/scores` adds points. Body: `{"user_id": "mary", "points": 5}`.
  You must send a header `Idempotency-Key` with a unique text.
- `GET /v1/scores?limit=10` shows the top players.
- `GET /v1/scores/{user_id}` shows one player's score and rank.

### gRPC (port 9090)

- `leaderboard.v1.Leaderboard/UpdateScore` adds points.
- `leaderboard.v1.Leaderboard/GetTopScores` shows the top players.
- `leaderboard.v1.Leaderboard/GetUserScore` shows one player's score and rank.

The contract is in the file `src/main/proto/leaderboard.proto`.

## Current results (one run of the app)

These results are from one run of the app. The data is in memory, so the numbers change after a restart.

### Top players (REST, port 8080)

| Rank | User | Score |
|---|---|---|
| 1 | bob | 20 |
| 2 | alice | 15 |
| 3 | carol | 10 |

The same data is returned by gRPC (port 9090). Carol was added through gRPC, and alice through REST. Both ports show both players, so they use one shared storage.

## Idempotency check

Sometimes the network is slow, and the client sends the same request again.
With the same key, the service does not add the points a second time.

| Step | Port | Key | Request | Result |
|---|---|---|---|---|
| First call | REST 8080 | demo-c1 | alice +5 | score 10 → 15 |
| Same key again | REST 8080 | demo-c1 | alice +5 | score stays 15 |
| First call | gRPC 9090 | grpc-c2 | carol +3 | score 7 → 10 |
| Same key again | gRPC 9090 | grpc-c2 | carol +3 | score stays 10 |

The full earlier check is in `docs/idempotency-check.txt`.

## Error checks

| Check | Port | Result |
|---|---|---|
| Request without `Idempotency-Key` header | REST 8080 | 400 Bad Request |
| Unknown user (`nobody`) | REST 8080 | 404 Not Found |
| Unknown user (`nobody`) | gRPC 9090 | NotFound: "User does not exist" |

## Tests

Run all automatic tests with:
mvn clean test


| Test class | What it checks |
|---|---|
| InMemoryLeaderboardServiceTest (3 tests) | Same key adds points once, tied players get the same rank, top list is sorted |
| LeaderboardGrpcServiceTest (5 tests) | Repeated key over gRPC adds points once, bad points give INVALID_ARGUMENT, empty key gives INVALID_ARGUMENT, unknown user gives NOT_FOUND, limit 0 gives INVALID_ARGUMENT |
| PayloadSizeTest (1 test) | Size of the same message in protobuf and JSON |

The last full run passed: 9 tests, 0 failures, 0 errors.

Client retry and timeout logic is not covered by automatic tests. I checked the idempotency through gRPC manually with grpcurl.

## Limits of this version

- The data is stored in memory. When the app stops, all data is lost.
- The service does not use a real database or Redis yet.
- Players are identified only by `user_id`. A display name is not stored.
- To find a rank, the code checks all players. This is slow for many players.
- The keys are never deleted, so the memory grows over time.
- The code uses `synchronized`, so only one change happens at a time.

## Speed test results

I sent 1000 requests with each way. The full results are in `docs/results.md`.

| Way | Tool | Median (p50) | 99% of requests (p99) |
|---|---|---|---|
| REST (8080) | curl | 0.88 ms | 1.33 ms |
| gRPC (9090) | grpcurl | 14.00 ms | 26.00 ms |

The gRPC numbers are higher, but this is not only because of gRPC.
The tool `grpcurl` starts again for every request and reads the proto file each time.
So the numbers show the whole test, not only the protocol.

Client timeout is set to 2000 ms for now. It is not yet tuned to the 50 ms target.

## Message size

The same data (user alice, 10 points):
- JSON: 31 bytes
- Protobuf: 9 bytes

Protobuf is smaller. The size is only the message body. Headers are not included.

## What I learned

- gRPC messages are smaller than JSON messages
- Measuring is not simple. The way you measure changes the result
- A retry is only safe when the same request has the same key