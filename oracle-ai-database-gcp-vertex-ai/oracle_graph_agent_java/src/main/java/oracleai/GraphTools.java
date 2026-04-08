package oracleai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class GraphTools {

    public record GraphRequest(String productId) {}
    public record GraphResponse(List<Map<String, String>> nodes, List<Map<String, String>> edges) {}

    @Bean
    @Description("Fetches supply chain dependencies from Oracle Property Graph for a specific product ID")
    public Function<GraphRequest, GraphResponse> getSupplyChainDependencies() {
        return request -> {
            // MOCK: In production, execute PGQL query here
            var nodes = List.of(
                Map.of("id", "S1", "label", "Supplier: Global Logistics"),
                Map.of("id", "P1", "label", "Product: " + request.productId())
            );
            var edges = List.of(
                Map.of("from", "S1", "to", "P1", "label", "SUPPLIES")
            );
            return new GraphResponse(nodes, edges);
        };
    }
}