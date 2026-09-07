package com.oracle.ai.fullstack.config;

import com.oracle.ai.fullstack.model.ToolDefinition;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/** Loads the toolkit's application configuration; it is not an MCP wire-format configuration. */
public final class ToolkitConfigurationLoader {
    @SuppressWarnings("unchecked")
    public List<ToolDefinition> load(InputStream input) {
        Map<String, Object> root = new Yaml().load(input);
        Map<String, Map<String, Object>> tools = (Map<String, Map<String, Object>>) root.getOrDefault("tools", Map.of());
        return tools.entrySet().stream().map(entry -> toDefinition(entry.getKey(), entry.getValue())).toList();
    }
    @SuppressWarnings("unchecked")
    private ToolDefinition toDefinition(String id, Map<String, Object> value) {
        Map<String, String> schema = new LinkedHashMap<>();
        ((Map<String, Object>) value.getOrDefault("inputs", Map.of())).forEach((key, val) -> schema.put(key, String.valueOf(val)));
        Map<String, Object> a2a = (Map<String, Object>) value.getOrDefault("a2a", Map.of());
        Map<String, Object> a2ui = (Map<String, Object>) value.getOrDefault("a2ui", Map.of());
        Map<String, Object> app = (Map<String, Object>) value.getOrDefault("mcpApp", Map.of());
        return new ToolDefinition(id, String.valueOf(value.getOrDefault("description", "")), schema,
                new ToolDefinition.McpExposure(enabled(value, "mcp", true)),
                new ToolDefinition.A2aExposure(enabled(a2a, "enabled", false), String.valueOf(a2a.getOrDefault("name", id)), String.valueOf(a2a.getOrDefault("description", value.getOrDefault("description", ""))), String.valueOf(a2a.getOrDefault("version", "0.1.0"))),
                new ToolDefinition.A2uiExposure(enabled(a2ui, "enabled", false), String.valueOf(a2ui.getOrDefault("surface", id + "-surface"))),
                new ToolDefinition.McpAppExposure(enabled(app, "enabled", false), String.valueOf(app.getOrDefault("resource", "ui://" + id))));
    }
    @SuppressWarnings("unchecked") private boolean enabled(Map<String, Object> parent, String key, boolean fallback) {
        Object raw = parent.get(key); if (raw instanceof Map<?, ?> map) raw = ((Map<String, Object>) map).get("enabled");
        return raw == null ? fallback : Boolean.parseBoolean(String.valueOf(raw));
    }
}
