package com.oracle.ai.fullstack.runtime;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

class SupplyChainSpatialA2aControllerTest {
  @Test void servesCardAndCompletesAnA2aMessage() throws Exception {
    var service = new SupplyChainSpatialService(new StandardEnvironment());
    var controller = new SupplyChainSpatialA2aController(service, "https://example.test:8444");
    assertEquals("0.3.0", controller.card().get("protocolVersion"));
    var request = new ObjectMapper().readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"message/send\",\"params\":{\"message\":{\"parts\":[{\"kind\":\"text\",\"text\":\"Show hotspots for SKU-500\"}]}}}");
    var response = controller.message(request);
    assertEquals(1, response.get("id"));
    assertEquals("completed", ((java.util.Map<?, ?>) response.get("result")).get("status") instanceof java.util.Map<?, ?> status ? status.get("state") : null);
  }
}
