# Oracle Java Agent Runtime for Gemini Enterprise

This directory now holds the shared Java/Spring Boot A2A runtime for the Oracle demo's Java-served agent experiences.

Today, the live implementation is still graph-first: it uses a deterministic A2A executor that calls the local graph tool directly and returns a rendered `image/png` artifact plus a short text summary. The same process can also serve multiple public agent cards so we can experiment with separate Gemini Enterprise imports before we split the runtime further.

## Related Files

- [`sql/supply_chain_graph_model.sql`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/sql/supply_chain_graph_model.sql): example Oracle tables, property-graph definition, and query pattern for replacing the current seeded demo data with real database results.
- [`sql/setup_supply_chain_graph_schema.sql`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/sql/setup_supply_chain_graph_schema.sql): idempotent setup DDL for creating the graph demo tables and property graph in Oracle Database.
- [`sql/run_supply_chain_graph_setup.sh`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/sql/run_supply_chain_graph_setup.sh): SQLcl-based wrapper that logs precheck, setup, and postcheck output into a timestamped `sql/logs/` run directory.
- [`sql/seed_supply_chain_graph_data.sql`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/sql/seed_supply_chain_graph_data.sql): idempotent sample data seed for three supply-chain paths, including `SKU-500`.
- [`sql/run_supply_chain_graph_seed.sh`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/sql/run_supply_chain_graph_seed.sh): SQLcl-based wrapper that logs row counts and runs verification queries after seeding.
- [`GRAPH_DATA_MODES.md`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/GRAPH_DATA_MODES.md): how `GRAPH_DATA_MODE=database|payload|auto` works, the supported JSON contract, and the validation rules for multi-agent flows.
- [`MULTI_AGENT_GRAPH_FLOW.md`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/MULTI_AGENT_GRAPH_FLOW.md): architecture notes for direct DB lookup vs upstream-agent payload handoff, including provenance, validation, and recommended `auto` behavior.
- [`HTTPS_SETUP.md`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/HTTPS_SETUP.md): step-by-step Let's Encrypt and public HTTPS setup for Gemini Enterprise.
- [`agent-card-graph.json`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/agent-card-graph.json): saved snapshot of the primary graph card.
- [`agent-card-spatial.json`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/agent-card-spatial.json): saved snapshot of the spatial alias card served by the same Java process.
- [`agent-card-action.json`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/agent-card-action.json): saved snapshot of the inventory-action coordinator card.

## Setup Instructions

1. **Build the Agent:**
   ```bash
   ./build.sh
   ```

2. **Run the Agent:**
   ```bash
   ./run.sh
   ```

3. **Test the A2A Endpoint:**
   ```bash
   ./test.sh
   ```

4. **Run with Public HTTPS for Gemini Enterprise:**
   Gemini Enterprise rejects `http://` agent URLs and expects `https://`.

   For a VM that only has a public IP address, use a publicly trusted IP certificate rather
   than a self-signed certificate:
   ```bash
   CERTBOT_EMAIL="you@example.com" PUBLIC_HOST="34.48.146.146" ./issue_ip_certificate.sh
   ./sync_ip_certificate.sh
   PUBLIC_HOST="34.48.146.146" GRAPH_AGENT_PORT="8080" ./run_public_https.sh
   GRAPH_AGENT_URL="https://34.48.146.146:8080" ./test.sh
   ```

   If Gemini Enterprise ignores the non-standard `:8080` port for your import path, run the
   same agent on standard HTTPS instead:
   ```bash
   sudo env \
     PUBLIC_HOST="34.48.146.146" \
     PUBLIC_PROTOCOL="https" \
     GRAPH_AGENT_URL="https://34.48.146.146" \
     GRAPH_AGENT_PORT="443" \
     BIND_HOST="0.0.0.0" \
     SSL_CERTIFICATE="/etc/letsencrypt/live/oracle-graph-agent-ip/fullchain.pem" \
     SSL_CERTIFICATE_PRIVATE_KEY="/etc/letsencrypt/live/oracle-graph-agent-ip/privkey.pem" \
     ./run.sh
   ```

   `issue_ip_certificate.sh` expects:
   - Certbot `5.3.0` or newer
   - inbound TCP `80` open during issuance and renewal
   - nothing else listening on port `80` while Certbot runs

   `sync_ip_certificate.sh` copies the root-owned Let's Encrypt files into a user-readable
   private directory so the non-root Java process can use HTTPS safely.

   Current tested import URLs:
   - standard HTTPS: `https://34.48.146.146/.well-known/agent-card.json`
   - graph alias card on the same Java process: `https://34.48.146.146/agent-card-graph.json`
   - spatial alias card on the same Java process: `https://34.48.146.146/agent-card-spatial.json`
   - inventory-action card on the same Java process: `https://34.48.146.146/agent-card-action.json`
   - dedicated inventory-action A2A card path: `https://34.48.146.146/inventory-action/.well-known/agent-card.json`
   - direct HTTPS on 8080: `https://34.48.146.146:8080/.well-known/agent-card.json`

   The primary `/.well-known/agent-card.json` endpoint is still the graph card. The spatial card is currently an import alias for Gemini Enterprise experiments, not a separate spatial implementation yet. The inventory-action card is a real additional agent surface in the same Spring Boot process, served at `/inventory-action`.

   On the GCP VM, a reliable way to keep the `443` deployment alive after SSH exits is to
   start it as a transient `systemd` service instead of a background shell job:
   ```bash
   sudo systemd-run \
     --unit=oracle-graph-agent \
     --description="Oracle Graph Agent HTTPS service" \
     --working-directory="$PWD" \
     --setenv=PUBLIC_HOST="34.48.146.146" \
     --setenv=PUBLIC_PROTOCOL="https" \
     --setenv=GRAPH_AGENT_URL="https://34.48.146.146" \
     --setenv=GRAPH_AGENT_PORT="443" \
     --setenv=BIND_HOST="0.0.0.0" \
     --setenv=SSL_CERTIFICATE="/etc/letsencrypt/live/oracle-graph-agent-ip/fullchain.pem" \
     --setenv=SSL_CERTIFICATE_PRIVATE_KEY="/etc/letsencrypt/live/oracle-graph-agent-ip/privkey.pem" \
     "$PWD/run.sh"
   sudo systemctl status oracle-graph-agent.service
   ```

