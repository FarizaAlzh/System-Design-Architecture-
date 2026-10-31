#!/bin/bash
N=${1:-1000}
RUN=$(date +%s)
mkdir -p docs
REST_OUT=docs/rest-times.txt
GRPC_OUT=docs/grpc-times.txt
TIMEFORMAT=%R

: > "$REST_OUT"
: > "$GRPC_OUT"

for i in $(seq 1 "$N"); do
  curl -s -o /dev/null -w "%{time_total}\n" \
    -X POST http://localhost:8080/v1/scores \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: rest-$RUN-$i" \
    -d "{\"user_id\":\"rest-$RUN-$i\",\"points\":1}" >> "$REST_OUT"
done

for i in $(seq 1 "$N"); do
  t=$( { time grpcurl -plaintext -import-path src/main/proto -proto leaderboard.proto \
      -d "{\"user_id\":\"grpc-$RUN-$i\",\"points\":1,\"idempotency_key\":\"grpc-$RUN-$i\"}" \
      localhost:9090 leaderboard.v1.Leaderboard/UpdateScore >/dev/null 2>&1; } 2>&1 )
  echo "$t" >> "$GRPC_OUT"
done

for f in "$REST_OUT" "$GRPC_OUT"; do
  sort -n "$f" | awk -v name="$f" '{a[NR]=$1*1000} END{printf "%s: n=%d p50=%.2f ms p99=%.2f ms\n", name, NR, a[int(NR*0.5)], a[int(NR*0.99)]}'
done
