# Interactive AI Applications with A2UI, AG-UI, MCP Apps, and Oracle AI Database

This reference project builds an account-risk assistant while keeping each protocol in its proper layer:

- Oracle AI Database owns governed data, transactions, stored procedures, and audit records.
- Oracle Database MCP Java Toolkit exposes narrow database tools.
- The Java agent service streams official AG-UI events and carries A2UI v0.9.1 envelopes.
- The browser validates and renders an allowlisted A2UI surface.
- A separate MCP App renders a richer dashboard inside compatible hosts.

See [`PROJECT_BRIEF.md`](PROJECT_BRIEF.md) for the requirements and [`docs/implementation-plan.md`](docs/implementation-plan.md) for verified versions, compatibility risks, and the phased delivery plan.

## What runs now

The dependency-free development path requires only JDK 21 (JDK 17 also works if virtual-thread executor use is replaced):

```bash
cd agent-service
./test.sh
./run.sh
```

Open `http://127.0.0.1:8080`, run a high-risk review, choose an account, and approve or cancel a follow-up. The development adapter uses deterministic in-memory data so the streaming and approval UX can run without credentials or a database.

The Oracle artifacts are not mocked:

- `database/` creates 20 accounts, 40 risk events, views, and the transaction-controlled procedure.
- `oracle-db-mcp-toolkit/config/tools.yaml` uses the toolkit's current YAML schema for two narrow read tools.
- `oracle-db-mcp-toolkit/contracts/create-customer-follow-up.json` defines the narrow write extension needed because current YAML has no OUT-parameter mode.
- `mcp-app/` follows the stable MCP Apps `ui://` resource and structured-content pattern.

## Live Oracle path

1. Create the schema with `database/01-schema.sql` through `04-views.sql`.
2. Build the toolkit from a sibling checkout of `oracle/mcp/src/oracle-db-mcp-java-toolkit`.
3. Run it with Streamable HTTP, TLS/authentication, this project's `tools.yaml`, and `-Dtools=account-risk-read`.
4. Implement the write contract as a minimal Java toolkit extension using `CallableStatement`, a registered numeric OUT parameter, explicit commit, and rollback on error.
5. Replace `DemoRiskRepository` with an MCP client adapter; no database credential belongs in the browser or agent prompts.

The separation is intentional. The base app works without MCP Apps, and the MCP App never connects directly to Oracle Database.

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
- The stored procedure does not commit; the caller controls commit/rollback.
- The A2UI renderer rejects unknown versions, catalogs, surfaces, envelopes, and components.
- Generated values are inserted with `textContent`, not executable HTML.
- Secrets are placeholders only in `.env.example`.

## Publication

[`blog.html`](blog.html) is the publication-ready technical article. It includes the required cover visual, key takeaways, decision guide, setup, limitations, and FAQ.
