# Oracle Spatial Agent for Gemini Enterprise

This is a Python A2A agent for Oracle Spatial-style warehouse map workflows.
The current implementation is deterministic and returns a rendered PNG map artifact.

For this specific agent, the important detail is:

- `test.sh` and `test.py` are callers that send A2A messages to the agent.
- `main.py` runs the A2A service and returns a `image/png` artifact.
- `fetch_warehouse_map_data(...)` is the local placeholder for the Oracle Spatial lookup you can swap to `oracledb`.

## Setup Instructions

1. **Install Dependencies:**
   ```bash
   ./setup_venv.sh
   ```

2. **Python Version:**
   Use Python 3.10 or newer. The current `google-adk` releases require Python `>=3.10`.

3. **Enter the Project Environment:**
   ```bash
   ./enter_venv.sh
   ```

4. **Run the Agent:**
   ```bash
   ./run.sh
   ```

5. **Test the A2A Endpoint:**
   ```bash
   ./test.sh
   ```

6. **Configure the Shared Repo `.env`:**
   The repo root now has a shared `.env` file at `../.env`.
   The shell helpers and Python entrypoints load it automatically.

   Shared local runtime settings:
   ```bash
   PUBLIC_PROTOCOL="http"
   PUBLIC_HOST="localhost"
   SPATIAL_AGENT_PORT="8080"
   ```

## What Success Looks Like

When `./test.sh` succeeds, discovery should show:

- `name: oracle_spatial_agent`
- `defaultOutputModes: ["image/png", "text/plain"]`
- `url: http://localhost:8080`

The action response should show:

- `status: completed`
- one artifact named `warehouse_map_png`
- one file part with `mimeType: image/png`
- one text status message like `Generated a PNG warehouse map for WH-101, WH-202.`

Example summarized action output:

```json
{
  "status": "completed",
  "artifacts": [
    {
      "name": "warehouse_map_png",
      "parts": [
        {
          "kind": "file",
          "mimeType": "image/png",
          "name": "warehouse-map.png"
        }
      ]
    }
  ],
  "statusMessageParts": [
    {
      "kind": "text",
      "text": "Generated a PNG warehouse map for WH-101, WH-202."
    }
  ]
}
```

## Moving This Directory

If you move this agent under another repo:

- keep the shared `.env` one directory above this folder, or update the scripts and `main.py`
- keep `SPATIAL_AGENT_PORT` defined in that new parent `.env`
- rerun `./setup_venv.sh` after the move so the local `.venv` is recreated in the new location

## Notes

- The agent advertises `image/png` and `text/plain` output modes.
- The final A2A artifact is a PNG; the final status message is a short text caption.
- The app advertises its A2A URL from `PUBLIC_HOST`, `PUBLIC_PROTOCOL`, and `SPATIAL_AGENT_PORT`. By default that is `http://localhost:8080`.
- `enter_venv.sh`, `setup_venv.sh`, `run.sh`, `test.sh`, `main.py`, and `test.py` all load the shared repo `.env`.
- `fetch_warehouse_map_data(...)` is where you can replace the mock warehouse coordinates with a real Oracle Spatial query.
