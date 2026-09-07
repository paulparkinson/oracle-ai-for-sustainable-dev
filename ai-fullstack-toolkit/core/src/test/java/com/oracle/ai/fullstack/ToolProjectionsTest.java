package com.oracle.ai.fullstack;

import static org.junit.jupiter.api.Assertions.*;
import com.oracle.ai.fullstack.model.ToolDefinition;
import com.oracle.ai.fullstack.projection.ToolProjections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolProjectionsTest {
  @Test void projectsOneToolAcrossAllSurfaces() {
    var tool = new ToolDefinition("inventory-transfer", "Move inventory", Map.of("fromLocation", "string"), null,
      new ToolDefinition.A2aExposure(true, "Inventory Transfer", "Move inventory", "1.0"), new ToolDefinition.A2uiExposure(true, "transfer"), new ToolDefinition.McpAppExposure(true, "ui://transfer"));
    assertEquals("inventory-transfer", ToolProjections.mcpDescriptor(tool).get("name"));
    assertTrue(ToolProjections.a2aCard(tool, "https://example.test").get("url").toString().endsWith("/inventory-transfer"));
    assertEquals(2, ToolProjections.a2uiExample(tool).size());
  }
}
