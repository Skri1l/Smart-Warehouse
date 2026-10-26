#!/usr/bin/env bash
# The whole shelf pipeline on one Mac or Linux PC:
# emulator or webcam -> backend (Docker) -> Vision (host) -> PostgreSQL (Docker).
# Step-by-step guide: Documentation/PIPELINE_RUN.md
#
#   scripts/pipeline.sh start       everything in one go: up, then the webcam on shelf 3
#   scripts/pipeline.sh start demo  the same with the labelled photos on shelf 2
#   scripts/pipeline.sh up          start Vision, PostgreSQL and the backend, seed shelves
#   scripts/pipeline.sh demo        emulator with the labelled Coca-Cola photos (shelf 2)
#   scripts/pipeline.sh webcam [N]  webcam N photographs shelf 3 (default: WEBCAM_INDEX,
#                                   else the first camera that is not black)
#   scripts/pipeline.sh status      devices, shelf stock and the last recognitions
#   scripts/pipeline.sh mock        no Docker: mock backend + Vision on port 8080
#   scripts/pipeline.sh seed        create the warehouse, shelves and stock again
#   scripts/pipeline.sh logs        follow the backend log
#   scripts/pipeline.sh down        stop everything, keep the database
#   scripts/pipeline.sh reset       stop everything and DELETE the database
#
# Before the emulator starts, the shelf it sends to (warehouse, rack, shelf, device),
# its stock and the Swagger links are printed. Extra arguments of start/demo/webcam
# go to the emulator, e.g. `demo --auto --interval 10`.

