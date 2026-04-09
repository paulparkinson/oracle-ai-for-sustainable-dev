package oracleai;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.agents.ParallelAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.FunctionTool;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class InventoryActionAdkService {

    private static final String APP_NAME = "oracle-inventory-action";

    private final InMemoryRunner runner;

    public InventoryActionAdkService(Environment environment, InventoryActionTools tools) {
        String modelName = firstNonBlank(
                environment.getProperty("ACTION_COORDINATOR_MODEL"),
                environment.getProperty("MODEL_NAME"),
                "gemini-2.0-flash"
        );

        FunctionTool graphEvidenceTool = FunctionTool.create(tools, "getGraphEvidence");
        FunctionTool spatialEvidenceTool = FunctionTool.create(tools, "getSpatialEvidence");
        FunctionTool externalSignalsTool = FunctionTool.create(tools, "getExternalSignals");
        FunctionTool policyTool = FunctionTool.create(tools, "checkTransferPolicy");
        FunctionTool draftActionTool = FunctionTool.create(tools, "draftInventoryTransferAction");

        LlmAgent graphEvidenceAgent = LlmAgent.builder()
                .name("graph_evidence_specialist")
                .description("Oracle Graph specialist for supply-chain dependency evidence.")
                .model(modelName)
                .instruction("""
                        You are the graph-evidence specialist for inventory risk response.
                        Always call getGraphEvidence for the relevant productId before answering.
                        Return only a concise supply-chain evidence summary and never recommend an action.
                        """)
                .tools(graphEvidenceTool)
                .build();

        LlmAgent spatialEvidenceAgent = LlmAgent.builder()
                .name("spatial_evidence_specialist")
                .description("Spatial hotspot specialist for warehouse pressure and transfer direction.")
                .model(modelName)
                .instruction("""
                        You are the spatial-evidence specialist for inventory risk response.
                        Always call getSpatialEvidence for the relevant productId before answering.
                        Return only a concise hotspot summary with recommended source and destination warehouses.
                        Do not make a final action recommendation.
                        """)
                .tools(spatialEvidenceTool)
                .build();

        LlmAgent externalSignalsAgent = LlmAgent.builder()
                .name("external_signal_specialist")
                .description("External-risk specialist for weather and geopolitical supply-lane impacts.")
                .model(modelName)
                .instruction("""
                        You are the external-signals specialist for inventory risk response.
                        Always call getExternalSignals for the relevant productId before answering.
                        Return only a concise summary of outside factors that could change the timing or urgency of an action.
                        Do not make a final action recommendation.
                        """)
                .tools(externalSignalsTool)
                .build();

        ParallelAgent parallelEvidenceAgent = ParallelAgent.builder()
                .name("parallel_evidence_gatherer")
                .description("Runs graph, spatial, and external-signal specialists in parallel before an action recommendation is made.")
                .subAgents(graphEvidenceAgent, spatialEvidenceAgent, externalSignalsAgent)
                .build();

        LlmAgent decisionAgent = LlmAgent.builder()
                .name("inventory_action_decider")
                .description("Synthesizes evidence, checks policy, and drafts an inventory action recommendation.")
                .model(modelName)
                .instruction("""
                        You are the final inventory-action coordinator.
                        Review the graph, spatial, and external evidence already gathered in this session.
                        Your job is to recommend one next step: transfer, expedite, substitute, or hold.
                        If a transfer is the best next move, call checkTransferPolicy first and then call draftInventoryTransferAction.
                        Never claim that an inventory move has been executed.
                        Your final answer must include:
                        1. Recommended action.
                        2. Why that action is justified from the evidence.
                        3. Whether approval is required.
                        4. If you drafted a move, the draft action id and the proposed source, destination, and units.
                        If evidence is missing, say so plainly and recommend the safest next step.
                        """)
                .tools(policyTool, draftActionTool)
                .build();

        SequentialAgent rootAgent = SequentialAgent.builder()
                .name("inventory_action_orchestrator")
                .description("Coordinates final-stage inventory action planning using evidence specialists and a decision agent.")
                .subAgents(parallelEvidenceAgent, decisionAgent)
                .build();

        this.runner = new InMemoryRunner(rootAgent, APP_NAME);
    }

    public InventoryActionResult run(String userInput, String contextId) {
        String normalizedInput = userInput == null || userInput.isBlank()
                ? "Recommend an inventory action for SKU-500."
                : userInput.trim();
        String userId = firstNonBlank(contextId, "inventory-action-user");
        String sessionId = firstNonBlank(contextId, "inventory-action-session");
        Session session = ensureSession(userId, sessionId);

        Flowable<Event> eventStream = runner.runAsync(
                userId,
                session.id(),
                Content.fromParts(Part.fromText(normalizedInput)),
                RunConfig.builder().build(),
                Map.of()
        );

        List<String> trace = new ArrayList<>();
        String[] finalText = new String[] {""};

        eventStream.blockingForEach(event -> {
            String content = event.stringifyContent();
            if (!content.isBlank()) {
                trace.add(content);
            }
            if (event.finalResponse() && !content.isBlank()) {
                finalText[0] = content;
            }
        });

        String resolvedText = !finalText[0].isBlank()
                ? finalText[0]
                : trace.stream().filter(text -> !text.isBlank()).reduce((left, right) -> right).orElse(
                        "The inventory action coordinator did not return a final recommendation."
                );

        return new InventoryActionResult(resolvedText, trace);
    }

    private Session ensureSession(String userId, String sessionId) {
        return runner.sessionService()
                .getSession(APP_NAME, userId, sessionId, Optional.empty())
                .switchIfEmpty(
                        runner.sessionService()
                                .createSession(APP_NAME, userId, new ConcurrentHashMap<>(), sessionId)
                                .toMaybe()
                )
                .blockingGet();
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return "";
    }

    public record InventoryActionResult(String responseText, List<String> trace) {}
}