## Gemini Enterprise Payload Prompt

If another agent or workflow already has graph data, keep `GRAPH_DATA_MODE=auto` or `GRAPH_DATA_MODE=payload`
and send the graph as structured JSON in the chat prompt. The parser accepts:

- a raw JSON object as the whole prompt
- a fenced JSON block inside a normal natural-language prompt
- a normal natural-language prompt followed by a raw JSON object pasted below it, even without code fences
- markdown-style escaped brackets such as `\[` and `\]` if the Gemini UI inserts them during paste
- either the payload root itself or `{ "graphPayload": { ... } }`

Recommended Gemini Enterprise prompt:

````text
Render this supply-chain dependency graph exactly from the structured payload below.
Do not query the database.
Use the payload as the authoritative graph input.

```json
{
  "graphPayload": {
    "schemaVersion": "1.0",
    "productId": "SKU-777",
    "nodes": [
      {
        "id": "supplier",
        "type": "SUPPLIER",
        "label": "Supplier: Blue Ocean Resins",
        "detail": "Tier 2 | Singapore",
        "metric": "On-time 97%"
      },
      {
        "id": "plant",
        "type": "PLANT",
        "label": "Plant: Austin Assembly",
        "detail": "Cycle 3.2 days",
        "metric": "Utilization 81%"
      },
      {
        "id": "port",
        "type": "PORT",
        "label": "Port: Long Beach",
        "detail": "ETA 22 hrs",
        "metric": "Delay risk 0.18"
      },
      {
        "id": "warehouse",
        "type": "WAREHOUSE",
        "label": "Warehouse: Reno DC",
        "detail": "Inventory 8120 units",
        "metric": "Fill rate 98%"
      },
      {
        "id": "product",
        "type": "PRODUCT",
        "label": "Product: SKU-777",
        "detail": "Demand +12%",
        "metric": "Margin 34%"
      },
      {
        "id": "alert",
        "type": "ALERT",
        "label": "Alert: Customs Review",
        "detail": "West coast lane",
        "metric": "Risk 0.27"
      }
    ],
    "edges": [
      { "from": "supplier", "to": "plant", "label": "SUPPLIES" },
      { "from": "plant", "to": "port", "label": "SHIPS_VIA" },
      { "from": "port", "to": "warehouse", "label": "ROUTES_TO" },
      { "from": "warehouse", "to": "product", "label": "STOCKS" },
      { "from": "alert", "to": "port", "label": "AFFECTS" }
    ]
  }
}
```
````

If Gemini Enterprise is rewriting surrounding text, you can also send only the JSON object above.

Expected success signal:

- the response text mentions the product and graph path
- the returned PNG artifact renders the exact nodes and edges you supplied
- the metadata reports `sourceMode=payload`

## Inventory Action Coordinator

The same Java process now also serves an ADK-backed inventory-action coordinator at:

- card alias: `https://34.48.146.146/agent-card-action.json`
- dedicated card path: `https://34.48.146.146/inventory-action/.well-known/agent-card.json`
- JSON-RPC endpoint: `https://34.48.146.146/inventory-action`

This coordinator is the first cut of the final-stage action flow described in the root repo README:

