package oracleai;

import io.a2a.spec.AgentCard;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdditionalAgentCardController {

    private final Environment environment;

    public AdditionalAgentCardController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping(
            value = {"/agent-card-graph.json", "/graph/.well-known/agent-card.json"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    AgentCard graphAgentCard() {
        return GraphA2AConfiguration.buildGraphAgentCard(environment);
    }

    @GetMapping(
            value = {
                    "/agent-card-spatial.json",
                    "/spatial-agent-card.json",
                    "/spatial/.well-known/agent-card.json"
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    AgentCard spatialAgentCard() {
        return GraphA2AConfiguration.buildSpatialAliasCard(environment);
    }
}
