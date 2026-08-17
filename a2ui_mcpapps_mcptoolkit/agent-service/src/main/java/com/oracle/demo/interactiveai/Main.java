package com.oracle.demo.interactiveai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class Main {
    private final Map<String, SupplyChainRepository> repositories;
    private final ApprovalService approvals;
    private final String actor =
            System.getenv().getOrDefault(
                    "REQUESTED_BY",
                    "supply.planner@example.com");
    private final Path webRoot;

    private Main(
            Path webRoot,
            Map<String, SupplyChainRepository> repositories) {
        this.webRoot = webRoot;
        this.repositories = Map.copyOf(repositories);
        this.approvals = new ApprovalService();
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(
                System.getenv().getOrDefault("AGENT_PORT", "8080"));
        Path webRoot = Path.of(
                System.getProperty("web.root", "../web-client"));
        Map<String, SupplyChainRepository> repositories = new LinkedHashMap<>();
        repositories.put(
                "full",
                McpToolkitSupplyChainRepository.fromEnvironment(System.getenv()));
        if (System.getenv().containsKey("DB_USERNAME2")
                && System.getenv().containsKey("DB_PASSWORD2")) {
            repositories.put(
                    "environmental",
                    McpToolkitSupplyChainRepository.secondaryFromEnvironment(
                            System.getenv()));
        }
        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        () -> repositories.values().forEach(Main::closeQuietly),
                        "repository-shutdown"));
        new Main(webRoot, repositories).start(port);
    }

    private void start(int port) throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", port),
                0);
        server.createContext("/api/runs", this::run);
        server.createContext("/api/recommendations", this::recommendations);
        server.createContext("/api/reviews", this::review);
        server.createContext("/api/approve", this::approve);
        server.createContext("/api/reject", this::reject);
        server.createContext(
                "/api/health",
                exchange -> respond(
                        exchange,
                        200,
                        "application/json",
                        Json.value(Map.of(
                                "status", "UP",
                                "mode", "mcp",
                                "backend",
                                "oracle-db-mcp-java-toolkit",
                                "useCase",
                                "supply-chain-inventory-exchange"))));
        server.createContext("/", this::staticFile);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println(
                "Supply-chain inventory exchange listening on http://127.0.0.1:"
                        + port);
    }

    private void recommendations(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain", "GET required");
            return;
        }
        try {
            Map<String, String> query = query(exchange);
            double minimumStockoutRisk = Double.parseDouble(
                    query.getOrDefault("minimumStockoutRisk", "70"));
            int maximumRows = Integer.parseInt(
                    query.getOrDefault("maximumRows", "10"));
            InputValidation.minimumStockoutRisk(minimumStockoutRisk);
            InputValidation.maximumRows(maximumRows);
            String accessProfile = accessProfile(query);
            List<TransferRecommendation> recommendations =
                    repository(accessProfile).findTransferRecommendations(
                            minimumStockoutRisk,
                            maximumRows);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            respond(
                    exchange,
                    200,
                    "application/json",
                    recommendationsJson(recommendations));
        } catch (IllegalArgumentException exception) {
            respond(
                    exchange,
                    400,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        } catch (IllegalStateException exception) {
            respond(
                    exchange,
                    500,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        }
    }

    private void run(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain", "POST required");
            return;
        }
        try {
            Map<String, String> form = form(exchange);
            double minimumStockoutRisk = Double.parseDouble(
                    form.getOrDefault("minimumStockoutRisk", "70"));
            int maximumRows = Integer.parseInt(
                    form.getOrDefault("maximumRows", "10"));
            InputValidation.minimumStockoutRisk(minimumStockoutRisk);
            InputValidation.maximumRows(maximumRows);
            String accessProfile = accessProfile(form);
            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, 0);
            try (var output = exchange.getResponseBody()) {
                new AguiRunService(
                        repository(accessProfile), approvals).stream(
                        output,
                        minimumStockoutRisk,
                        maximumRows,
                        scopedActor(accessProfile));
            }
        } catch (IllegalArgumentException exception) {
            respond(
                    exchange,
                    400,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        } catch (IllegalStateException exception) {
            respond(
                    exchange,
                    500,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        }
    }

    private void review(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain", "POST required");
            return;
        }
        try {
            Map<String, String> form = form(exchange);
            double minimumStockoutRisk = Double.parseDouble(
                    form.getOrDefault("minimumStockoutRisk", "70"));
            int maximumRows = Integer.parseInt(
                    form.getOrDefault("maximumRows", "10"));
            InputValidation.minimumStockoutRisk(minimumStockoutRisk);
            InputValidation.maximumRows(maximumRows);
            String accessProfile = accessProfile(form);
            List<TransferRecommendation> recommendations =
                    repository(accessProfile).findTransferRecommendations(
                            minimumStockoutRisk,
                            maximumRows);
            String approvalId = approvals.issue(
                    scopedActor(accessProfile), recommendations);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            respond(
                    exchange,
                    200,
                    "application/json",
                    reviewJson(
                            recommendations,
                            approvalId,
                            repository(accessProfile).writesAllowed()));
        } catch (IllegalArgumentException exception) {
            respond(
                    exchange,
                    400,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        } catch (IllegalStateException exception) {
            respond(
                    exchange,
                    500,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        }
    }

    private void approve(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain", "POST required");
            return;
        }
        try {
            Map<String, String> form = form(exchange);
            String accessProfile = accessProfile(form);
            String recommendationId = required(form, "recommendationId");
            String approvalId = required(form, "approvalId");
            String notes = required(form, "approvalNotes");
            TransferRecommendation recommendation =
                    approvals.consume(
                            approvalId,
                            recommendationId,
                            scopedActor(accessProfile));
            InputValidation.approval(
                    recommendation, notes, scopedActor(accessProfile));
            TransferResult result =
                    repository(accessProfile).approveTransfer(
                            recommendation, notes, scopedActor(accessProfile));
            System.out.printf(
                    "audit tool=approve-inventory-transfer actor=%s "
                            + "recommendationId=%s result=APPROVED transferId=%d%n",
                    scopedActor(accessProfile),
                    recommendationId,
                    result.transferId());
            respond(
                    exchange,
                    200,
                    "application/json",
                    Json.value(Map.of(
                            "transferId", result.transferId(),
                            "recommendationId", result.recommendationId(),
                            "transferQuantity", result.transferQuantity(),
                            "status", result.status())));
        } catch (IllegalArgumentException exception) {
            respond(
                    exchange,
                    400,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        } catch (IllegalStateException exception) {
            respond(
                    exchange,
                    500,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        }
    }

    private void reject(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain", "POST required");
            return;
        }
        try {
            Map<String, String> form = form(exchange);
            String accessProfile = accessProfile(form);
            approvals.reject(
                    required(form, "approvalId"), scopedActor(accessProfile));
            System.out.printf(
                    "audit tool=approve-inventory-transfer actor=%s "
                            + "result=REJECTED%n",
                    scopedActor(accessProfile));
            respond(
                    exchange,
                    200,
                    "application/json",
                    "{\"status\":\"REJECTED\"}");
        } catch (IllegalArgumentException exception) {
            respond(
                    exchange,
                    400,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        }
    }

    private void staticFile(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String filename = requestPath.equals("/")
                ? "index.html"
                : requestPath.substring(1);
        if (!filename.matches("[A-Za-z0-9._/-]+")
                || filename.contains("..")) {
            respond(exchange, 400, "text/plain", "Invalid path");
            return;
        }
        Path file = webRoot.resolve(filename).normalize();
        if (!file.startsWith(webRoot.normalize())
                || !Files.isRegularFile(file)) {
            respond(exchange, 404, "text/plain", "Not found");
            return;
        }
        String type = filename.endsWith(".js")
                ? "text/javascript"
                : filename.endsWith(".css")
                        ? "text/css"
                        : "text/html";
        byte[] body = Files.readAllBytes(file);
        exchange.getResponseHeaders().set(
                "Content-Type",
                type + "; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static Map<String, String> form(HttpExchange exchange)
            throws IOException {
        return pairs(new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8));
    }

    private static Map<String, String> query(HttpExchange exchange) {
        return pairs(exchange.getRequestURI().getRawQuery());
    }

    private static Map<String, String> pairs(String encoded) {
        Map<String, String> result = new HashMap<>();
        if (encoded == null || encoded.isBlank()) return result;
        for (String pair : encoded.split("&")) {
            if (pair.isBlank()) continue;
            String[] pieces = pair.split("=", 2);
            result.put(
                    URLDecoder.decode(pieces[0], StandardCharsets.UTF_8),
                    pieces.length == 2
                            ? URLDecoder.decode(
                                    pieces[1],
                                    StandardCharsets.UTF_8)
                            : "");
        }
        return result;
    }

    private static String required(
            Map<String, String> values,
            String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private String accessProfile(Map<String, String> values) {
        String profile = values.getOrDefault("accessProfile", "full");
        if (!repositories.containsKey(profile)) {
            throw new IllegalArgumentException(
                    "Unknown or unavailable access profile: " + profile);
        }
        return profile;
    }

    private SupplyChainRepository repository(String accessProfile) {
        return repositories.get(accessProfile);
    }

    private String scopedActor(String accessProfile) {
        return actor + ":" + accessProfile;
    }

    static String recommendationsJson(
            List<TransferRecommendation> recommendations) {
        return Json.value(Map.of(
                "source", "oracle-db-mcp-java-toolkit",
                "recommendations",
                recommendations.stream()
                        .map(Json::recommendationMap)
                        .toList()));
    }

    static String reviewJson(
            List<TransferRecommendation> recommendations,
            String approvalId,
            boolean writesAllowed) {
        return Json.value(Map.of(
                "source", "oracle-db-mcp-java-toolkit",
                "approvalId", approvalId,
                "writesAllowed", writesAllowed,
                "recommendations",
                recommendations.stream()
                        .map(Json::recommendationMap)
                        .toList()));
    }

    private static void respond(
            HttpExchange exchange,
            int status,
            String contentType,
            String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type",
                contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void closeQuietly(Object candidate) {
        if (!(candidate instanceof AutoCloseable closeable)) return;
        try {
            closeable.close();
        } catch (Exception exception) {
            System.err.println(
                    "Unable to close MCP repository: "
                            + exception.getMessage());
        }
    }
}
