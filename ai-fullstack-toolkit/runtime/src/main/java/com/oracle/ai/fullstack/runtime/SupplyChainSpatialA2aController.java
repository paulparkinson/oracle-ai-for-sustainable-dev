package com.oracle.ai.fullstack.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Standalone A2A v0.3-compatible spatial specialist; no dependency on the existing agent process. */
@RestController
public class SupplyChainSpatialA2aController {
  private final SupplyChainSpatialService spatial;
  private final String publicUrl;
  public SupplyChainSpatialA2aController(SupplyChainSpatialService spatial, @Value("${ai.fullstack.public-url:http://localhost:8080}") String publicUrl) { this.spatial = spatial; this.publicUrl = publicUrl.replaceAll("/$", ""); }
  @GetMapping(value = {"/a2a/spatial/.well-known/agent-card.json", "/agent-card-supply-chain-spatial.json"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> card() {
    return Map.of("protocolVersion", "0.3.0", "name", "ai_fullstack_supply_chain_spatial", "description", "Supply-chain spatial specialist. It reads Oracle warehouse hotspot risk data and recommends a relief route.", "url", publicUrl + "/a2a/spatial", "version", "0.1.0", "capabilities", Map.of("streaming", false, "pushNotifications", false, "stateTransitionHistory", false), "defaultInputModes", List.of("text/plain"), "defaultOutputModes", List.of("text/plain", "application/json"), "skills", List.of(Map.of("id", "supply-chain-spatial", "name", "Supply-chain spatial hotspots", "description", "Locate risk hotspots and recommend warehouse relief routes for SKU inventory.", "tags", List.of("supply-chain", "spatial", "oracle-database"), "examples", List.of("Show warehouse hotspots for SKU-500 and recommend a relief route."), "inputModes", List.of("text/plain"), "outputModes", List.of("text/plain", "application/json"))), "preferredTransport", "JSONRPC");
  }
  @PostMapping(value = "/a2a/spatial", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> message(@RequestBody JsonNode request) {
    Object id = request.has("id") ? request.get("id").isNumber() ? request.get("id").numberValue() : request.get("id").asText() : null;
    if (!"message/send".equals(request.path("method").asText())) return rpcError(id, -32601, "Only message/send is supported");
    String prompt = text(request.path("params").path("message").path("parts"));
    var result = spatial.resolve(prompt);
    String summary = summary(result);
    String taskId = UUID.randomUUID().toString();
    Map<String, Object> data = new LinkedHashMap<>(); data.put("productId", result.productId()); data.put("region", result.region()); data.put("stockoutProbability", result.stockoutProbability()); data.put("sourceMode", result.sourceMode()); data.put("sourceDetail", result.sourceDetail()); data.put("hotspots", result.hotspots());
    Map<String, Object> status = Map.of(
        "state", "completed", "timestamp", Instant.now().toString(),
        "message", Map.of("kind", "message", "role", "agent", "parts", List.of(Map.of("kind", "text", "text", summary)))
    );
    Map<String, Object> artifact = Map.of(
        "artifactId", taskId + "-spatial", "name", "supply_chain_spatial_evidence",
        "parts", List.of(Map.of("kind", "text", "text", summary), Map.of("kind", "data", "data", data))
    );
    Map<String, Object> task = Map.of("kind", "task", "id", taskId, "contextId", UUID.randomUUID().toString(), "status", status, "artifacts", List.of(artifact));
    return id == null ? Map.of("jsonrpc", "2.0", "result", task) : Map.of("jsonrpc", "2.0", "id", id, "result", task);
  }
  private static String text(JsonNode parts) { if (!parts.isArray()) return ""; for (JsonNode part : parts) if ("text".equals(part.path("kind").asText())) return part.path("text").asText(""); return ""; }
  private static Map<String, Object> rpcError(Object id, int code, String message) { return id == null ? Map.of("jsonrpc", "2.0", "error", Map.of("code", code, "message", message)) : Map.of("jsonrpc", "2.0", "id", id, "error", Map.of("code", code, "message", message)); }
  private static String summary(SupplyChainSpatialService.SpatialResult r) { var target = r.hotspots().get(0); var source = r.hotspots().stream().filter(h -> h.recommendedRole().contains("SOURCE")).findFirst().orElse(r.hotspots().get(r.hotspots().size() - 1)); return "Spatial risk for " + r.productId() + ": " + target.name() + " (" + target.code() + ") is the primary hotspot with " + target.coverageDays() + " days of coverage. Recommend relief from " + source.name() + " (" + source.code() + "). Source: " + r.sourceMode() + "."; }
}
