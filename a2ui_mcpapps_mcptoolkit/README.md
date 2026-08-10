# Supply-Chain Inventory Exchange with A2UI, AG-UI, MCP Apps, the Oracle Database MCP Java Toolkit, and Oracle AI Database

This runnable reference application turns a stockout investigation into a governed inventory-transfer workflow while keeping each technology in its proper layer:

- Oracle AI Database computes feasible transfer recommendations from inventory, demand, safety-stock, and route data, then owns the transaction and audit record.
- Oracle Database MCP Java Toolkit exposes five bounded supply-chain tools that can be reused by multiple agents and MCP-compatible clients.
- The Java agent service streams official AG-UI events and carries A2UI v0.9.1 envelopes.
- The browser validates and renders an allowlisted A2UI recommendation-and-approval surface.
- A separate MCP App renders the same Toolkit-backed recommendations inside ChatGPT, Claude, and Gemini Enterprise.
- A Gemini Enterprise adapter also serves the workflow over A2A v0.3 with the A2UI v0.8 messages its native renderer currently requires.

See [`PROJECT_BRIEF.md`](PROJECT_BRIEF.md) for the requirements and [`docs/implementation-plan.md`](docs/implementation-plan.md) for verified versions, compatibility risks, and the implementation design.

## Run through the Oracle Database MCP Java Toolkit

The default runtime uses the same `financialdb_high` database alias and `FINANCIAL` schema as the observability demo. It requires JDK 21, Maven 3.9+, Git, and network access for the first Toolkit build.

```bash
cp .env.example .env
# Edit .env: set the wallet path and DB_PASSWORD.

cd agent-service
./setup-database.sh
./test.sh
./run.sh
```

Open `http://127.0.0.1:8080`, retrieve stockout-risk recommendations, choose a concrete source-to-target transfer, and approve or cancel it. On first start, `run.sh` downloads the pinned official Oracle Toolkit source into ignored `.runtime/`, builds it, and starts it as an MCP stdio child process. `McpToolkitSupplyChainRepository` discovers the exact tool allowlist and invokes every runtime database read and write through MCP. The Toolkit owns Oracle UCP and the runtime database connections.

The ignored `financial/setup/.env` is reused automatically when it is present, so this project does not duplicate the financial password. Its deployment-only JDBC URL is replaced locally with `financialdb_high` plus the configured wallet directory. The one-time setup runner uses UCP only to install the Toolkit's database objects; it is not an application data path. It never drops existing objects, refuses unsafe partial supply-chain state, can resume after the base supply-chain schema has been installed, and becomes a no-op after successful installation. SQLcl users can alternatively run `database/setup.sql`.

The Oracle artifacts are not mocked:

- `database/` creates four locations, five products, twenty inventory positions, twelve transfer lanes, a recommendation view, an audit table, a sequence, and a transaction-controlled stored procedure.
- `oracle-db-mcp-toolkit/config/tools.yaml` defines bounded recommendation reads, transfer-ID reservation, the procedure-backed write, and a verification query.
- `oracle-db-mcp-toolkit/contracts/approve-inventory-transfer.json` records the write tool contract enforced around the YAML tool.
- `mcp-app/` follows the stable MCP Apps `ui://` resource and structured-content pattern.

## Why the Toolkit is the preferred path

The Toolkit is preferred for this reference architecture because its governed tool contracts can be reused by multiple agents and MCP-compatible clients. The application implements that path throughout:

1. `prepare-mcp-toolkit.sh` builds a pinned revision of Oracle's official Toolkit without copying its source into this repository. It applies the checked-in one-line stdio compatibility patch described below.
2. The agent starts it over stdio with `tools.yaml` and only the `supply-chain-exchange` toolset.
3. Credentials pass in the child-process environment, not YAML, browser code, prompts, or Git.
4. The agent verifies the connected server identity and exact five-tool allowlist before accepting traffic.
5. Every application read and approved write executes through MCP. Only the separate one-time schema installer connects through its own UCP configuration.

The Toolkit YAML schema has no callable OUT-parameter mode. The demo therefore reserves a transfer ID with a bounded read tool and supplies it to an input-only stored procedure tool. The procedure locks both inventory positions in deterministic order, recomputes current source surplus and target shortage, rejects a stale or excessive transfer, inserts the audit row, and reserves source inventory in one database statement. A failed statement creates no business record, although Oracle sequences can legitimately contain gaps.

The pinned Toolkit revision advertises tool-list change notifications before its stdio transport can enqueue several startup registrations. `patches/stdio-tool-registration.patch` changes only the stdio capability from `tools(true)` to `tools(false)`. Static tool discovery and invocation remain available; dynamic list-change notifications are disabled for this fixed local configuration. The agent's exact allowlist check fails closed if registration is incomplete.

## Host adapters: one workflow, three presentation contracts

The Java service is the common policy and domain boundary. The hosts do not
share a UI binary or transport:

| Experience | Host contract | UI contract |
|---|---|---|
| Standalone browser | AG-UI events over SSE | allowlisted A2UI v0.9.1 |
| ChatGPT | MCP over Streamable HTTP | MCP Apps `ui://` HTML and host bridge |
| Gemini Enterprise (native) | A2A v0.3 endpoint | native A2UI v0.8 |
| Gemini Enterprise (embedded app) | private MCP over Streamable HTTP | sandboxed MCP Apps `ui://` HTML |

