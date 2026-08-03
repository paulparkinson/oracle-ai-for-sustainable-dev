# Supply-chain inventory-transfer MCP App

This dashboard is a separate MCP App that uses the running Java service as its
governed-data and approval adapter. The Java service invokes
`find-stockout-transfer-recommendations` through the Oracle Database MCP Java
Toolkit, binds a short-lived approval handle to the exact returned rows, and
returns Toolkit-labeled data. The sandboxed
`ui://oracle-supply-chain/inventory-exchange-v1` resource receives the rows and
the widget-only handle through the MCP Apps host bridge. It never receives
database credentials or connects directly to Oracle Database.

The MCP server exposes:

- `show-inventory-transfer-dashboard` to the model and app; it is read-only.
- `approve-inventory-transfer` only to the app; it executes one exact reviewed
  recommendation.
- `reject-inventory-transfer-review` only to the app; it invalidates the
  handle without a write.

The model cannot call the approve or reject tools. The person must select a
card, inspect the route and quantity, review the notes, and click the explicit
approval button inside the MCP App.

Requires Node.js 20.19+ or 22.12+:

```bash
./run.sh
```

Keep the Java service running at `http://127.0.0.1:8080`. The MCP App server defaults to that address; set `AGENT_SERVICE_URL` only when it differs.

For a no-account local demonstration, start the pinned official MCP Apps basic host in a third terminal:

```bash
./run-basic-host.sh
```

Open `http://127.0.0.1:8082`, choose `show-inventory-transfer-dashboard`, set the stockout-risk inputs, and call the tool. The official host fetches the `ui://` resource, renders it inside its two-level sandbox, and passes the live Toolkit-governed recommendations to the dashboard.

The wrapper pins the official host source and applies one checked-in local-runtime compatibility patch: it serves the already-built sandbox HTML with a direct file read because Express `sendFile` can return `NotFoundError` for that generated asset in some macOS runtime combinations. The host, sandbox, bridge, and MCP App implementation remain the pinned official code.

ChatGPT can render this same portable MCP App without a separate UI implementation. ChatGPT supports the standard `_meta.ui.resourceUri` metadata and `ui/*` host bridge used here. To demonstrate it:

1. Keep the Java service and `./run.sh` running.
2. Make port 3001 reachable through HTTPS. Use OpenAI Secure MCP Tunnel when available, or an HTTPS development tunnel such as ngrok or Cloudflare Tunnel. ChatGPT cannot connect directly to `127.0.0.1`.
3. In ChatGPT, enable **Developer mode** under **Settings → Security and login**. If the setting is unavailable, the account or workspace administrator must allow it.
4. Open **Settings → Plugins** or `https://chatgpt.com/plugins`, select **+**, and create a developer-mode app using the tunnel URL ending in `/mcp`.
5. Start a new chat, select the app from **+ → More**, and prompt: `Show the inventory transfer dashboard for products with a minimum stockout risk of 70, limited to 10 recommendations.`
6. Confirm that ChatGPT calls `show-inventory-transfer-dashboard`, renders the
   dashboard, and that selecting a recommendation sends its structured source,
   target, SKU, and quantity back to the conversation.
7. Select one recommendation, review the approval notes, and click **Approve
   and execute transfer**. Confirm the audited transfer ID appears in the app
   and conversation. Run a second review and choose **Cancel review** to verify
   that no write occurs.

See OpenAI's [MCP Apps compatibility](https://developers.openai.com/apps-sdk/mcp-apps-in-chatgpt), [Connect from ChatGPT](https://developers.openai.com/apps-sdk/deploy/connect-chatgpt), and [testing](https://developers.openai.com/apps-sdk/deploy/testing) guidance. Developer Mode availability and permissions depend on the account and workspace policy.

Claude web and Claude Desktop remain optional host demonstrations. Expose port 3001 through an HTTPS tunnel and add the resulting `/mcp` URL as a custom connector. Neither ChatGPT nor Claude is required for the local basic-host walkthrough.

To continue the host validation on another machine or account, follow
[`../docs/chatgpt-claude-mcp-app-handoff.md`](../docs/chatgpt-claude-mcp-app-handoff.md)
and complete the checked-in
[`../docs/mcp-app-host-validation-results.md`](../docs/mcp-app-host-validation-results.md)
template. The runbook includes prerequisites, database startup, tunnel choices,
current ChatGPT and Claude navigation, prompts, expected results, screenshot
names, security rules, and a copy/paste prompt for the next ChatGPT session.

Gemini Enterprise does not use this `ui://` application. It uses the sibling
`../gemini-enterprise-a2a/` adapter, which exposes the same Java workflow over
A2A and produces native A2UI v0.8 controls.

For production, use HTTPS, authentication, an explicit origin policy,
service-to-service authorization between the MCP App server and agent service,
durable approval/idempotency storage, and durable user identity rather than
the local loopback trust boundary.

## Cloud Run deployment for ChatGPT

The repository now includes a separate Cloud Run image and deployment script:

```powershell
.\deploy\gcp\deploy-chatgpt-mcp.ps1
```

This service packages the MCP App, Java service, Oracle Database MCP Java
Toolkit, and wallet mount in one container. It uses the same Direct VPC egress
and `paulparkdb_tp` secrets as the Gemini deployment, but it does not modify or
proxy the Gemini A2A service.

The safe default is private and read-only. Set `MCP_WRITES_ENABLED=true` only
behind an MCP-compatible OAuth 2.1 resource server. The deployment script
refuses `-AllowUnauthenticated -EnableWriteActions` because app-only visibility
is host metadata, not an authorization boundary. For a time-bounded
developer-mode rendering test with synthetic data, `-AllowUnauthenticated`
keeps write tools unregistered. Remove public invocation immediately after the
test.

See
[`../docs/chatgpt-cloud-run-deployment.md`](../docs/chatgpt-cloud-run-deployment.md)
for the verified preflight, sizing decision, commands, and access choices.
