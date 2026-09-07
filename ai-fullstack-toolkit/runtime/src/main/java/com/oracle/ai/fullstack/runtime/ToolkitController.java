package com.oracle.ai.fullstack.runtime;

import com.oracle.ai.fullstack.model.ToolDefinition;
import com.oracle.ai.fullstack.projection.ToolProjections;
import com.oracle.ai.fullstack.registry.ToolRegistry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/tools")
public class ToolkitController {
    private final ToolRegistry registry;
    private final SupplyChainSpatialService spatial;
    public ToolkitController(ToolRegistry registry, SupplyChainSpatialService spatial) { this.registry = registry; this.spatial = spatial; }
    @GetMapping public Collection<ToolDefinition> list() { return registry.list(); }
    @PutMapping("/{id}") public ToolDefinition register(@PathVariable("id") String id, @RequestBody ToolDefinition tool) {
        if (!id.equals(tool.id())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path id and body id must match");
        return registry.register(tool);
    }
    @GetMapping("/{id}/mcp") public Map<String, Object> mcp(@PathVariable("id") String id) { return ToolProjections.mcpDescriptor(tool(id)); }
    @GetMapping("/{id}/a2a/card") public Map<String, Object> a2a(@PathVariable("id") String id) { return ToolProjections.a2aCard(tool(id), "http://localhost:8080"); }
    @GetMapping("/{id}/a2ui/example") public List<Map<String, Object>> a2ui(@PathVariable("id") String id) { return ToolProjections.a2uiExample(tool(id)); }
    @GetMapping("/{id}/mcp-app") public Map<String, Object> app(@PathVariable("id") String id) { return ToolProjections.mcpAppDescriptor(tool(id)); }
    @GetMapping("/database/spatial-demo") public SupplyChainSpatialService.SpatialResult databaseDemo(@RequestParam(name = "sku", defaultValue = "SKU-500") String sku) { return spatial.resolve("Show hotspots for " + sku); }
    private ToolDefinition tool(String id) { return registry.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown tool: " + id)); }
}
