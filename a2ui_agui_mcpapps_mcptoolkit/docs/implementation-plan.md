# Implementation plan and compatibility record

Verified on 2026-07-20 against the upstream projects linked below.

## Version decisions

| Layer | Baseline | Decision |
|---|---|---|
| Oracle Database MCP Java Toolkit | `com.oracle.database.mcptoolkit:oracle-db-mcp-toolkit:1.0.0`; pinned upstream commit `5bb406b5b70e109a749cd16cc026422134a117b2`; JDK 17+, Maven 3.9+ | Build the official source into ignored `.runtime/`, apply the one-line stdio registration patch, and launch it with only `account-risk`. Use Streamable HTTP with TLS/OAuth for a remote deployment. |
| MCP protocol client | MCP `2025-03-26`, matching the Toolkit’s `io.modelcontextprotocol.sdk:mcp:0.12.1` baseline | Keep the agent adapter small and dependency-light: newline-delimited JSON-RPC over stdio, initialization, tool discovery, tool calls, 30-second timeouts, server identity checks, and an exact allowlist. |
| AG-UI | Current standardized lifecycle, text, tool-call, state, and `CUSTOM` events | Emit the official event names over SSE. Carry each A2UI envelope in a `CUSTOM` event named `a2ui.message`. This avoids inventing AG-UI event names such as `APPROVAL_REQUIRED`; approval is application state. |
| A2UI | v0.9.1, current production | Emit `createSurface`, `updateComponents`, and `updateDataModel` envelopes using the Basic Catalog ID. The browser renderer accepts only a small catalog allowlist. |
| MCP Apps | Stable 2026-01-26 extension; `@modelcontextprotocol/ext-apps` 1.7.4; `ui://` resource; `text/html;profile=mcp-app` | Keep the dashboard in a separate TypeScript package. It receives structured tool results through the host bridge and never connects to Oracle Database. |

## Toolkit write compatibility

Current Toolkit YAML parameters describe `name`, `type`, `description`, and `required`; they do not describe JDBC/PLSQL OUT-parameter modes. The live integration avoids a Toolkit fork by reserving an ID with `CUSTOMER_ACTION_MCP_SEQ`, then passing that ID into the input-only `CREATE_CUSTOMER_FOLLOW_UP_MCP` procedure. The procedure performs validation, row locking, insert, and status update in one MCP tool statement. Failure creates no partial business record; sequence gaps remain valid Oracle behavior.

## Runtime architecture

```text
web-client
  |  POST run / approval + SSE response
  v
agent-service
  |  MCP 2025-03-26 over stdio, exact tool allowlist
  v
Oracle Database MCP Java Toolkit
  |  YAML business tools + JDBC/UCP + binds
  v
Oracle AI Database

MCP-compatible host
  |  ui:// resource + postMessage bridge
  v
mcp-app (dashboard; structured data only)
```

## Delivery phases

1. **Foundation (included):** database scripts, deterministic data, tool YAML, AG-UI SSE, allowlisted A2UI renderer, standalone dashboard preview, tests, and blog.
2. **Oracle schema bootstrap (included):** guarded one-time UCP setup, financial database defaults, views, sequence, procedures, and deterministic sample records. This installer is separate from application runtime access.
3. **MCP integration (included):** pinned official Toolkit bootstrap, synchronous stdio client, exact allowlist verification, YAML reads, and a sequence-backed input-only procedure write. Every application database operation uses this path.
4. **SDK substitution:** replace the dependency-free browser renderer with the maintained A2UI web renderer if its framework/runtime fits the consuming app. Preserve the same v0.9.1 envelopes.
5. **Production hardening:** external identity, actor claims, persistent approval nonce/idempotency keys, VPD/data-role policies, centralized audit export, and MCP host compatibility testing.

## Compatibility risks

- The Java AG-UI SDK is community-maintained and has moved within the upstream monorepo. The baseline therefore uses the stable wire contract without taking a hard dependency on a volatile Java artifact.
- A2UI v1.0 is a candidate, not the production baseline. Moving to it requires handling `actionResponse`, action IDs, and `surfaceProperties` changes.
- MCP Apps is an MCP extension and host support varies. The base web app works without it.
- YAML cannot currently express a callable OUT parameter. The sequence-plus-input-only-procedure design avoids that requirement, but sequence gaps are expected and the exact Toolkit commit remains pinned and tested.
- The pinned Toolkit/SDK combination can emit tool-list change notifications before stdio is ready when several tools register at startup. The checked-in patch disables only those dynamic stdio notifications; static discovery and calls remain enabled, and the agent checks the exact list before serving.
- Oracle bind support in `FETCH FIRST :maximumRows ROWS ONLY` can vary by execution path. The demo uses `ROWNUM <= :maximumRows` in an outer query.

## Primary sources

- [Oracle Database MCP Java Toolkit](https://github.com/oracle/mcp/tree/main/src/oracle-db-mcp-java-toolkit)
- [AG-UI events](https://docs.ag-ui.com/sdk/js/core/events)
- [A2UI v0.9.1 specification](https://a2ui.org/specification/v0.9.1-a2ui/)
- [MCP Apps overview](https://modelcontextprotocol.io/extensions/apps/overview)
- [MCP Apps build guide](https://modelcontextprotocol.io/extensions/apps/build)
