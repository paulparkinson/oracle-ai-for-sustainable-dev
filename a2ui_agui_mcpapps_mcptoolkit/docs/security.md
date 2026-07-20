# Security notes

## Trust boundaries

The browser, model output, A2UI envelopes, MCP App content, and tool arguments are untrusted. The agent service is the policy point for approval and tool allowlisting. Oracle AI Database is the final enforcement and transaction point.

## Controls implemented in the foundation

- Server validation for score, limit, customer ID, action type, notes, and actor
- Short-lived, actor-bound, single-use approval nonce
- Rejection path that never calls a write tool
- Fixed A2UI version, catalog, surface, message, and component allowlists
- DOM construction with `textContent`; no generated scripts or HTML
- Prepared bind placeholders in toolkit YAML
- Stored-procedure validation and caller-owned transaction boundary
- Tool allowlist limited to account-risk business operations
- Placeholder-only environment file

## Controls required for a deployment

- TLS on the agent and MCP endpoints
- OAuth 2.0 rather than the toolkit's development token mode
- Least-privileged Oracle database user with execute/select grants only
- MCP client authentication and authorization mapped to the authenticated actor
- Persistent, hashed idempotency/approval records rather than process memory
- Server-side session binding and CSRF protection for browser actions
- Explicit CORS and MCP App CSP/permission declarations
- Database or centralized audit trail for every tool outcome
- Timeouts, cancellation propagation, rate limits, and bounded response sizes
- Redaction rules for logs and model context
- Integration tests that prove rollback when the procedure or audit write fails
