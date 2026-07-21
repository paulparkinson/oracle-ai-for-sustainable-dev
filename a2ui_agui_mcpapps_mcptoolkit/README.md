# Interactive AI Applications with A2UI, AG-UI, MCP Apps, the Oracle Database MCP Java Toolkit, and Oracle AI Database

This reference project builds an account-risk assistant while keeping each protocol in its proper layer:

- Oracle AI Database owns governed data, transactions, stored procedures, and audit records.
- Oracle Database MCP Java Toolkit exposes narrow database tools.
- The Java agent service streams official AG-UI events and carries A2UI v0.9.1 envelopes.
- The browser validates and renders an allowlisted A2UI surface.
- A separate MCP App renders a richer dashboard inside compatible hosts.

See [`PROJECT_BRIEF.md`](PROJECT_BRIEF.md) for the requirements and [`docs/implementation-plan.md`](docs/implementation-plan.md) for verified versions, compatibility risks, and the phased delivery plan.

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

Open `http://127.0.0.1:8080`, run a high-risk review, choose an account, and approve or cancel a follow-up. On first start, `run.sh` downloads the pinned official Oracle Toolkit source into ignored `.runtime/`, builds it, and starts it as an MCP stdio child process. `McpToolkitRiskRepository` uses a small synchronous MCP protocol client to discover the exact tool allowlist and invoke every runtime database read and write. The Toolkit owns Oracle UCP and the database connection.

The ignored `financial/setup/.env` is reused automatically when it is present, so this repository does not duplicate the financial password. Its deployment-only JDBC URL is replaced locally with `financialdb_high` plus the configured wallet directory. The one-time setup runner uses UCP only to install the Toolkit’s database objects; it is not an application data path. It refuses unsafe partial state, upgrades the original schema with the MCP write objects, never drops objects, and becomes a no-op after successful installation. SQLcl users can alternatively run `database/setup.sql`.

The Oracle artifacts are not mocked:

- `database/` creates 20 accounts, 40 risk events, views, a sequence, and transaction-controlled procedures.
- `oracle-db-mcp-toolkit/config/tools.yaml` defines the bounded reads, action-ID reservation, procedure-backed write, and verification query.
- `oracle-db-mcp-toolkit/contracts/create-customer-follow-up.json` records the write tool contract enforced around the YAML tool.
- `mcp-app/` follows the stable MCP Apps `ui://` resource and structured-content pattern.

## Why the Toolkit is the preferred path

The Toolkit path is preferred for this reference architecture because its governed tool contracts can be reused by multiple agents and MCP-compatible clients. The application now implements that path:

1. `prepare-mcp-toolkit.sh` builds a pinned revision of Oracle's official Toolkit without copying its source into this repository. It applies the checked-in one-line stdio compatibility patch described below.
2. The agent starts it over stdio with `tools.yaml` and only the `account-risk` toolset.
3. Credentials pass in the child-process environment, not YAML, browser code, prompts, or Git.
4. The agent verifies the connected server identity and exact tool allowlist before accepting traffic.
5. Every application read and approved write executes through MCP. Only the separate one-time schema installer connects through its own UCP configuration.

The small committed compatibility patch disables tool-list-change notifications while the pinned Toolkit registers its static YAML tools before an stdio client session exists. It does not alter tool execution, SQL, datasource handling, or the strict application allowlist.

The current Toolkit YAML schema has no callable OUT-parameter mode. The demo therefore reserves an action ID with a bounded read tool and passes it into an input-only stored procedure tool. The procedure validates, locks, inserts, and updates in one database statement. A failed statement creates no business record, although Oracle sequences can legitimately contain gaps.

The pinned Toolkit revision advertises tool-list change notifications before its stdio transport can enqueue several startup registrations. `patches/stdio-tool-registration.patch` changes only the stdio capability from `tools(true)` to `tools(false)`. Static tool discovery and invocation remain available; dynamic list-change notifications are disabled for this fixed local configuration. The agent’s exact allowlist check fails closed if registration is incomplete.

## MCP App

With Node.js 20+ installed:

```bash
cd mcp-app
npm install
npm run typecheck
npm run build
npm run serve
```

Then connect an MCP Apps-compatible host to `http://127.0.0.1:3001/mcp` for local development.

## Security posture

- Only narrow business tools are enabled; do not use `-Dtools=*`.
- Every user-controlled database value is bound.
- Row counts, ranges, action types, notes, and actor fields are validated server-side.
- Approval IDs are actor-bound, short-lived, and single-use.
- Rejection never invokes the write operation.
- The procedure-backed MCP write is one atomic statement; the Toolkit commits only a successful tool statement.
- The A2UI renderer rejects unknown versions, catalogs, surfaces, envelopes, and components.
- Generated values are inserted with `textContent`, not executable HTML.
- Secrets are placeholders only in `.env.example`.

## Publication

[`blog.html`](blog.html) is the publication-ready technical article. It includes the required cover visual, key takeaways, decision guide, setup, limitations, and FAQ.
