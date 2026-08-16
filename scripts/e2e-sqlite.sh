#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DB="$ROOT/third-api-admin-server/third-api-sqlite.db"
LOG="/tmp/third-api-admin-e2e.log"

rm -f "$DB"
cd "$ROOT"

export SPRING_DATASOURCE_URL="jdbc:sqlite:$DB"
export SPRING_SQL_INIT_MODE=always

mvn -q -pl third-api-admin-server -am install -DskipTests

mvn -pl third-api-admin-server spring-boot:run >"$LOG" 2>&1 &
ADMIN_PID=$!

cleanup() {
    kill "$ADMIN_PID" 2>/dev/null || true
}
trap cleanup EXIT

for _ in $(seq 1 60); do
    if curl -sf "http://127.0.0.1:8080/api/v1/admin/apps" >/dev/null; then
        break
    fi
    sleep 1
done

E2E_RUN=true E2E_ADMIN_URL=http://127.0.0.1:8080 \
    mvn -pl third-api-spring-boot-starter -Dtest=EndToEndTest test
