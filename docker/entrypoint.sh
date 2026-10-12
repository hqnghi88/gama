#!/usr/bin/env bash
set -euo pipefail

gamaHome="${GAMA_HOME:-/opt/gama}"
# PaaS providers (Render, Heroku, ...) inject PORT; fall back to GAMA_SOCKET_PORT.
pubPort="${PORT:-${GAMA_SOCKET_PORT:-10000}}"
# Port GAMA itself listens on, behind the shim.
gamaPort="${GAMA_INTERNAL_PORT:-10001}"
workspace="${GAMA_WORKSPACE:-/data/workspace}"
memory="${GAMA_MEMORY:-256m}"

mkdir -p "$workspace"

extra=()
if [[ -n "${GAMA_EXTRA_ARGS:-}" ]]; then
    read -r -a extra <<< "$GAMA_EXTRA_ARGS"
fi

shim_pid=""
gama_pid=""

cleanup() {
    trap - TERM INT EXIT
    [[ -n "${gama_pid}" ]] && kill -TERM "${gama_pid}" 2>/dev/null || true
    [[ -n "${shim_pid}" ]] && kill -TERM "${shim_pid}" 2>/dev/null || true
    wait 2>/dev/null || true
}
trap cleanup TERM INT EXIT

# GAMA only opens its socket after the whole platform has initialised, which can
# take minutes on a small host. PaaS port scanners (Render) give up after a short
# timeout if nothing is listening on $PORT, so bind it right away and forward the
# bytes to GAMA's real socket. Render's TCP port check then passes immediately and
# WebSocket traffic is proxied through once GAMA is ready.
socat "TCP-LISTEN:${pubPort},fork,reuseaddr" "TCP:127.0.0.1:${gamaPort}" &
shim_pid=$!

if ! kill -0 "${shim_pid}" 2>/dev/null; then
    echo "Failed to start port shim on ${pubPort}" >&2
    exit 1
fi
echo "Port shim listening on 0.0.0.0:${pubPort} -> GAMA on 127.0.0.1:${gamaPort}"

echo "Starting GAMA headless socket server on port ${gamaPort} (heap ${memory}, workspace ${workspace})"

"${gamaHome}/headless/gama-headless.sh" \
    -m "$memory" \
    -ws "$workspace" \
    -socket "$gamaPort" \
    "${extra[@]}" &
gama_pid=$!

wait "${gama_pid}"
