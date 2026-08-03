# ChatGPT MCP App on Cloud Run

## Status

Verified privately on August 3, 2026 in `adb-pm-prod/us-east4`:

- Cloud Build completed the Java, Toolkit, TypeScript typecheck, and UI build.
- Cloud Run revision `oracle-supply-chain-mcp-app-00002-q52` became ready.
- Anonymous health returned HTTP 403.
- Authenticated health returned `UP` and `writeActionsEnabled: false`.
- MCP discovery returned only `show-inventory-transfer-dashboard`.
- The `ui://oracle-supply-chain/inventory-exchange-v1` resource returned
  `text/html;profile=mcp-app`.
- A read-only tool call returned one live Toolkit-governed `WATER-SENSE`
  recommendation from `PHX-DC` to `SEA-FC` with risk 74.6.
- No approval handle was returned and no write or rejection tools were
  registered.

ChatGPT host rendering is not yet verified.

## Topology and sizing

The MCP App does not run on a Compute Engine VM. It has a dedicated Cloud Run
service so its ingress and authentication lifecycle remain independent of the
working Gemini Enterprise A2A service:

```text
ChatGPT
  -> HTTPS /mcp
  -> Cloud Run: oracle-supply-chain-mcp-app (Node, port 8080)
  -> Java agent service (loopback, port 8081)
  -> Oracle Database MCP Java Toolkit (stdio)
  -> paulparkdb_tp through Direct VPC egress
```

The initial limit is 2 vCPU and 2 GiB with concurrency 10 and a maximum of one
instance. This matches the proven Gemini container envelope; the only added
runtime is one Node process. No VM resize is needed. Use one minimum instance
during an interactive demonstration to avoid the combined Java/Toolkit cold
start, then return to zero minimum instances when continuous readiness is not
required.

## Deploy privately

From the project directory:

```powershell
.\deploy\gcp\deploy-chatgpt-mcp.ps1
```

The script builds `Dockerfile.mcp-app-cloud-run`, uses the existing runtime
service account, wallet secret, password secret, VPC, and subnet, and deploys
with `--no-allow-unauthenticated`. It defaults to
`MCP_WRITES_ENABLED=false`.

To keep one warm instance:

```powershell
.\deploy\gcp\deploy-chatgpt-mcp.ps1 -MinInstances 1
```

## Access decision before ChatGPT

Cloud Run IAM proves the private service but is not the end-user OAuth flow
required by ChatGPT. OpenAI documents that customer-specific data and write
actions should use an MCP-compatible OAuth 2.1 authorization-code flow with
PKCE. ChatGPT cannot send custom API keys, Google service-account credentials,
or a client-credentials grant.

There are two safe phases:

1. **Synthetic read-only developer validation.** After explicit approval,
   temporarily grant public Cloud Run invocation while
   `MCP_WRITES_ENABLED=false`. The server registers only the dashboard tool and
   returns no approval handle. Keep one warm instance, capture the ChatGPT UI
   evidence, then remove `allUsers` invocation.
2. **Authenticated write validation.** Integrate an established OAuth 2.1
   provider that supports MCP protected-resource metadata, authorization-server
   discovery, PKCE, an appropriate ChatGPT client-registration method, and the
   `resource` audience. Validate every bearer token's signature, issuer,
   audience, expiration, and scopes before enabling write actions.

The deployment script deliberately refuses this unsafe combination:

```powershell
.\deploy\gcp\deploy-chatgpt-mcp.ps1 `
  -AllowUnauthenticated -EnableWriteActions
```

## ChatGPT read-only validation

After the read-only endpoint is explicitly made reachable:

1. Open ChatGPT **Settings -> Security and login** and enable
   **Developer mode**.
2. Open `https://chatgpt.com/plugins` and select the plus button.
3. Name it `Oracle Supply-Chain Inventory Exchange`.
4. Enter the Cloud Run URL including `/mcp`.
5. Confirm discovery lists only `show-inventory-transfer-dashboard`.
6. Start a new chat, enable the connection, and prompt:

```text
Show the inventory transfer dashboard for products with a minimum stockout
risk of 70, limited to 3 recommendations.
```

Verify that ChatGPT calls the tool with `70` and `3`, renders the MCP App,
shows the Toolkit source label and live `WATER-SENSE` card, and provides no
approval controls. Capture sanitized screenshots and then revoke anonymous
Cloud Run invocation.

## Official OpenAI references

- [Authentication](https://developers.openai.com/plugins/build/auth)
- [Add UI to an MCP server](https://developers.openai.com/plugins/build/chatgpt-ui)
- [Connect and test a plugin](https://developers.openai.com/plugins/deploy/connect-chatgpt)
