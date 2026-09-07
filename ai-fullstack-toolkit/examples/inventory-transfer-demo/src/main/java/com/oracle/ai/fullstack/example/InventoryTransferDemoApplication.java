package com.oracle.ai.fullstack.example;

import com.oracle.ai.fullstack.model.ToolDefinition;
import com.oracle.ai.fullstack.projection.ToolProjections;
import com.oracle.ai.fullstack.registry.ToolRegistry;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal example of exposing an existing business operation on multiple AI surfaces. */
@SpringBootApplication
public class InventoryTransferDemoApplication {
  public static void main(String[] args) { SpringApplication.run(InventoryTransferDemoApplication.class, args); }
  @Bean CommandLineRunner registerInventoryTransfer(ToolRegistry registry) { return args -> registry.register(tool()); }
  @Bean ToolDefinition tool() { return new ToolDefinition("inventory-transfer", "Move SKU inventory between two locations.", Map.of("sku", "string", "quantity", "integer"), null,
      new ToolDefinition.A2aExposure(true, "Inventory Transfer Agent", "Drafts reviewable inventory moves.", "0.1.0"), new ToolDefinition.A2uiExposure(true, "inventory-transfer-review"), new ToolDefinition.McpAppExposure(true, "ui://inventory-transfer/review")); }
  @RestController static class DemoController {
    private final ToolDefinition tool; DemoController(ToolDefinition tool) { this.tool = tool; }
    @GetMapping("/demo/a2a-card") Map<String,Object> card() { return ToolProjections.a2aCard(tool, "http://localhost:8081"); }
    @GetMapping("/demo/a2ui") Object a2ui() { return ToolProjections.a2uiExample(tool); }
  }
}
