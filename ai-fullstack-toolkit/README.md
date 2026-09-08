# AI Fullstack Toolkit

AI Fullstack Toolkit makes one business-tool definition available across four complementary AI surfaces:

| Surface | Generated output |
| --- | --- |
| MCP | Tool descriptor and JSON input schema |
| A2A | Agent card with an addressable skill |
| A2UI | Declarative review surface messages |
| MCP Apps | UI resource descriptor for an interactive tool experience |

It deliberately has only three publishable Maven artifacts:

1. `ai-fullstack-toolkit` — framework-neutral Java core: definitions, validation, registry, YAML loader, and projections.
2. `ai-fullstack-toolkit-starter` — Spring Boot auto-configuration for adding the registry to an application.
3. `ai-fullstack-runtime` — standalone Spring Boot runtime and configuration UI.

The included inventory-transfer project is a non-published example, not a fourth library. The upstream Oracle Database MCP Java Toolkit source is vendored as a pinned functional reference under [`upstream/`](UPSTREAM.md); it is not copied into or hidden behind the new API.

## Quick start

Prerequisites: JDK 17+ and Maven 3.9+.

```bash
cd ai-fullstack-toolkit
mvn test
mvn -pl runtime -am spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). The Tron-themed **AI Fullstack Toolkit** dashboard displays the seeded definitions and their enabled surfaces. Select each output tab to view the generated MCP descriptor, A2A card, A2UI messages, or MCP App resource descriptor. Use **Create** to create or replace a definition in the running registry. This first runtime configuration store is intentionally in-memory: restart restores the seeded definition, which makes the demo safe to explore.

## Seeded supply-chain surfaces

| Tool | MCP | A2A | A2UI | MCP App |
| --- | --- | --- | --- | --- |
| `inventory-transfer-a2ui` | disabled | enabled | enabled | disabled |
| `inventory-transfer-mcpapp` | enabled | disabled | disabled | enabled |
| `inventory-spatial-mcpapp` | enabled | disabled | disabled | enabled |
| `inventory-graph-mcpapp` | enabled | disabled | disabled | enabled |

The runtime also imports the checked-in Oracle Database MCP Java Toolkit `tools.yaml` snapshot as MCP-only definitions. This makes the SQL surface visible as `oracle-sql`, alongside the existing supply-chain toolkit entries such as `find-stockout-transfer-recommendations` and `approve-inventory-transfer`. For each imported configured tool, the MCP tab also displays its exact SQL statement or PL/SQL block. The imported configuration has placeholders only (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`); it contains no database secret.

## Walkthrough: inventory transfer

1. Start the runtime with the command above.
2. Select `inventory-transfer-a2ui`. Its tiles show only **A2A** and **A2UI** enabled. The A2A card has an addressable inventory-transfer skill, and the A2UI tab begins the `inventory-transfer-review` surface.
3. Select any `*-mcpapp` tool. Its tiles show only **MCP** and **MCP APP** enabled. Use the MCP APP tab to inspect the interactive resource URI.
4. Select `oracle-sql` or an imported supply-chain tool to inspect the MCP descriptor and input schema imported from the Oracle toolkit catalog.
5. Use **Create** to add a definition. The UI performs a `PUT /api/tools/{id}` and immediately regenerates all enabled projections.

Equivalent HTTP inspection:

```bash
curl -s http://localhost:8080/api/tools | jq
curl -s http://localhost:8080/api/tools/inventory-transfer-a2ui/a2a/card | jq
curl -s http://localhost:8080/api/tools/inventory-transfer-a2ui/a2ui/example | jq
curl -s http://localhost:8080/api/tools/inventory-spatial-mcpapp/mcp-app | jq
curl -s http://localhost:8080/api/tools/oracle-sql/mcp | jq
```

## Demonstrating database access

Use **DATABASE CHECK** in the dashboard, or call the equivalent endpoint:

```bash
curl -s 'http://localhost:8080/api/tools/database/spatial-demo?sku=SKU-500' | jq
```

It runs the supply-chain spatial lookup with the runtime's configured Oracle JDBC connection. A real database result is unambiguous: the response has `"sourceMode": "oracle-database"`, along with the resolved inventory locations. If it returns `"sourceMode": "seeded-demo"`, the response's `sourceDetail` identifies why the live connection was unavailable; that is intentionally not presented as a database demonstration. Configure `DB_USERNAME`, `DB_PASSWORD`, `DB_DSN`, and, for wallet connections, `TNS_ADMIN` (or `DB_WALLET_DIR`) before starting the runtime.

## Configuration format

The core can load a portable application YAML document. It is deliberately named as toolkit configuration rather than presented as an MCP standard:

```yaml
tools:
  inventory-transfer:
    description: Move SKU inventory between two locations.
    inputs:
      sku: string
      quantity: integer
    mcp:
      enabled: true
    a2a:
      enabled: true
      name: Inventory Transfer Agent
      version: 0.1.0
    a2ui:
      enabled: true
      surface: inventory-transfer-review
    mcpApp:
      enabled: true
      resource: ui://inventory-transfer/review
```

Use it directly from Java:

```java
var definitions = new ToolkitConfigurationLoader().load(inputStream);
definitions.forEach(registry::register);
```

## Spring Boot integration

Add the starter to an existing service, then inject `ToolRegistry` and register a `ToolDefinition`. The registry remains independent of Spring, so the same definitions can run in a CLI, servlet, or another framework. The included [`examples/inventory-transfer-demo`](examples/inventory-transfer-demo/README.md) demonstrates the smallest useful Spring Boot service:

```bash
mvn -pl examples/inventory-transfer-demo -am spring-boot:run
curl -s http://localhost:8081/demo/a2a-card | jq
```

## Current scope

This runnable slice supplies the unified contract, generated discovery/UI documents, live runtime API, GUI, test coverage, and a vendor baseline. It imports the original Oracle toolkit YAML entries as MCP-only dashboard definitions; their production execution remains owned by the Oracle MCP Java Toolkit. Exposing an MCP tool as an A2A, A2UI, or MCP App surface is intentionally opt-in and must carry its authentication, authorization, input validation, auditing, and approval rules forward.
