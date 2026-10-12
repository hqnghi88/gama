#!/usr/bin/env bash
set -euo pipefail

gamaHome="${GAMA_HOME:-/opt/gama}"
# PaaS providers (Render, Heroku, ...) inject PORT; fall back to GAMA_SOCKET_PORT.
port="${PORT:-${GAMA_SOCKET_PORT:-10000}}"
workspace="${GAMA_WORKSPACE:-/data/workspace}"
memory="${GAMA_MEMORY:-256m}"

mkdir -p "$workspace"

extra=()
if [[ -n "${GAMA_EXTRA_ARGS:-}" ]]; then
    read -r -a extra <<< "$GAMA_EXTRA_ARGS"
fi

echo "Starting GAMA headless socket server on port ${port} (heap ${memory}, workspace ${workspace})"

"${gamaHome}/headless/gama-headless.sh" \
    -m "$memory" \
    -ws "$workspace" \
    -socket "$port" \
    "${extra[@]}" &
child=$!

# Forward termination signals to GAMA so the JVM can shut down cleanly.
trap 'kill -TERM "$child" 2>/dev/null || true' TERM INT
wait "$child"
