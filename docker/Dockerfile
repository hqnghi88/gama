# syntax=docker/dockerfile:1

# GAMA headless (socket server) image tuned to run on small hosts (512 MB).
#
# It is built from an official GAMA Linux release archive (the archive already
# contains the `headless/` scripts, the plugins and `Gama.ini`). The memory
# defaults hard-coded in `Gama.ini` (-Xms4G -Xmx16G) are ignored by the bundled
# launcher in favour of the small values below.
#
# Build (run from the repository root):
#   docker build -f docker/Dockerfile -t gama-headless .
#
# Build against another release:
#   docker build -f docker/Dockerfile \
#     --build-arg GAMA_ZIP_URL=https://github.com/gama-platform/gama/releases/download/<ver>/<asset>.zip \
#     -t gama-headless .
#
# Run (limits the container to 512 MB and exposes the socket server on 10000):
#   docker run --rm --memory=512m -p 10000:10000 \
#     -v gama-workspace:/data gama-headless
#
# With more RAM available, raise the heap, e.g.:
#   docker run --rm --memory=2g -e GAMA_MEMORY=1536m gama-headless
#
# On a PaaS (Render, ...) the injected $PORT is used automatically, so the
# socket server listens where the platform expects it.

# Official GAMA release used as the base distribution. Override with
# --build-arg GAMA_ZIP_URL=<url> to pin another version.
ARG GAMA_ZIP_URL=https://github.com/gama-platform/gama/releases/download/2026.09.0/GAMA_2026.09.0_Linux_08.09.26_3ed0306.zip

FROM eclipse-temurin:25-jdk-noble

ARG GAMA_ZIP_URL

ENV DEBIAN_FRONTEND=noninteractive \
    GAMA_HOME=/opt/gama \
    GAMA_SOCKET_PORT=10000 \
    GAMA_WORKSPACE=/data/workspace \
    GAMA_MEMORY=256m \
    GAMA_XMS=64m \
    GAMA_JAVA_OPTS="-XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -XX:+UseSerialGC" \
    GAMA_EXTRA_ARGS=""

RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl unzip bash \
 && rm -rf /var/lib/apt/lists/*

# Download and extract the GAMA distribution. The archive may or may not wrap
# everything in a single top-level directory, so the real root is located by
# looking for the `plugins` folder.
RUN set -eux; \
    mkdir -p "$GAMA_HOME" /tmp/gama; \
    curl -fSL "$GAMA_ZIP_URL" -o /tmp/gama.zip; \
    unzip -q /tmp/gama.zip -d /tmp/gama; \
    pluginsDir="$(find /tmp/gama -maxdepth 2 -type d -name plugins -print -quit)"; \
    if [ -z "$pluginsDir" ]; then echo "Could not find GAMA 'plugins' directory in archive" >&2; exit 1; fi; \
    gamaRoot="$(dirname "$pluginsDir")"; \
    cp -a "$gamaRoot"/. "$GAMA_HOME"/; \
    rm -rf /tmp/gama /tmp/gama.zip

# Replace the launcher shipped in the archive with the memory-friendly one from
# this repository so the image works regardless of the release version.
COPY gama.product/extraresources/headless/unix/gama-headless.sh ${GAMA_HOME}/headless/gama-headless.sh
COPY docker/entrypoint.sh /usr/local/bin/gama-entrypoint.sh

RUN chmod +x "${GAMA_HOME}/headless/gama-headless.sh" /usr/local/bin/gama-entrypoint.sh \
 && useradd --create-home --shell /bin/bash gama \
 && mkdir -p "$GAMA_WORKSPACE" \
 && chown -R gama:gama "$GAMA_HOME" /data

USER gama
WORKDIR ${GAMA_HOME}

VOLUME ["/data"]
EXPOSE 10000

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD bash -c "exec 3<>/dev/tcp/127.0.0.1/${GAMA_SOCKET_PORT}" || exit 1

ENTRYPOINT ["/usr/local/bin/gama-entrypoint.sh"]
