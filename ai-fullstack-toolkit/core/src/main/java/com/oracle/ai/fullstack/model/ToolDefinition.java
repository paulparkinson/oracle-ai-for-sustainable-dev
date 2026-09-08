package com.oracle.ai.fullstack.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** A transport-neutral description of one business tool and its enabled surfaces. */
public record ToolDefinition(String id, String description, Map<String, String> inputSchema,
                             McpExposure mcp, A2aExposure a2a, A2uiExposure a2ui, McpAppExposure mcpApp) {
    public ToolDefinition {
        if (id == null || !id.matches("[a-z0-9][a-z0-9-]*")) throw new IllegalArgumentException("Tool id must be lowercase kebab-case");
        description = Objects.requireNonNullElse(description, "");
        inputSchema = Map.copyOf(inputSchema == null ? Map.of() : new LinkedHashMap<>(inputSchema));
        mcp = mcp == null ? new McpExposure(true) : mcp;
        a2a = a2a == null ? new A2aExposure(false, id, description, "0.1.0") : a2a;
        a2ui = a2ui == null ? new A2uiExposure(false, id + "-surface") : a2ui;
        mcpApp = mcpApp == null ? new McpAppExposure(false, "ui://" + id) : mcpApp;
    }
    /** MCP projection settings; statement is optional because generic tools need not expose SQL. */
    public record McpExposure(boolean enabled, String statement) {
        public McpExposure(boolean enabled) { this(enabled, ""); }
        public McpExposure { statement = Objects.requireNonNullElse(statement, ""); }
    }
    public record A2aExposure(boolean enabled, String name, String description, String version) {}
    public record A2uiExposure(boolean enabled, String surfaceId) {}
    public record McpAppExposure(boolean enabled, String resourceUri) {}
}
