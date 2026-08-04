# MCP App host validation results

This record captures the completed ChatGPT read-only validation and remains the
checklist for Claude. Do not record emails, passwords, API keys, wallet paths,
tunnel IDs, tunnel credentials, private endpoints, or other secrets.

## Test environment

- Date and timezone: August 3, 2026, America/New_York
- Git commit tested: Cloud Run deployment based on `c6a3a1e`
- Host and account type: ChatGPT with Developer mode
- Transport: temporary public HTTPS Cloud Run endpoint, immediately returned to private

## Local preflight

- Java service health:
- Recommendation source equals `oracle-db-mcp-java-toolkit`:
- MCP App health:
- Official basic-host result:
- MCP Inspector discovery and invocation:

## ChatGPT

- Tested: yes, read-only phase
- Developer mode available: yes
- Connection created: yes, `Supply-Chain Inventory Exchange`
- Discovered tool: `show-inventory-transfer-dashboard` only
- Direct prompt: `Show the inventory transfer dashboard for products with a minimum stockout risk of 70, limited to 3 recommendations.`
- Tool arguments: minimum risk `70`, limit `3`
- Dashboard rendered: yes, inline
- Toolkit source label visible: yes
- Live result: `WATER-SENSE`, `PHX-DC` to `SEA-FC`, 42 units, risk 74.6
- App-only approval returned audited transfer ID: not tested; writes disabled and no approval handle issued
- Separate cancellation performed no write: not tested in this read-only phase
- Dashboard screenshot: `images/chatgpt-mcp-app-dashboard.png`
- Setup screenshots: `images/chatgpt-developer-mode.png`, `images/chatgpt-plugin-connection.png`, `images/chatgpt-plugin-tool-discovery.png`
- Errors and minimal fixes: first load reported `Failed to fetch template`; retry rendered successfully. CSP enforcement was off and production CSP/domain metadata remains a hardening task.

## Claude

- Tested: yes/no
- Connector type and account category:
- Connection created:
- Discovered tool:
- Direct prompt:
- Tool arguments:
- Dashboard rendered:
- Toolkit source label visible:
- Selection returned to conversation:
- App-only approval returned audited transfer ID:
- Separate cancellation performed no write:
- Follow-up prompt result:
- Negative prompt avoided tool invocation:
- Dashboard screenshot:
- Selection screenshot:
- Errors and minimal fixes:

## Blog handback

- Confirmed statements the blog can make:
- Product-specific wording or navigation that changed:
- Limitations or caveats:
- Suggested screenshot captions and alt text:

