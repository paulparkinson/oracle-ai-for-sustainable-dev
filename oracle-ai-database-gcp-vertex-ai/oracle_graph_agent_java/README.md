# Oracle Graph Agent for Gemini Enterprise

This is the Java/Spring Boot A2A agent for graph and dependency workflows.

The current Java implementation uses a deterministic A2A executor that calls the local graph tool directly and returns a rendered `image/png` artifact plus a short text summary.

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

## What Success Looks Like

When `./test.sh` succeeds, discovery should show:

- `name: oracle_graph_agent`
- `defaultOutputModes: ["image/png", "text/plain"]`
- `url: http://localhost:8081`

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
- `test.sh` reads the same `.env` and targets `GRAPH_AGENT_URL` or a URL derived from `PUBLIC_HOST`, `PUBLIC_PROTOCOL`, and `GRAPH_AGENT_PORT`.
- The JSON-RPC test uses the standard A2A `message/send` method at the root `/` endpoint and summarizes returned artifacts without printing the embedded base64 PNG.
- The current response model is: PNG artifact first, text summary second.
- `GraphTools.getSupplyChainDependencies()` is still the right place to swap in a real Oracle Graph query.
