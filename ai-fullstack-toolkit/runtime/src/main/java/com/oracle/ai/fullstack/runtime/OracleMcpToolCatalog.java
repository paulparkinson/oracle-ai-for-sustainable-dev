package com.oracle.ai.fullstack.runtime;

import com.oracle.ai.fullstack.model.ToolDefinition;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/** Imports the checked-in Oracle MCP toolkit tool catalog as MCP-only definitions. */
final class OracleMcpToolCatalog {
  private OracleMcpToolCatalog() {}
  @SuppressWarnings("unchecked")
  static List<ToolDefinition> load() {
    try (InputStream input = OracleMcpToolCatalog.class.getResourceAsStream("/oracle-mcp-tools.yaml")) {
      Map<String, Object> root = new Yaml().load(input);
      Map<String, Map<String, Object>> tools = (Map<String, Map<String, Object>>) root.get("tools");
      var result = new java.util.ArrayList<ToolDefinition>();
      result.add(mcpOnly("oracle-sql", "Oracle Database SQL tool from the Oracle MCP Java Toolkit.", Map.of("statement", "string", "parameters", "object"), ""));
      tools.forEach((id, config) -> {
        Map<String, String> schema = new LinkedHashMap<>();
        for (Map<String, Object> parameter : (List<Map<String, Object>>) config.getOrDefault("parameters", List.of())) schema.put(String.valueOf(parameter.get("name")), String.valueOf(parameter.getOrDefault("type", "string")));
        result.add(mcpOnly(id, String.valueOf(config.getOrDefault("description", "Oracle MCP toolkit tool.")), schema,
            String.valueOf(config.getOrDefault("statement", ""))));
      });
      return List.copyOf(result);
    } catch (Exception exception) { throw new IllegalStateException("Unable to load oracle-mcp-tools.yaml", exception); }
  }
  private static ToolDefinition mcpOnly(String id, String description, Map<String, String> schema, String statement) {
    return new ToolDefinition(id, description, schema, new ToolDefinition.McpExposure(true, statement),
        new ToolDefinition.A2aExposure(false, id, description, "0.1.0"), new ToolDefinition.A2uiExposure(false, id + "-surface"), new ToolDefinition.McpAppExposure(false, "ui://" + id));
  }
}
