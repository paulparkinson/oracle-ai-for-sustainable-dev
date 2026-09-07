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
        return args -> registry.register(new ToolDefinition("inventory-transfer", "Propose and review an inventory transfer between locations.",
                Map.of("sourceLocation", "string", "destinationLocation", "string", "sku", "string", "quantity", "integer"), null,
                new ToolDefinition.A2aExposure(true, "Inventory Transfer Agent", "Plans an inventory transfer and returns a reviewable action.", "0.1.0"),
                new ToolDefinition.A2uiExposure(true, "inventory-transfer-review"), new ToolDefinition.McpAppExposure(true, "ui://inventory-transfer/review")));
    }
}