- it uses Google ADK Java inside the existing Spring Boot process
- it runs a `ParallelAgent` for graph, spatial, and external evidence gathering
- it follows that with an `LlmAgent` decision step that checks policy and drafts a transfer recommendation
- it does not execute the move; it only recommends and drafts, with approval handling called out in the response

Current tool coverage inside the coordinator:

- `getGraphEvidence`: calls the Oracle graph tool directly and summarizes the dependency path
- `getSpatialEvidence`: returns the current seeded hotspot view and a suggested transfer direction
- `getExternalSignals`: returns seeded weather or lane-risk context
- `checkTransferPolicy`: decides whether approval is required
- `draftInventoryTransferAction`: creates a draft, not an execution

Recommended Gemini Enterprise prompt:

```text
What inventory action should we take for SKU-500 given the current supply risk? Gather graph, spatial, and external evidence first, then recommend the safest next move and say whether approval is required.
```

More explicit transfer-oriented prompt:

```text
For SKU-500, gather the supporting graph, spatial, and external evidence, then tell me whether we should shift inventory between warehouses. If a transfer is appropriate, draft the move but do not execute it.
```

Local test script:

```bash
./test_inventory_action.sh
./test_inventory_action.sh "What inventory action should we take for SKU-500 given the current supply risk?"
GRAPH_AGENT_URL="https://34.48.146.146" ./test_inventory_action.sh
```

Expected response shape:

- evidence-backed recommendation in plain text
- explicit approval status
- if a transfer was drafted, the source warehouse, destination warehouse, units, and draft action id

## What Success Looks Like

When `./test.sh` succeeds, discovery should show:

- `name: oracle_graph_agent`
- `defaultOutputModes: ["image/png", "text/plain"]`
- `url: http://localhost:8081` for local dev, or your public `https://` URL when deployed

The action response should show:

- `status: completed`
- one artifact named `supply_chain_graph_png`
- one file part with `mimeType: image/png`
- one text status message summarizing the dependency chain

Example summarized action output:

```json
{
  "status": "completed",
  "artifacts": [
    {
      "name": "supply_chain_graph_png",
      "parts": [
        {
          "kind": "file",
          "mimeType": "image/png",
          "name": "supply-chain-graph.png"
        }
      ]
    }
  ],
  "statusMessageParts": [
    {
      "kind": "text",
      "text": "Dependencies for SKU-500: Supplier: Global Logistics -> Product: SKU-500 | relationships: SUPPLIES"
    }
  ]
}
```

## Moving This Directory

If you move this agent under another repo:

- keep the shared `.env` one directory above this folder, or update `run.sh` and `test.sh`
- keep `GRAPH_AGENT_PORT` defined in that new parent `.env`
- rebuild after the move with `./build.sh`

## Notes

- `run.sh` reads the shared repo `.env` and starts the Spring Boot app on `GRAPH_AGENT_PORT`.
- If `SSL_CERTIFICATE` and `SSL_CERTIFICATE_PRIVATE_KEY` are set, `run.sh` starts the same Spring Boot app with HTTPS enabled.
- `test.sh` reads the same `.env` and targets `GRAPH_AGENT_URL` or a URL derived from `PUBLIC_HOST`, `PUBLIC_PROTOCOL`, and `GRAPH_AGENT_PORT`.
- `test.sh` now persists returned file artifacts under `test-output/` and prints the saved file path.
- The JSON-RPC test uses the standard A2A `message/send` method at the root `/` endpoint and summarizes returned artifacts without printing the embedded base64 PNG.
- The current response model is: PNG artifact first, text summary second.
- The graph agent now supports `GRAPH_DATA_MODE=database|payload|auto`. In `database` mode it queries the seeded Oracle Property Graph directly; in `payload` mode it renders a validated upstream JSON payload; in `auto` mode it prefers payload and falls back to the database.
- `GraphTools.getSupplyChainDependencies()` now owns both the Oracle query path and the validated payload path.
- The inventory-action coordinator uses Google ADK Java in-process. It currently mixes one live Oracle graph tool with seeded spatial and external evidence tools while we keep the overall demo in a single convenient JVM.
- For wallet-backed Oracle Database access, the Maven config now imports Oracle's `ojdbc-bom`, pins Spring Boot's managed Oracle version to `23.3.0.23.09`, and includes `oraclepki` alongside `ojdbc11`. Older 19c/21c examples in this repo use `osdt_core` and `osdt_cert`, but Oracle's 23ai guidance says wallet support on 23ai only requires `oraclepki`, and the `23.3.0.23.09` `osdt_*` artifacts are not published on Maven Central.
- Self-signed certificates are a poor fit for Gemini Enterprise because the Google-managed caller must trust the certificate chain. Use a publicly trusted certificate instead.
