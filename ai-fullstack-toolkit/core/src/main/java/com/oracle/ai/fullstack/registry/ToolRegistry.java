package com.oracle.ai.fullstack.registry;

import com.oracle.ai.fullstack.model.ToolDefinition;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe source of truth used to project a tool into MCP, A2A, A2UI and MCP Apps. */
public final class ToolRegistry {
    private final ConcurrentHashMap<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    public ToolDefinition register(ToolDefinition definition) { tools.put(definition.id(), definition); return definition; }
    public Optional<ToolDefinition> find(String id) { return Optional.ofNullable(tools.get(id)); }
    public Collection<ToolDefinition> list() { return tools.values().stream().sorted(java.util.Comparator.comparing(ToolDefinition::id)).toList(); }
}
