# Oracle Database MCP Java Toolkit integration

This directory contains only this demo's configuration and contracts. It deliberately does not copy Oracle's toolkit source.

`agent-service/run.sh` invokes `prepare-mcp-toolkit.sh`, which downloads and builds a pinned revision of [oracle/mcp](https://github.com/oracle/mcp) into the ignored `.runtime/` directory. It applies `patches/stdio-tool-registration.patch`, then starts the Toolkit over stdio with this configuration and `-Dtools=account-risk`.

The YAML datasource resolves `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from the Toolkit child-process environment. Credentials are never stored in this file. For a remote deployment, run Streamable HTTP with TLS and OAuth 2.0 as documented by the Toolkit.

Do not use `-Dtools=*` for this demo: it would enable unrestricted `write-query`, table-management, and other broad tools.

The current YAML schema cannot register a callable OUT parameter. The demo avoids a custom Toolkit fork: `reserve-customer-action-id` obtains a sequence value, then `create-customer-follow-up` passes it to the input-only `CREATE_CUSTOMER_FOLLOW_UP_MCP` procedure. That one statement validates, locks, inserts, and updates atomically. Failed or abandoned workflows may leave normal sequence gaps but no partial business record.

At startup, the Java agent verifies both the server identity and the exact five-tool allowlist before serving requests. This lets the same governed tool definitions be reused by other agents and MCP-compatible clients.

### Why the stdio patch exists

The pinned Toolkit revision declares dynamic tool-list change notifications while registering tools immediately after constructing its stdio server. With several startup tools, the underlying SDK can reject a notification before the transport is ready. The one-line patch changes only the stdio declaration from `tools(true)` to `tools(false)`. Tool listing and calls still work; only dynamic list-change notifications are disabled. The HTTP transport is unchanged.

`patches/stdio-tool-registration.patch` disables tool-list-change notifications while the pinned Toolkit registers this fixed YAML toolset before the stdio session exists. The patch does not change tool execution or database behavior.
