# Account-risk MCP App

This dashboard is a separate MCP App that uses the running Java service as its governed-data adapter. The Java service invokes `find-at-risk-customers` through the Oracle Database MCP Java Toolkit; the MCP App server validates that Toolkit-labeled response and returns it as structured tool content. The sandboxed `ui://oracle-account-risk/dashboard-v1` resource receives those rows through the MCP Apps host bridge. It never receives database credentials or connects directly to Oracle Database.

Requires Node.js 20+:

```bash
npm install
npm run typecheck
npm run build
npm run serve
```

Keep the Java service running at `http://127.0.0.1:8080`. The MCP App server defaults to that address; set `AGENT_SERVICE_URL` only when it differs.

For a no-account local demonstration, start the pinned official MCP Apps basic host in a third terminal:

```bash
./run-basic-host.sh
```

Open `http://127.0.0.1:8082`, choose `show-account-risk-dashboard`, set the risk inputs, and call the tool. The official host fetches the `ui://` resource, renders it inside its two-level sandbox, and passes the live Toolkit-governed result to the dashboard.

Claude web and Claude Desktop are an optional polished host demonstration. Expose port 3001 through an HTTPS tunnel and add the resulting `/mcp` URL as a custom connector. This is not required for the local basic-host walkthrough.

For production, use HTTPS, authentication, an explicit origin policy, service-to-service authorization between the MCP App server and agent service, and durable user identity rather than the local loopback trust boundary.
