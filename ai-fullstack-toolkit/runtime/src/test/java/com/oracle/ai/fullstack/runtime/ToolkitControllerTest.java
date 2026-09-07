package com.oracle.ai.fullstack.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.oracle.ai.fullstack.model.ToolDefinition;
import com.oracle.ai.fullstack.registry.ToolRegistry;
import java.util.Map;
import org.springframework.core.env.StandardEnvironment;
import org.junit.jupiter.api.Test;

class ToolkitControllerTest {
  @Test void controllerReturnsGeneratedA2aAndA2uiDocuments() {
    var registry = new ToolRegistry();
    registry.register(new ToolDefinition("inventory-transfer", "Move inventory", Map.of(), null,
      new ToolDefinition.A2aExposure(true, "Inventory Transfer Agent", "Move inventory", "0.1.0"), new ToolDefinition.A2uiExposure(true, "review"), new ToolDefinition.McpAppExposure(true, "ui://review")));
    var controller = new ToolkitController(registry, new SupplyChainSpatialService(new StandardEnvironment()));
    assertEquals("Inventory Transfer Agent", controller.a2a("inventory-transfer").get("name"));
    assertEquals("review", controller.a2ui("inventory-transfer").get(0).get("surfaceId"));
  }
}
