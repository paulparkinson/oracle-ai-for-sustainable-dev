# Implementation plan and compatibility record

Verified on 2026-07-20 against the upstream projects linked below.

## Version decisions

| Layer | Baseline | Decision |
|---|---|---|
| Oracle Database MCP Java Toolkit | `com.oracle.database.mcptoolkit:oracle-db-mcp-toolkit:1.0.0`; JDK 17+, Maven 3.9+ | Build from a sibling checkout of `oracle/mcp`; do not copy the toolkit into this repo. Use Streamable HTTP at `/mcp` for the agent service. |
| MCP SDK used by toolkit | `io.modelcontextprotocol.sdk:mcp:0.12.1` in the current toolkit POM | Treat as an implementation detail of the external toolkit, not an application dependency. |
| AG-UI | Current standardized lifecycle, text, tool-call, state, and `CUSTOM` events | Emit the official event names over SSE. Carry each A2UI envelope in a `CUSTOM` event named `a2ui.message`. This avoids inventing AG-UI event names such as `APPROVAL_REQUIRED`; approval is application state. |
| A2UI | v0.9.1, current production | Emit `createSurface`, `updateComponents`, and `updateDataModel` envelopes using the Basic Catalog ID. The browser renderer accepts only a small catalog allowlist. |
| MCP Apps | Stable 2026-01-26 extension; `@modelcontextprotocol/ext-apps` 1.7.4; `ui://` resource; `text/html;profile=mcp-app` | Keep the dashboard in a separate TypeScript package. It receives structured tool results through the host bridge and never connects to Oracle Database. |

## Important toolkit finding

Current toolkit YAML parameters describe `name`, `type`, `description`, and `required`; they do not describe JDBC/PLSQL OUT-parameter modes. The two read tools fit YAML cleanly. `create-customer-follow-up` must return the generated action ID from an OUT parameter and control commit/rollback, so it needs a narrow Java MCP tool extension (or a future toolkit feature that explicitly supports callable OUT parameters). The project includes the write contract and stored procedure, but does not misrepresent it as working YAML.

## Runtime architecture

```text
web-client
  |  POST run / approval + SSE response
  v
agent-service
  |  MCP Streamable HTTP (business-tool allowlist)
  v
Oracle Database MCP Java Toolkit + narrow write extension
  |  JDBC/UCP + binds + explicit transaction
  v
Oracle AI Database

MCP-compatible host
  |  ui:// resource + postMessage bridge
  v
mcp-app (dashboard; structured data only)
```

## Delivery phases

1. **Foundation (included):** database scripts, deterministic data, narrow read-tool YAML, write-tool contract, runnable dependency-free Java demo adapter, AG-UI SSE, allowlisted A2UI renderer, standalone dashboard preview, tests, and blog.
2. **Live Oracle integration:** build the toolkit from a sibling checkout, implement the narrow write extension using the toolkit's current extension point, configure TLS/OAuth, and run database integration tests.
3. **SDK substitution:** replace the dependency-free browser renderer with the maintained A2UI web renderer if its framework/runtime fits the consuming app. Preserve the same v0.9.1 envelopes.
4. **Production hardening:** external identity, actor claims, persistent approval nonce/idempotency keys, VPD/data-role policies, centralized audit export, and MCP host compatibility testing.

## Compatibility risks

- The Java AG-UI SDK is community-maintained and has moved within the upstream monorepo. The baseline therefore uses the stable wire contract without taking a hard dependency on a volatile Java artifact.
- A2UI v1.0 is a candidate, not the production baseline. Moving to it requires handling `actionResponse`, action IDs, and `surfaceProperties` changes.
- MCP Apps is an MCP extension and host support varies. The base web app works without it.
- YAML alone cannot currently express the required callable OUT parameter. The write extension must be tested against the exact toolkit commit used for deployment.
- Oracle bind support in `FETCH FIRST :maximumRows ROWS ONLY` can vary by execution path. The demo uses `ROWNUM <= :maximumRows` in an outer query.

## Primary sources

- [Oracle Database MCP Java Toolkit](https://github.com/oracle/mcp/tree/main/src/oracle-db-mcp-java-toolkit)
- [AG-UI events](https://docs.ag-ui.com/sdk/js/core/events)
- [A2UI v0.9.1 specification](https://a2ui.org/specification/v0.9.1-a2ui/)
- [MCP Apps overview](https://modelcontextprotocol.io/extensions/apps/overview)
- [MCP Apps build guide](https://modelcontextprotocol.io/extensions/apps/build)
