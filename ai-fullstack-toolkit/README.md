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

Open [http://localhost:8080](http://localhost:8080). The Tron-themed **Tool Surface Grid** shows the seeded `inventory-transfer` tool. Select each output tab to view the generated MCP descriptor, A2A card, A2UI messages, or MCP App resource descriptor. Use **Configure tool** to create or replace a definition in the running registry. This first runtime configuration store is intentionally in-memory: restart restores the seeded definition, which makes the demo safe to explore.

## Walkthrough: inventory transfer

1. Start the runtime with the command above.
2. In the UI, select `inventory-transfer`. Its four neon tiles show which surfaces are enabled.
3. Select **A2A CARD**. The result is the agent-card-shaped document an A2A client can discover; its generated endpoint is `/a2a/inventory-transfer`.
4. Select **A2UI**. The response begins a `inventory-transfer-review` surface and supplies a review card with an approval button. Your application is responsible for receiving that user action and enforcing authorization.
5. Select **MCP** or **MCP APP** to inspect the matching descriptor from the same definition.
6. Use **Configure tool** to add a second definition. The UI performs a `PUT /api/tools/{id}` and immediately regenerates all enabled projections.

Equivalent HTTP inspection:

```bash
curl -s http://localhost:8080/api/tools | jq
curl -s http://localhost:8080/api/tools/inventory-transfer/a2a/card | jq
curl -s http://localhost:8080/api/tools/inventory-transfer/a2ui/example | jq
curl -s http://localhost:8080/api/tools/inventory-transfer/mcp-app | jq
```

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

## Current scope and next integration step

This initial runnable slice supplies the unified contract, generated discovery/UI documents, live runtime API, GUI, test coverage, and a vendor baseline. It does not claim that each upstream database tool has already been migrated through the registry. The next implementation increment is an adapter that reads the vendored/original Oracle MCP toolkit `ToolConfig` entries and registers them as `ToolDefinition` instances, preserving Oracle Database MCP behavior while adding opt-in A2A/A2UI/MCP App exposure. This is intentionally opt-in: exposing an MCP tool as an agent endpoint must carry its existing authentication, authorization, input validation, auditing, and approval rules forward.