set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
BACKEND_URL=${BACKEND_URL:-http://127.0.0.1:8080}
VISION_URL=${VISION_URL:-http://127.0.0.1:8000}
RUN_DIR="$ROOT/.pipeline"
DEVICE_PY="$ROOT/Device/.venv/bin/python"
VISION_PY="$ROOT/Vision/.venv/bin/python"

say() { printf '\033[1m==> %s\033[0m\n' "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

compose() {
    (cd "$ROOT/Backend" &&
        docker compose -f docker-compose.yml -f docker-compose.host-vision.yml "$@")
}

# wait_for URL NAME SECONDS
wait_for() {
    local deadline=$((SECONDS + $3))
    until curl -sf -o /dev/null "$1"; do
        ((SECONDS < deadline)) || die "$2 did not answer at $1 within $3 s"
        sleep 2
    done
}

device_venv() {
    [[ -x $DEVICE_PY ]] && return
    say "Creating Device/.venv (httpx, OpenCV)"
    python3 -m venv "$ROOT/Device/.venv"
    "$DEVICE_PY" -m pip install -q -r "$ROOT/Device/requirements-webcam.txt"
}

vision_venv() {
    [[ -x $VISION_PY ]] && return
    say "Creating Vision/.venv (torch, ultralytics, CLIP: 1-2 GB, a few minutes)"
    python3 -m venv "$ROOT/Vision/.venv"
    "$VISION_PY" -m pip install -q torch torchvision
    "$VISION_PY" -m pip install -q -r "$ROOT/Vision/requirements-docker.txt"
}

start_vision() {
    if curl -sf -o /dev/null "$VISION_URL/docs"; then
        say "Vision already running at $VISION_URL"
        return
    fi
    vision_venv
    mkdir -p "$RUN_DIR"
    say "Starting Vision (first start downloads ~500 MB of weights), log: .pipeline/vision.log"
    (cd "$ROOT/Vision" &&
        nohup "$VISION_PY" -m uvicorn api:app --host 127.0.0.1 --port 8000 \
            >"$RUN_DIR/vision.log" 2>&1 &
        echo $! >"$RUN_DIR/vision.pid")
    wait_for "$VISION_URL/docs" Vision 600
}

stop_vision() {
    [[ -f $RUN_DIR/vision.pid ]] || return 0
    kill "$(cat "$RUN_DIR/vision.pid")" 2>/dev/null || true
    rm -f "$RUN_DIR/vision.pid"
    say "Vision stopped"
}

start_docker() {
    command -v docker >/dev/null || die "docker not found: brew install colima docker docker-compose"
    docker info >/dev/null 2>&1 && return
    command -v colima >/dev/null || die "the Docker daemon is not running; start Docker Desktop or Colima"
    say "Starting Colima"
    colima start --cpu 4 --memory 4 --disk 30
}

backend_port_free() {
    ! curl -s -o /dev/null "$BACKEND_URL" 2>/dev/null
}

# shelf_info SCENARIO [WEBCAM_INDEX]
shelf_info() {
    python3 "$ROOT/scripts/pipeline_info.py" "$ROOT/Device/$1" \
        --base-url "$BACKEND_URL" --vision-url "$VISION_URL" ${2:+--webcam "$2"}
}

find_webcam() {
    if [[ -n ${WEBCAM_INDEX:-} ]]; then
        echo "$WEBCAM_INDEX"
        return
    fi
    say "Looking for a camera that delivers a picture (set WEBCAM_INDEX to skip)" >&2
    "$DEVICE_PY" "$ROOT/scripts/find_webcam.py"
}

cmd_up() {
    start_docker
    [[ -f $ROOT/Backend/.env ]] || cp "$ROOT/Backend/.env.example" "$ROOT/Backend/.env"
    start_vision
    say "Starting PostgreSQL"
    compose up -d --wait postgres
    if ! docker ps --format '{{.Names}}' | grep -qx smart-warehouse-backend &&
        ! backend_port_free; then
        die "port 8080 is taken (a running mock-server?); stop it first"
    fi
    say "Building the backend image (the first build takes a few minutes)"
    compose build -q backend
    say "Starting the backend"
    compose up -d --no-deps backend
    wait_for "$BACKEND_URL/v3/api-docs" backend 300
    cmd_seed
    say "Ready. Swagger UI: $BACKEND_URL/swagger-ui.html"
}

cmd_start() {
    local mode=webcam
    if [[ ${1:-} == demo || ${1:-} == webcam ]]; then
        mode=$1
        shift
    fi
    cmd_up
    "cmd_$mode" "$@"
}

cmd_seed() {
    python3 "$ROOT/scripts/seed_backend.py" --base-url "$BACKEND_URL"
}

cmd_demo() {
    device_venv
    shelf_info scenarios/demo/scenario.toml
    cd "$ROOT/Device"
    exec "$DEVICE_PY" -m shelf_agent emulate --scenario scenarios/demo/scenario.toml \
        --base-url "$BACKEND_URL" "$@"
}

cmd_webcam() {
    device_venv
    local index
    if [[ ${1:-} =~ ^[0-9]+$ ]]; then
        index=$1
        shift
    else
        index=$(find_webcam) || die "no working camera found; pass its number: webcam N"
    fi
    shelf_info scenarios/webcam/scenario.toml "$index"
    say "Webcam $index -> shelf 3. Enter = take a photo, status, q = quit." \
        "Photos: Device/scenarios/webcam/.captures/"
    cd "$ROOT/Device"
    exec "$DEVICE_PY" -m shelf_agent emulate --scenario scenarios/webcam/scenario.toml \
        --webcam "$index" --base-url "$BACKEND_URL" "$@"
}

cmd_mock() {
    backend_port_free || die "port 8080 is taken (the backend?); run scripts/pipeline.sh down first"
    device_venv
    start_vision
    cd "$ROOT/Device"
    exec "$DEVICE_PY" -m shelf_agent mock-server --vision-url "$VISION_URL"
}

cmd_status() {
    curl -sf -o /dev/null "$BACKEND_URL/v3/api-docs" || die "backend not reachable at $BACKEND_URL"
    BACKEND_URL=$BACKEND_URL python3 - <<'PY'
import json, os
from urllib.request import urlopen

base = os.environ["BACKEND_URL"]
get = lambda path: json.load(urlopen(base + path, timeout=10))

print("Devices:")
for d in get("/devices"):
    print(f"  #{d['id']} {d['deviceKey']:<12} shelf {d['shelfId']}  {d['status']:<8} last seen {d['lastSeenAt']}")
print("Shelf stock:")
for shelf in (2, 3):
    for s in get(f"/shelf-stock/shelf/{shelf}"):
        print(f"  shelf {shelf}: quantity {s.get('quantity')} (stock #{s.get('id')})")
results = get("/recognition-results")
print(f"Recognition results: {len(results)} in total, last 5:")
for r in results[-5:]:
    fields = {k: r.get(k) for k in ("id", "imageEventId", "detectedQuantity", "quantityDelta", "status")}
    print("  " + "  ".join(f"{k}={v}" for k, v in fields.items()))
PY
}

cmd_logs() {
    compose logs -f backend
}

cmd_down() {
    compose stop
    stop_vision
}

cmd_reset() {
    read -r -p "Delete the database (all shelves, devices, events)? [y/N] " answer
    [[ $answer == [yY] ]] || exit 0
    compose down -v
    stop_vision
    rm -rf "$ROOT/Device/.outbox"
    say "Database deleted; run scripts/pipeline.sh up"
}

case ${1:-} in
    start | up | seed | demo | webcam | mock | status | logs | down | reset)
        command=$1
        shift
        "cmd_$command" "$@"
        ;;
    *)
        sed -n '2,22p' "$0" | sed 's/^# \{0,1\}//'
        exit 1
        ;;
esac
