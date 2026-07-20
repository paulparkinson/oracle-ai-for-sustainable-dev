# Account-risk MCP App

This optional dashboard is a separate MCP App. It does not connect directly to Oracle Database. The MCP host passes structured tool data to its sandboxed `ui://oracle-account-risk/dashboard-v1` resource using the MCP Apps bridge.

Requires Node.js 20+:

```bash
npm install
npm run typecheck
npm run build
npm run serve
```

Connect an MCP Apps-compatible host to `http://127.0.0.1:3001/mcp`, then invoke `show-account-risk-dashboard`. For production, use HTTPS, authentication, an explicit origin policy, and structured data returned by the Oracle-backed tool rather than the development sample in `server.ts`.
