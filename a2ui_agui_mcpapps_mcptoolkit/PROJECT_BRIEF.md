# Project: Interactive AI Applications with A2UI, AG-UI, MCP Apps, and Oracle AI Database

## Objective

Create a runnable reference application and technical blog showing Oracle AI Database as the governed data and execution layer for an interactive account-risk assistant built with:

- Oracle Database MCP Java Toolkit
- AG-UI for streaming agent events and application state
- A2UI for declarative, client-rendered user interfaces
- MCP Apps for rich interfaces inside an MCP-compatible host

The application must explain where the technologies complement one another and where their responsibilities overlap.

## Core message

Oracle AI Database owns trusted retrieval, transactions, business logic, vector search, authorization, and auditing. The Oracle Database MCP Java Toolkit exposes narrowly scoped database capabilities as MCP tools. AG-UI carries agent progress, messages, tool activity, and state. A2UI describes safe native controls. MCP Apps provide richer sandboxed dashboards.

## Reference use case

> Show me high-risk customer accounts, explain the main risk factors, and let me approve a follow-up action.

1. The user submits a request.
2. The agent invokes a narrow MCP database tool.
3. Oracle AI Database returns governed account-risk data.
4. AG-UI streams run, message, tool, and state events.
5. A2UI renders filters, account results, and approval controls.
6. An optional MCP App renders a richer risk dashboard.
7. The user selects an account and explicitly approves an action.
8. A narrow MCP tool calls a stored procedure in one transaction.
9. The UI displays the audited result.

## Responsibility boundaries

Do not place AG-UI and A2UI inside the Oracle Database MCP Java Toolkit.

- **Oracle Database MCP Java Toolkit:** database-facing MCP tools
- **Agent service:** orchestration, model integration, approval state, MCP client, and AG-UI events
- **Web client:** AG-UI consumption and allowlisted A2UI rendering
- **MCP App:** optional embedded dashboard
- **Oracle AI Database:** system of record, transactions, policies, and auditing

Extend the toolkit only where database-specific behavior cannot be expressed safely through YAML-defined tools.

## Security requirements

- Never commit passwords, wallets, tokens, client secrets, or API keys.
- Use bind variables for user-provided values.
- Do not expose unrestricted SQL tools to the model.
- Enable only the three business tools required by the demo.
- Require explicit approval before a write.
- Put writes behind stored procedures or narrow Java tools.
- Validate tool inputs and A2UI envelopes.
- Allowlist A2UI components; never execute generated JavaScript.
- Treat MCP App content as sandboxed, untrusted web content.
- Log actor, tool, timestamp, input classification, and result status without logging secrets or sensitive records.
- Roll back failed actions and enforce limits and timeouts.

## Definition of done

- Reproducible schema and data for at least 20 accounts
- Three narrow business-tool contracts
- AG-UI event streaming for an account-risk run
- A2UI account results and approval controls
- Separate MCP App risk dashboard
- Transactional approved action; rejection causes no write
- Tests for reads, approvals, rejection, invalid input, and rollback
- Secret-free local setup and a followable blog

This checked-in brief is a concise, implementation-oriented rendering of the attached Codex project brief. The compatibility research and any deliberate deviations are recorded in `docs/implementation-plan.md`.