Gemini Enterprise supports two complementary paths. It can render native A2UI
controls supplied by `gemini-enterprise-a2a/`, or connect to the same sandboxed
MCP App used by ChatGPT and Claude through a private Custom MCP Server data
store. Both paths call the same Java APIs and Toolkit tools; neither is a second
business implementation.

## MCP App in ChatGPT, Claude, and Gemini Enterprise

The port-8080 web application demonstrates AG-UI and A2UI; it does not embed the MCP App. The separate `mcp-app/` package demonstrates the MCP Apps extension inside a compatible host. `server.ts` registers the model-visible `show-inventory-transfer-dashboard` tool, the app-only approve and reject tools, and the `ui://oracle-supply-chain/inventory-exchange-v2` resource, while `src/mcp-app.ts` implements the dashboard.

The model-visible MCP App tool calls the Java service's `/api/reviews` adapter.
That adapter invokes `find-stockout-transfer-recommendations`, binds a
short-lived single-use approval handle to the exact returned rows, and puts the
handle in widget-only result metadata. The iframe can call two app-only tools:
`approve-inventory-transfer` and `reject-inventory-transfer-review`. The model
cannot invoke either action tool. The iframe never receives database
credentials or a direct database connection.

With Node.js 20.19+ or 22.12+ installed:

```bash
cd mcp-app
./run.sh
```

Keep that server and the Java service running. In a third terminal, launch the pinned official MCP Apps basic host:

```bash
./run-basic-host.sh
```

Open `http://127.0.0.1:8082`, select `show-inventory-transfer-dashboard`, and invoke it. This path requires no third-party host account.

ChatGPT can render this standards-based MCP App. Keep the Java service and
`mcp-app/run.sh` running, expose port 3001 through OpenAI Secure MCP Tunnel or
another HTTPS tunnel, enable ChatGPT Developer Mode, and create a
developer-mode plugin using the resulting `/mcp` URL. Start a new chat, select
the plugin, ask it to show the dashboard, select one exact recommendation,
review the notes, and choose **Approve and execute transfer** or **Cancel
review**. ChatGPT cannot connect directly to the loopback URL. See the detailed
steps in [`mcp-app/README.md`](mcp-app/README.md).

Claude in a regular browser has also rendered the same read-only MCP App through
a custom web connector using the public `/mcp` endpoint. The installed Chrome-
app/PWA surface returned a generic reachability error during the same test, so
that surface remains a compatibility retest rather than a database or MCP
failure.

For cross-machine ChatGPT or Claude validation, use the complete
[`docs/chatgpt-claude-mcp-app-handoff.md`](docs/chatgpt-claude-mcp-app-handoff.md)
runbook and record sanitized evidence in
[`docs/mcp-app-host-validation-results.md`](docs/mcp-app-host-validation-results.md).

Gemini Enterprise uses a separate private Cloud Run deployment of the same MCP
App. Its Discovery Engine service agent receives `roles/run.invoker`; end users
authorize the Custom MCP Server data store through a Google OAuth web client.
See [`docs/gemini-enterprise-mcp-app.md`](docs/gemini-enterprise-mcp-app.md) for
the deployment, OAuth, data-store, action-enablement, and validation steps.

## Gemini Enterprise native A2UI path

Keep the Java service running and start the separate adapter:

```bash
cd gemini-enterprise-a2a
./run.sh
```

Deploy that adapter at a public authenticated HTTPS endpoint and register its
agent card as a custom A2A agent in Gemini Enterprise. Gemini Enterprise
currently supports A2UI v0.8 rather than the browser's v0.9.1 envelopes, so the
adapter emits the older `beginRendering`, `surfaceUpdate`, and
`dataModelUpdate` shapes. See
[`gemini-enterprise-a2a/README.md`](gemini-enterprise-a2a/README.md). The
verified `adb-pm-prod` resource map, Cloud Run packaging, Secret Manager
boundary, and registration commands are documented in
[`docs/gemini-enterprise-gcp-deployment.md`](docs/gemini-enterprise-gcp-deployment.md).

## Security posture

- Only five bounded supply-chain business tools are enabled; do not use `-Dtools=*`.
- Every user-controlled database value is bound and validated server-side.
- A recommendation is determined by governed SQL, not invented by the browser or model.
- The approval ID is actor-bound, short-lived, single-use, and bound to the exact recommendation returned by the database.
- MCP App write tools are app-only; the model-visible tool remains read-only.
- Rejection never invokes the write operation.
- The stored procedure revalidates current quantities under row locks before reserving inventory.
- The procedure-backed MCP write is one atomic statement; the Toolkit commits only a successful tool statement.
- The A2UI renderer rejects unknown versions, catalogs, surfaces, envelopes, and components.
- Generated values are inserted with `textContent`, not executable HTML.
- Secrets are placeholders only in `.env.example`.

## Publication

[`blog.html`](blog.html) is the publication-ready technical article. It includes the required cover visual, key takeaways, architecture, end-to-end walkthrough, limitations, references, and FAQ.
