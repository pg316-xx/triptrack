#!/bin/sh
# Run on the APP server during a test. Every 5 s it appends container CPU/RAM
# and the number of active Postgres queries to monitor-<time>.csv.
#   sh loadtest/monitor.sh        (Ctrl+C to stop)
OUT="monitor-$(date +%H%M%S).csv"
echo "time,container,cpu,mem,pg_active,pg_total" > "$OUT"
echo "writing $OUT"
while true; do
  T=$(date +%H:%M:%S)
  PG=$(docker compose exec -T postgres psql -U "${DB_USERNAME:-tripnext}" -d tripnext -Atc \
    "select count(*) filter (where state='active'), count(*) from pg_stat_activity where datname='tripnext'" 2>/dev/null | tr '|' ',')
  docker stats --no-stream --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}}' | while IFS= read -r line; do
    echo "$T,$line,${PG:-,}" >> "$OUT"
  done
  tail -n 4 "$OUT"
  sleep 5
done
