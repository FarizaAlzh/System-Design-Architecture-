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
  It uses the file `src/main/proto/leaderboard.proto`.

## What is the Idempotency-Key?

Sometimes the network is slow, and the client sends the same request again.
With the same key, the service does not add the points a second time.

I tested this. I sent the same request with the key `g-key-1` two times.
The score stayed the same. Then I sent a new key, and the score went up.
The results are in `docs/idempotency-check.txt`.

## Limits of this version

- The data is stored in memory. When the app stops, all data is lost.
- The service does not use a real database or Redis yet.
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

## Message size

The same data (user alice, 10 points):
- JSON: 31 bytes
- Protobuf: 9 bytes

Protobuf is smaller. The size is only the message body. Headers are not included.

## What I learned

- gRPC messages are smaller than JSON messages
- Measuring is not simple. The way you measure changes the result
- A retry is only safe when the same request has the same key

