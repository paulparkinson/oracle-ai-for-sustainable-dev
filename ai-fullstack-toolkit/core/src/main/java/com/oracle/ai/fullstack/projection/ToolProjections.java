package com.oracle.ai.fullstack.projection;

import com.oracle.ai.fullstack.model.ToolDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Generates protocol-facing documents from the common tool definition. */
public final class ToolProjections {
    private ToolProjections() {}
    public static Map<String, Object> mcpDescriptor(ToolDefinition tool) {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("name", tool.id());
        descriptor.put("description", tool.description());
        descriptor.put("inputSchema", Map.of("type", "object", "properties", tool.inputSchema()));
        if (!tool.mcp().statement().isBlank()) descriptor.put("statement", tool.mcp().statement());
        return Map.copyOf(descriptor);
    }
    public static Map<String, Object> a2aCard(ToolDefinition tool, String baseUrl) {
        var a = tool.a2a();
        return Map.of("name", a.name(), "description", a.description(), "version", a.version(),
                "url", baseUrl + "/a2a/" + tool.id(), "capabilities", Map.of("streaming", false),
                "skills", List.of(Map.of("id", tool.id(), "name", a.name(), "description", tool.description())));
    }
    public static List<Map<String, Object>> a2uiExample(ToolDefinition tool) {
        String surface = tool.a2ui().surfaceId();
        Map<String, Object> component = new LinkedHashMap<>();
        component.put("id", "transfer-review"); component.put("component", "Card");
        component.put("children", List.of(Map.of("component", "Text", "text", "Review " + tool.a2a().name()), Map.of("component", "Button", "label", "Approve transfer")));
        return List.of(Map.of("type", "beginRendering", "surfaceId", surface), Map.of("type", "surfaceUpdate", "surfaceId", surface, "components", List.of(component)));
    }
    public static Map<String, Object> mcpAppDescriptor(ToolDefinition tool) {
        return Map.of("resource", tool.mcpApp().resourceUri(), "mimeType", "text/html", "tool", tool.id());
    }
}
