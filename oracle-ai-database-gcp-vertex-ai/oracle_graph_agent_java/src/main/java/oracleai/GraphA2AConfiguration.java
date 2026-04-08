package oracleai;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class GraphA2AConfiguration {

    private static final Pattern PRODUCT_ID_PATTERN =
            Pattern.compile("\\b([A-Z]{2,}-\\d+)\\b");

    @Bean
    AgentCard agentCard(Environment environment) {
        String publicProtocol = valueOrDefault(environment, "PUBLIC_PROTOCOL", "http");
        String publicHost = valueOrDefault(environment, "PUBLIC_HOST", "localhost");
        String graphPort = valueOrDefault(
                environment,
                "GRAPH_AGENT_PORT",
                valueOrDefault(environment, "PORT", "8081")
        );
        String graphUrl = firstNonBlank(
                environment.getProperty("GRAPH_AGENT_URL"),
                environment.getProperty("A2A_URL"),
                String.format("%s://%s:%s", publicProtocol, publicHost, graphPort)
        );

        return new AgentCard(
                "oracle_graph_agent",
                "Specialist in Oracle Graph dependency analysis and supply chain visualization.",
                graphUrl,
                null,
                "0.0.1",
                null,
                new AgentCapabilities(false, false, false, List.of()),
                List.of("text/plain"),
                List.of("image/png", "text/plain"),
                List.of(
                        new AgentSkill(
                                "oracle_graph_agent",
                                "model",
                                "Specialist in Oracle Graph dependency analysis. When a user asks for supply chain dependencies or graph relationships, use the getSupplyChainDependencies tool.",
                                List.of("llm"),
                                List.of(),
                                List.of("text/plain"),
                                List.of("image/png", "text/plain"),
                                null
                        ),
                        new AgentSkill(
                                "oracle_graph_agent-getSupplyChainDependencies",
                                "getSupplyChainDependencies",
                                "Fetches supply chain dependencies from Oracle Property Graph for a specific product ID.",
                                List.of("llm", "tools"),
                                List.of(),
                                null,
                                List.of("image/png"),
                                null
                        )
                ),
                false,
                null,
                null,
                null,
                List.of(),
                "JSONRPC",
                "0.3.0",
                null
        );
    }

    @Bean
    AgentExecutor agentExecutor(
            Function<GraphTools.GraphRequest, GraphTools.GraphResponse> getSupplyChainDependencies
    ) {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                TaskUpdater updater = new TaskUpdater(context, eventQueue);
                if (context.getTask() == null) {
                    updater.submit();
                }

                updater.startWork();

                try {
                    String userInput = context.getUserInput("");
                    String productId = extractProductId(userInput);
                    GraphTools.GraphResponse graphResponse =
                            getSupplyChainDependencies.apply(new GraphTools.GraphRequest(productId));
                    String responseText = formatResponse(productId, graphResponse);
                    Part<?> imagePart = new FilePart(
                            new FileWithBytes(
                                    "image/png",
                                    "supply-chain-graph.png",
                                    renderGraphPng(productId, graphResponse)
                            )
                    );

                    updater.addArtifact(
                            List.of(imagePart),
                            null,
                            "supply_chain_graph_png",
                            Map.of(
                                    "productId", productId,
                                    "contentType", "image/png"
                            )
                    );
                    updater.complete(
                            updater.newAgentMessage(
                                    List.of(new TextPart(responseText)),
                                    Map.of(
                                            "tool", "getSupplyChainDependencies",
                                            "artifactName", "supply-chain-graph.png"
                                    )
                            )
                    );
                } catch (Exception e) {
                    updater.fail(
                            updater.newAgentMessage(
                                    List.of(new TextPart("Graph agent failed: " + e.getMessage())),
                                    Map.of("error", "graph_agent_execution_failed")
                            )
                    );
                    throw new JSONRPCError(-32603, "Graph agent execution failed: " + e.getMessage(), null);
                }
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                if (context.getTask() != null && context.getTask().getStatus() != null) {
                    throw new TaskNotCancelableError();
                }
                throw new TaskNotCancelableError();
            }
        };
    }

    private static String extractProductId(String userInput) {
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(userInput);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "SKU-500";
    }

    private static String formatResponse(String productId, GraphTools.GraphResponse graphResponse) {
        String nodeSummary = graphResponse.nodes().stream()
                .map(node -> node.getOrDefault("label", node.toString()))
                .reduce((left, right) -> left + " -> " + right)
                .orElse("No nodes found");

        String edgeSummary = graphResponse.edges().stream()
                .map(edge -> edge.getOrDefault("label", "RELATED_TO"))
                .reduce((left, right) -> left + ", " + right)
                .orElse("No relationships found");

        return "Dependencies for " + productId + ": " + nodeSummary + " | relationships: " + edgeSummary;
    }

    private static String renderGraphPng(String productId, GraphTools.GraphResponse graphResponse) throws Exception {
        int width = 1100;
        int height = 720;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        String supplierLabel = nodeLabel(graphResponse, 0, "Supplier");
        String productLabel = nodeLabel(graphResponse, 1, "Product: " + productId);
        String relationshipLabel = edgeLabel(graphResponse, "RELATED_TO");

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(0xF6F2EA));
            graphics.fillRect(0, 0, width, height);

            graphics.setColor(new Color(0x17324D));
            graphics.fillRect(0, 0, width, 132);
            graphics.setColor(new Color(0xF8FAFC));
            graphics.setFont(new Font("SansSerif", Font.BOLD, 32));
            graphics.drawString("Oracle Graph Dependency View", 48, 56);
            graphics.setColor(new Color(0xCBD5E1));
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 18));
            graphics.drawString("PNG artifact returned over A2A for Gemini Enterprise rendering.", 48, 94);

            drawNode(graphics, 110, 250, 310, 120, new Color(0xE0F2FE), supplierLabel);
            drawNode(graphics, 665, 250, 310, 120, new Color(0xFEF3C7), productLabel);

            graphics.setStroke(new BasicStroke(6f));
            graphics.setColor(new Color(0xC2410C));
            graphics.drawLine(420, 310, 655, 310);
            graphics.fillOval(645, 300, 18, 18);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 22));
            graphics.drawString(relationshipLabel, 466, 282);

            graphics.setColor(new Color(0xFFFFFF));
            graphics.fill(new RoundRectangle2D.Double(110, 450, 865, 180, 28, 28));
            graphics.setColor(new Color(0xD6CABB));
            graphics.setStroke(new BasicStroke(2f));
            graphics.draw(new RoundRectangle2D.Double(110, 450, 865, 180, 28, 28));
            graphics.setColor(new Color(0x1E293B));
            graphics.setFont(new Font("SansSerif", Font.BOLD, 24));
            graphics.drawString("Dependency Summary", 148, 500);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 18));
            graphics.setColor(new Color(0x475569));
            graphics.drawString("Product: " + productId, 148, 542);
            graphics.drawString(
                    "Relationship: " + relationshipLabel,
                    148,
                    576
            );
            graphics.drawString(
                    "Replace GraphTools.getSupplyChainDependencies() with a real Oracle Graph query later.",
                    148,
                    610
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } finally {
            graphics.dispose();
        }
    }

    private static String nodeLabel(
            GraphTools.GraphResponse graphResponse,
            int index,
            String fallback
    ) {
        if (graphResponse.nodes().size() <= index) {
            return fallback;
        }
        return graphResponse.nodes().get(index).getOrDefault("label", fallback);
    }

    private static String edgeLabel(
            GraphTools.GraphResponse graphResponse,
            String fallback
    ) {
        if (graphResponse.edges().isEmpty()) {
            return fallback;
        }
        return graphResponse.edges().get(0).getOrDefault("label", fallback);
    }

    private static void drawNode(
            Graphics2D graphics,
            int x,
            int y,
            int width,
            int height,
            Color fillColor,
            String label
    ) {
        RoundRectangle2D.Double node = new RoundRectangle2D.Double(x, y, width, height, 30, 30);
        graphics.setColor(fillColor);
        graphics.fill(node);
        graphics.setColor(new Color(0x475569));
        graphics.setStroke(new BasicStroke(3f));
        graphics.draw(node);
        graphics.setColor(new Color(0x0F172A));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 22));
        graphics.drawString(label, x + 24, y + 68);
    }

    private static String valueOrDefault(Environment environment, String key, String defaultValue) {
        return firstNonBlank(environment.getProperty(key), defaultValue);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
