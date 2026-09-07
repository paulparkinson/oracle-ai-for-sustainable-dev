package com.oracle.ai.fullstack.runtime;

import com.oracle.ai.fullstack.model.ToolDefinition;
import com.oracle.ai.fullstack.registry.ToolRegistry;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AiFullstackRuntimeApplication {
    public static void main(String[] args) { SpringApplication.run(AiFullstackRuntimeApplication.class, args); }
    @Bean CommandLineRunner seedInventoryTransfer(ToolRegistry registry) {
        return args -> {
            registry.register(new ToolDefinition("inventory-transfer-a2ui", "Review and approve a governed inventory transfer in an A2UI form.", Map.of("sourceLocation", "string", "destinationLocation", "string", "sku", "string", "quantity", "integer"), new ToolDefinition.McpExposure(false),
                new ToolDefinition.A2aExposure(true, "Inventory Transfer A2UI", "Plans and renders a reviewable inventory transfer form.", "0.1.0"), new ToolDefinition.A2uiExposure(true, "inventory-transfer-review"), new ToolDefinition.McpAppExposure(false, "ui://inventory-transfer/review")));
            registerMcpApp(registry, "inventory-transfer-mcpapp", "Inventory transfer MCP App", "ui://inventory-transfer/review");
            registerMcpApp(registry, "inventory-spatial-mcpapp", "Inventory spatial MCP App", "ui://inventory-spatial/map");
            registerMcpApp(registry, "inventory-graph-mcpapp", "Inventory graph MCP App", "ui://inventory-graph/dependencies");
            OracleMcpToolCatalog.load().forEach(registry::register);
        };
    }
    private static void registerMcpApp(ToolRegistry registry, String id, String description, String resource) {
        registry.register(new ToolDefinition(id, description, Map.of("sku", "string"), new ToolDefinition.McpExposure(true), new ToolDefinition.A2aExposure(false, id, description, "0.1.0"), new ToolDefinition.A2uiExposure(false, id + "-surface"), new ToolDefinition.McpAppExposure(true, resource)));
    }
}
