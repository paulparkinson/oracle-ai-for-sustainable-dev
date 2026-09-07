#!/usr/bin/env bash
set -euo pipefail

# Reuse only runtime database settings from the existing, ignored VM environment.
set -a
source /opt/oracle-gemini-supply-chain-a2a/.env
set +a

exec /usr/bin/java -jar /opt/ai-fullstack-spatial-a2a/ai-fullstack-spatial-a2a.jar \
  --server.address=0.0.0.0 \
  --server.port=8444 \
  --server.ssl.enabled=true \
  --server.ssl.bundle=spatial \
  --spring.ssl.bundle.pem.spatial.reload-on-update=true \
  --spring.ssl.bundle.pem.spatial.keystore.certificate=file:/etc/letsencrypt/live/oracle-graph-agent-ip/fullchain.pem \
  --spring.ssl.bundle.pem.spatial.keystore.private-key=file:/etc/letsencrypt/live/oracle-graph-agent-ip/privkey.pem \
  --PUBLIC_A2A_URL=https://34.48.146.146:8444
